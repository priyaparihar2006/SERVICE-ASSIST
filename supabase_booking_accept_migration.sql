-- supabase_booking_accept_migration.sql
-- Idempotent schema migration for Booking Acceptance and Partner Cancellation

-- 1. Add accepted_at to bookings
ALTER TABLE public.bookings ADD COLUMN IF NOT EXISTS accepted_at TIMESTAMPTZ;

-- Backfill in-flight / completed bookings
UPDATE public.bookings 
SET accepted_at = COALESCE(accepted_at, created_at)
WHERE status IN ('ON_THE_WAY','ARRIVED','STARTED','AWAITING_PAYMENT','COMPLETED') 
  AND accepted_at IS NULL;

-- Index for partner query performance
CREATE INDEX IF NOT EXISTS idx_bookings_pro_accept ON public.bookings (professional_id, accepted_at);

-- 2. Add cancellation tracking columns
ALTER TABLE public.bookings ADD COLUMN IF NOT EXISTS cancelled_by TEXT CHECK (cancelled_by IN ('CUSTOMER','PARTNER','ADMIN'));
ALTER TABLE public.bookings ADD COLUMN IF NOT EXISTS partner_cancel_reason_code TEXT;

-- Verify cancellation_reason, cancellation_feedback, cancelled_at exist
ALTER TABLE public.bookings ADD COLUMN IF NOT EXISTS cancellation_reason TEXT;
ALTER TABLE public.bookings ADD COLUMN IF NOT EXISTS cancellation_feedback TEXT;
ALTER TABLE public.bookings ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMPTZ;

-- 3. Partner Job Cancellations Audit Table
CREATE TABLE IF NOT EXISTS public.partner_job_cancellations (
  id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  booking_id         BIGINT NOT NULL REFERENCES public.bookings(id) ON DELETE CASCADE,
  booking_code       TEXT,
  partner_id         TEXT NOT NULL,
  customer_id        TEXT,
  reason_code        TEXT NOT NULL CHECK (reason_code IN (
                       'EMERGENCY',
                       'VEHICLE_ISSUE',
                       'RUNNING_LATE',
                       'LOCATION_ISSUE',
                       'CUSTOMER_UNREACHABLE',
                       'CUSTOMER_REQUEST',
                       'TOOLS_UNAVAILABLE',
                       'SAFETY_CONCERN',
                       'OTHER'
                     )),
  reason_label       TEXT NOT NULL,
  reason_note        TEXT,
  status_at_cancel   TEXT NOT NULL,
  accepted_at        TIMESTAMPTZ,
  cancelled_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_pjc_partner ON public.partner_job_cancellations (partner_id, cancelled_at DESC);

-- Enable RLS (Service role access only via Edge Functions)
ALTER TABLE public.partner_job_cancellations ENABLE ROW LEVEL SECURITY;
