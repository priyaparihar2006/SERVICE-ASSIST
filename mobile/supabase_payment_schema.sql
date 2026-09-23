-- =========================================================================
-- PHASE 1: SUPABASE PAYMENT SCHEMA & COMPLETION GUARD
-- Tables, RLS, App Config, and Load-bearing Completion Trigger
-- =========================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 1. App Configuration Table (Payee VPA & Business Name)
CREATE TABLE IF NOT EXISTS public.app_config (
    key         TEXT PRIMARY KEY,
    value       TEXT NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO public.app_config (key, value) VALUES
    ('upi_payee_vpa', '9105830551@upi'),
    ('upi_payee_name', 'Servora')
ON CONFLICT (key) DO NOTHING;

-- 2. Payments Table
CREATE TABLE IF NOT EXISTS public.payments (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id         BIGINT NOT NULL REFERENCES public.bookings(id) ON DELETE CASCADE,
    amount             INTEGER NOT NULL CHECK (amount > 0),
    currency           TEXT NOT NULL DEFAULT 'INR',
    method             TEXT NOT NULL CHECK (method IN ('CASH', 'UPI')),
    status             TEXT NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PAID', 'FAILED', 'CANCELLED')),
    upi_vpa            TEXT,
    upi_reference      TEXT,
    qr_payload         TEXT,
    confirmed_by       TEXT,
    confirmed_role     TEXT CHECK (confirmed_role IN ('PARTNER', 'PROFESSIONAL', 'CUSTOMER', 'ADMIN')),
    confirmation_note  TEXT,
    gateway_provider   TEXT,
    gateway_order_id   TEXT,
    gateway_payment_id TEXT,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    paid_at            TIMESTAMPTZ,
    UNIQUE (booking_id)
);

CREATE INDEX IF NOT EXISTS idx_payments_booking_id ON public.payments (booking_id);

-- 3. Payment Audit Log (Append-only)
CREATE TABLE IF NOT EXISTS public.payment_audit_log (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id  UUID REFERENCES public.payments(id) ON DELETE SET NULL,
    booking_id  BIGINT NOT NULL,
    actor_id    TEXT NOT NULL,
    actor_role  TEXT NOT NULL CHECK (actor_role IN ('PARTNER', 'PROFESSIONAL', 'CUSTOMER', 'ADMIN', 'SYSTEM')),
    action      TEXT NOT NULL CHECK (action IN ('QR_GENERATED', 'CASH_COLLECTED', 'UPI_CONFIRMED', 'ADMIN_OVERRIDE', 'MARKED_FAILED')),
    detail      TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 4. Forward-compatible Columns on Bookings
ALTER TABLE public.bookings ADD COLUMN IF NOT EXISTS payment_reference TEXT;
ALTER TABLE public.bookings ADD COLUMN IF NOT EXISTS paid_at BIGINT;

-- 5. Drop and recreate partner_booking_view
DROP VIEW IF EXISTS public.partner_booking_view CASCADE;

CREATE VIEW public.partner_booking_view
WITH (security_invoker = true)
AS
SELECT 
    id,
    customer_id,
    customer_name,
    booking_code,
    service_id,
    service_name,
    package_name,
    scheduled_date,
    scheduled_time,
    address_text,
    locality,
    city,
    total_amount,
    discount_amount,
    promo_code,
    payment_method,
    is_paid,
    status,
    professional_id,
    start_otp,
    special_notes,
    payment_reference,
    paid_at,
    created_at
FROM public.bookings;

-- 6. RLS & Permissions
ALTER TABLE public.payments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.payment_audit_log ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.app_config ENABLE ROW LEVEL SECURITY;

-- Deny direct client table access for payments & audit log
REVOKE ALL ON public.payments, public.payment_audit_log FROM anon, authenticated;
GRANT SELECT ON public.app_config TO anon, authenticated;
REVOKE INSERT, UPDATE, DELETE ON public.app_config FROM anon, authenticated;

-- Allow SELECT policy on app_config for anon and authenticated
DROP POLICY IF EXISTS "allow_read_app_config" ON public.app_config;
CREATE POLICY "allow_read_app_config" ON public.app_config
FOR SELECT TO anon, authenticated
USING (true);

-- 7. Load-Bearing Completion Guard Trigger
CREATE OR REPLACE FUNCTION public.enforce_payment_before_completion()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    IF NEW.status = 'COMPLETED' AND OLD.status IS DISTINCT FROM 'COMPLETED' THEN
        IF NOT EXISTS (
            SELECT 1 FROM public.payments
            WHERE booking_id = NEW.id AND status = 'PAID'
        ) THEN
            RAISE EXCEPTION 'Booking % cannot be marked COMPLETED without a PAID payment record', NEW.id;
        END IF;
        IF NEW.is_paid IS DISTINCT FROM true THEN
            RAISE EXCEPTION 'Booking % is_paid must be true when status is COMPLETED', NEW.id;
        END IF;
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_enforce_payment_before_completion ON public.bookings;
CREATE TRIGGER trg_enforce_payment_before_completion
BEFORE UPDATE ON public.bookings
FOR EACH ROW
EXECUTE FUNCTION public.enforce_payment_before_completion();

-- 8. Append-Only Trigger for Audit Log
CREATE OR REPLACE FUNCTION public.block_audit_mutation()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'payment_audit_log is append-only';
END;
$$;

DROP TRIGGER IF EXISTS trg_block_payment_audit_update ON public.payment_audit_log;
CREATE TRIGGER trg_block_payment_audit_update
BEFORE UPDATE OR DELETE ON public.payment_audit_log
FOR EACH ROW
EXECUTE FUNCTION public.block_audit_mutation();
