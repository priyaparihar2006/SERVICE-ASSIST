-- =========================================================================
-- PHASE 0: SUPABASE SECURITY HARDENING & RLS CLOSURE
-- Closed RLS, Role Protection, Phone Masking View & ID Mapping
-- =========================================================================

-- 1. Add auth_user_id mapping column to user_profiles if not exists
ALTER TABLE public.user_profiles 
ADD COLUMN IF NOT EXISTS auth_user_id UUID UNIQUE;

-- 2. Secure helper functions for JWT role and identity
CREATE OR REPLACE FUNCTION public.current_auth_role()
RETURNS TEXT
LANGUAGE sql
STABLE
AS $$
  SELECT COALESCE(
    (auth.jwt() -> 'app_metadata' ->> 'role'),
    'anonymous'
  );
$$;

CREATE OR REPLACE FUNCTION public.current_user_profile_id()
RETURNS TEXT
LANGUAGE sql
STABLE
AS $$
  SELECT COALESCE(
    (SELECT id FROM public.user_profiles WHERE auth_user_id = auth.uid()),
    auth.uid()::text
  );
$$;

-- 3. Trigger to prevent clients from updating user_profiles.role
CREATE OR REPLACE FUNCTION public.prevent_role_escalation()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
  IF NEW.role <> OLD.role THEN
    IF current_auth_role() <> 'admin' AND current_user <> 'service_role' THEN
      RAISE EXCEPTION 'Unauthorized: Only service_role or admin can update user role';
    END IF;
  END IF;
  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_prevent_role_escalation ON public.user_profiles;
CREATE TRIGGER trg_prevent_role_escalation
BEFORE UPDATE ON public.user_profiles
FOR EACH ROW
EXECUTE FUNCTION public.prevent_role_escalation();

-- 4. Partner Booking View (Masked: Completely excludes customer_phone)
CREATE OR REPLACE VIEW public.partner_booking_view
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
    created_at
FROM public.bookings;

-- 5. Drop overly-permissive "Allow public ..." policies
DROP POLICY IF EXISTS "Allow public read bookings" ON public.bookings;
DROP POLICY IF EXISTS "Allow public insert bookings" ON public.bookings;
DROP POLICY IF EXISTS "Allow public update bookings" ON public.bookings;
DROP POLICY IF EXISTS "Allow public delete bookings" ON public.bookings;

DROP POLICY IF EXISTS "Allow public read user_profiles" ON public.user_profiles;
DROP POLICY IF EXISTS "Allow public insert user_profiles" ON public.user_profiles;
DROP POLICY IF EXISTS "Allow public update user_profiles" ON public.user_profiles;
DROP POLICY IF EXISTS "Allow public delete user_profiles" ON public.user_profiles;

DROP POLICY IF EXISTS "Allow public read saved_addresses" ON public.saved_addresses;
DROP POLICY IF EXISTS "Allow public insert saved_addresses" ON public.saved_addresses;
DROP POLICY IF EXISTS "Allow public update saved_addresses" ON public.saved_addresses;
DROP POLICY IF EXISTS "Allow public delete saved_addresses" ON public.saved_addresses;

-- 6. Enforce Tightened RLS on Bookings
ALTER TABLE public.bookings ENABLE ROW LEVEL SECURITY;

-- Admins: full access
CREATE POLICY "admin_all_bookings" ON public.bookings
FOR ALL TO authenticated
USING (public.current_auth_role() = 'admin')
WITH CHECK (public.current_auth_role() = 'admin');

-- Customers: view only their own bookings
CREATE POLICY "customer_select_bookings" ON public.bookings
FOR SELECT TO authenticated
USING (customer_id = public.current_user_profile_id() AND public.current_auth_role() = 'customer');

-- Customers: insert bookings for themselves
CREATE POLICY "customer_insert_bookings" ON public.bookings
FOR INSERT TO authenticated
WITH CHECK (customer_id = public.current_user_profile_id());

-- Partners: select only bookings assigned to them
CREATE POLICY "partner_select_bookings" ON public.bookings
FOR SELECT TO authenticated
USING (professional_id = public.current_user_profile_id() AND public.current_auth_role() = 'partner');

-- Partners: update booking status only for their assigned jobs
CREATE POLICY "partner_update_booking_status" ON public.bookings
FOR UPDATE TO authenticated
USING (professional_id = public.current_user_profile_id() AND public.current_auth_role() = 'partner')
WITH CHECK (professional_id = public.current_user_profile_id() AND public.current_auth_role() = 'partner');

-- 7. Enforce Tightened RLS on User Profiles
ALTER TABLE public.user_profiles ENABLE ROW LEVEL SECURITY;

CREATE POLICY "admin_all_user_profiles" ON public.user_profiles
FOR ALL TO authenticated
USING (public.current_auth_role() = 'admin')
WITH CHECK (public.current_auth_role() = 'admin');

CREATE POLICY "user_select_own_profile" ON public.user_profiles
FOR SELECT TO authenticated
USING (id = public.current_user_profile_id() OR public.current_auth_role() = 'admin');

CREATE POLICY "user_update_own_profile" ON public.user_profiles
FOR UPDATE TO authenticated
USING (id = public.current_user_profile_id() OR public.current_auth_role() = 'admin')
WITH CHECK (id = public.current_user_profile_id() OR public.current_auth_role() = 'admin');

-- 8. Enforce Tightened RLS on Saved Addresses
ALTER TABLE public.saved_addresses ENABLE ROW LEVEL SECURITY;

CREATE POLICY "user_all_saved_addresses" ON public.saved_addresses
FOR ALL TO authenticated
USING (user_id = public.current_user_profile_id() OR public.current_auth_role() = 'admin')
WITH CHECK (user_id = public.current_user_profile_id() OR public.current_auth_role() = 'admin');
