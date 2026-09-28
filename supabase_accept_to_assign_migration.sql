-- supabase_accept_to_assign_migration.sql
-- Master Prompt 15: Booking confirmed first, assigned only on acceptance.
-- Idempotent, safe migration with lifecycle guard trigger.

BEGIN;

-- 1. Backfill legacy bookings: ASSIGNED without accepted_at -> CONFIRMED
UPDATE public.bookings 
SET status = 'CONFIRMED' 
WHERE status = 'ASSIGNED' AND accepted_at IS NULL;

-- 2. Set default status on bookings to CONFIRMED
ALTER TABLE public.bookings ALTER COLUMN status SET DEFAULT 'CONFIRMED';

-- 3. Lifecycle rank and guard trigger
CREATE OR REPLACE FUNCTION public.booking_status_rank(s TEXT) RETURNS INT LANGUAGE sql IMMUTABLE AS $$
  SELECT CASE s 
    WHEN 'PENDING' THEN 0 
    WHEN 'CONFIRMED' THEN 1 
    WHEN 'ASSIGNED' THEN 2 
    WHEN 'ON_THE_WAY' THEN 3
    WHEN 'ARRIVED' THEN 4 
    WHEN 'STARTED' THEN 5 
    WHEN 'AWAITING_PAYMENT' THEN 6 
    WHEN 'COMPLETED' THEN 7
    WHEN 'CANCELLED' THEN 99 
    ELSE -1 
  END $$;

CREATE OR REPLACE FUNCTION public.trg_bookings_guard_lifecycle() RETURNS TRIGGER
LANGUAGE plpgsql SET search_path = public AS $f$
BEGIN
  -- never erase acceptance
  IF OLD.accepted_at IS NOT NULL AND NEW.accepted_at IS NULL THEN 
    NEW.accepted_at := OLD.accepted_at; 
  END IF;

  -- terminal states stay terminal
  IF OLD.status IN ('COMPLETED','CANCELLED') AND NEW.status IS DISTINCT FROM OLD.status THEN
    NEW.status := OLD.status;
  -- no downgrades (cancel is always allowed from non-terminal)
  ELSIF NEW.status <> 'CANCELLED' AND booking_status_rank(NEW.status) < booking_status_rank(OLD.status) THEN
    NEW.status := OLD.status;
  END IF;

  -- work can't start before acceptance
  IF NEW.accepted_at IS NULL AND booking_status_rank(NEW.status) BETWEEN 3 AND 7 THEN
    RAISE EXCEPTION 'Job must be accepted by the professional first' USING ERRCODE = 'P0001';
  END IF;

  -- ASSIGNED always carries accepted_at
  IF NEW.status = 'ASSIGNED' AND NEW.accepted_at IS NULL THEN 
    NEW.accepted_at := now(); 
  END IF;

  RETURN NEW;
END $f$;

DROP TRIGGER IF EXISTS trg_bookings_guard_lifecycle ON public.bookings;
CREATE TRIGGER trg_bookings_guard_lifecycle BEFORE UPDATE ON public.bookings
FOR EACH ROW EXECUTE FUNCTION public.trg_bookings_guard_lifecycle();

COMMIT;
