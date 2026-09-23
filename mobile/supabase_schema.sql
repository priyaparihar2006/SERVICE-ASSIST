-- ==========================================================
-- SERVICE ASSIST - SUPABASE POSTGRESQL DATABASE SCHEMA & DEMO DATA
-- Run this SQL in your Supabase Dashboard: SQL Editor -> New Query
-- ==========================================================

-- 1. USER PROFILES TABLE
CREATE TABLE IF NOT EXISTS public.user_profiles (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    phone TEXT NOT NULL,
    email TEXT NOT NULL,
    city TEXT NOT NULL DEFAULT 'Agra',
    locality TEXT NOT NULL DEFAULT 'Taj Nagri Phase 2',
    role TEXT NOT NULL DEFAULT 'CUSTOMER',
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now())
);

-- 2. SAVED ADDRESSES TABLE
CREATE TABLE IF NOT EXISTS public.saved_addresses (
    id BIGSERIAL PRIMARY KEY,
    user_id TEXT NOT NULL DEFAULT 'user_priya_1',
    title TEXT NOT NULL,
    full_address TEXT NOT NULL,
    locality TEXT NOT NULL,
    city TEXT NOT NULL DEFAULT 'Agra',
    landmark TEXT DEFAULT '',
    is_default BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now())
);

-- 3. BOOKINGS TABLE
CREATE TABLE IF NOT EXISTS public.bookings (
    id BIGSERIAL PRIMARY KEY,
    customer_id TEXT NOT NULL DEFAULT 'user_priya_1',
    customer_name TEXT NOT NULL DEFAULT 'Priya Sharma',
    customer_phone TEXT NOT NULL DEFAULT '+91 98765 43210',
    booking_code TEXT NOT NULL UNIQUE,
    service_id TEXT NOT NULL,
    service_name TEXT NOT NULL,
    package_name TEXT NOT NULL,
    scheduled_date TEXT NOT NULL,
    scheduled_time TEXT NOT NULL,
    address_text TEXT NOT NULL,
    locality TEXT NOT NULL,
    city TEXT NOT NULL DEFAULT 'Agra',
    total_amount INTEGER NOT NULL,
    discount_amount INTEGER DEFAULT 0,
    promo_code TEXT DEFAULT '',
    payment_method TEXT NOT NULL DEFAULT 'Cash after service',
    is_paid BOOLEAN DEFAULT false,
    status TEXT NOT NULL DEFAULT 'ASSIGNED',
    professional_id TEXT NOT NULL DEFAULT 'pro_rajesh_1',
    start_otp TEXT NOT NULL DEFAULT '4829',
    special_notes TEXT DEFAULT '',
    created_at BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::bigint
);

-- 4. REVIEWS TABLE
CREATE TABLE IF NOT EXISTS public.reviews (
    id BIGSERIAL PRIMARY KEY,
    service_id TEXT NOT NULL,
    service_name TEXT NOT NULL,
    professional_name TEXT NOT NULL,
    customer_name TEXT NOT NULL DEFAULT 'Priya S.',
    rating REAL NOT NULL DEFAULT 5.0,
    comment TEXT NOT NULL,
    tags TEXT DEFAULT 'Punctual, Expert',
    date_text TEXT DEFAULT 'Today',
    created_at BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::bigint
);

-- 5. SERVICE CATEGORIES TABLE
CREATE TABLE IF NOT EXISTS public.service_categories (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT NOT NULL,
    starting_price INTEGER NOT NULL,
    icon_name TEXT NOT NULL,
    tag TEXT DEFAULT '',
    is_featured BOOLEAN DEFAULT false
);

-- 6. INDEXES
CREATE INDEX IF NOT EXISTS idx_bookings_customer_id ON public.bookings (customer_id);
CREATE INDEX IF NOT EXISTS idx_saved_addresses_user_id ON public.saved_addresses (user_id);

-- 7. ENABLE ROW LEVEL SECURITY (RLS)
ALTER TABLE public.user_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.saved_addresses ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.bookings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.reviews ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.service_categories ENABLE ROW LEVEL SECURITY;

-- 8. RLS POLICIES (Allow public/anon read and write for the Android client)
-- User Profiles
DROP POLICY IF EXISTS "Allow public read user_profiles" ON public.user_profiles;
DROP POLICY IF EXISTS "Allow public insert user_profiles" ON public.user_profiles;
DROP POLICY IF EXISTS "Allow public update user_profiles" ON public.user_profiles;
CREATE POLICY "Allow public read user_profiles" ON public.user_profiles FOR SELECT USING (true);
CREATE POLICY "Allow public insert user_profiles" ON public.user_profiles FOR INSERT WITH CHECK (true);
CREATE POLICY "Allow public update user_profiles" ON public.user_profiles FOR UPDATE USING (true);

-- Saved Addresses
DROP POLICY IF EXISTS "Allow public read saved_addresses" ON public.saved_addresses;
DROP POLICY IF EXISTS "Allow public insert saved_addresses" ON public.saved_addresses;
DROP POLICY IF EXISTS "Allow public update saved_addresses" ON public.saved_addresses;
DROP POLICY IF EXISTS "Allow public delete saved_addresses" ON public.saved_addresses;
CREATE POLICY "Allow public read saved_addresses" ON public.saved_addresses FOR SELECT USING (true);
CREATE POLICY "Allow public insert saved_addresses" ON public.saved_addresses FOR INSERT WITH CHECK (true);
CREATE POLICY "Allow public update saved_addresses" ON public.saved_addresses FOR UPDATE USING (true);
CREATE POLICY "Allow public delete saved_addresses" ON public.saved_addresses FOR DELETE USING (true);

-- Bookings
DROP POLICY IF EXISTS "Allow public read bookings" ON public.bookings;
DROP POLICY IF EXISTS "Allow public insert bookings" ON public.bookings;
DROP POLICY IF EXISTS "Allow public update bookings" ON public.bookings;
CREATE POLICY "Allow public read bookings" ON public.bookings FOR SELECT USING (true);
CREATE POLICY "Allow public insert bookings" ON public.bookings FOR INSERT WITH CHECK (true);
CREATE POLICY "Allow public update bookings" ON public.bookings FOR UPDATE USING (true);

-- Reviews
DROP POLICY IF EXISTS "Allow public read reviews" ON public.reviews;
DROP POLICY IF EXISTS "Allow public insert reviews" ON public.reviews;
CREATE POLICY "Allow public read reviews" ON public.reviews FOR SELECT USING (true);
CREATE POLICY "Allow public insert reviews" ON public.reviews FOR INSERT WITH CHECK (true);

-- Service Categories
DROP POLICY IF EXISTS "Allow public read service_categories" ON public.service_categories;
CREATE POLICY "Allow public read service_categories" ON public.service_categories FOR SELECT USING (true);

-- ==========================================================
-- 9. COMPREHENSIVE DEMO DATA SEEDING
-- ==========================================================

-- DEMO USERS
INSERT INTO public.user_profiles (id, name, phone, email, city, locality, role)
VALUES 
('user_priya_1', 'Priya Sharma', '+91 98765 43210', 'priya.sharma@example.com', 'Agra', 'Taj Nagri Phase 2', 'CUSTOMER'),
('user_rajesh_pro', 'Rajesh Sharma', '+91 98234 56789', 'rajesh.pro@serviceassist.in', 'Agra', 'Fatehabad Road', 'PROFESSIONAL'),
('user_admin_sneha', 'Sneha Patel', '+91 94120 11223', 'admin@serviceassist.in', 'Agra', 'Sanjay Place', 'ADMIN')
ON CONFLICT (id) DO UPDATE SET 
    name = EXCLUDED.name,
    phone = EXCLUDED.phone,
    email = EXCLUDED.email,
    city = EXCLUDED.city,
    locality = EXCLUDED.locality,
    role = EXCLUDED.role;

-- DEMO SAVED ADDRESSES
INSERT INTO public.saved_addresses (id, user_id, title, full_address, locality, city, landmark, is_default)
VALUES
(1, 'user_priya_1', 'Home', 'Flat 402, Royal Residency, Taj Nagri Phase 2', 'Taj Nagri Phase 2', 'Agra', 'Near Shilpgram East Gate', true),
(2, 'user_priya_1', 'Office', 'Suite 305, Corporate Plaza, Sanjay Place', 'Sanjay Place', 'Agra', 'Opposite LIC Building', false),
(3, 'user_priya_1', 'Parents Home', 'B-14, Radhasoami Colony, Dayalbagh Main Road', 'Dayalbagh', 'Agra', 'Near Radhasoami Temple', false),
(4, 'user_priya_1', 'Farmhouse', 'Villa 7, Shamshabad Road Green Acres', 'Shamshabad Road', 'Agra', 'Behind Toll Plaza', false)
ON CONFLICT (id) DO UPDATE SET
    user_id = EXCLUDED.user_id,
    title = EXCLUDED.title,
    full_address = EXCLUDED.full_address,
    locality = EXCLUDED.locality,
    city = EXCLUDED.city,
    landmark = EXCLUDED.landmark,
    is_default = EXCLUDED.is_default;

-- DEMO SERVICE CATEGORIES
INSERT INTO public.service_categories (id, name, description, starting_price, icon_name, tag, is_featured)
VALUES
('cat_ac', 'AC & Appliance Repair', 'Deep jet foam cleaning, gas refill, repair & uninstallation', 399, 'AcUnit', 'HOT', true),
('cat_cleaning', 'Home Deep Cleaning', 'Bathroom scrub, sofa shampoo, kitchen chimney & floor buffing', 799, 'CleaningServices', 'POPULAR', true),
('cat_salon', 'Salon & Spa for Women', 'Mani-pedi, waxing, facials & haircut with hygienic disposable kits', 499, 'Spa', 'TRENDING', true),
('cat_electrician', 'Electrician & Plumber', 'Fan installation, short circuit fix, pipe leak & tap fitting', 149, 'Bolt', 'FAST 30M', true),
('cat_painting', 'Painting & Waterproofing', 'Laser wall inspection, Asian Paints Royale & seepage diagnosis', 1999, 'FormatPaint', 'WARRANTY', false),
('cat_pest', 'Pest Control', 'Eco-friendly odorless herbal gel for termites, cockroaches & bedbugs', 699, 'PestControl', 'ODORLESS', false)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    starting_price = EXCLUDED.starting_price,
    icon_name = EXCLUDED.icon_name,
    tag = EXCLUDED.tag,
    is_featured = EXCLUDED.is_featured;

-- DEMO BOOKINGS (Various Realistic Lifecycles)
INSERT INTO public.bookings (id, customer_id, customer_name, customer_phone, booking_code, service_id, service_name, package_name, scheduled_date, scheduled_time, address_text, locality, city, total_amount, discount_amount, promo_code, payment_method, is_paid, status, professional_id, start_otp, special_notes)
VALUES
(1, 'user_priya_1', 'Priya Sharma', '+91 98765 43210', 'SRV-84920', 'ac_service_deep', 'Intense AC Foam Jet Service', '1 Split AC Deep Jet Cleaning', 'Today', '02:30 PM', 'Flat 402, Royal Residency, Taj Nagri Phase 2, Agra', 'Taj Nagri Phase 2', 'Agra', 499, 100, 'FIRST20', 'Cash after service', false, 'ON_THE_WAY', 'pro_rajesh_1', '6824', 'Please ring bell twice, AC is in master bedroom.'),
(2, 'user_priya_1', 'Priya Sharma', '+91 98765 43210', 'SRV-95012', 'plumbing_leak_fix', 'Pipe Leakage & Tap Fix', 'Drain & Pipe Repair', 'Tomorrow', '11:00 AM', 'Suite 305, Corporate Plaza, Sanjay Place, Agra', 'Sanjay Place', 'Agra', 249, 50, 'AGRA50', 'UPI (Google Pay)', true, 'ASSIGNED', 'pro_kavita_4', '3912', 'Pantry sink tap is leaking continuously.'),
(3, 'user_priya_1', 'Priya Sharma', '+91 98765 43210', 'SRV-73105', 'deep_home_cleaning', 'Complete Home Deep Cleaning', '2 BHK Intensive Deep Clean', '12 Sep 2026', '10:00 AM', 'Flat 402, Royal Residency, Taj Nagri Phase 2, Agra', 'Taj Nagri Phase 2', 'Agra', 1899, 200, 'CLEAN100', 'UPI (PhonePe)', true, 'COMPLETED', 'pro_amit_2', '3194', 'Completed thoroughly with hospital-grade sanitizers.'),
(4, 'user_priya_1', 'Priya Sharma', '+91 98765 43210', 'SRV-61294', 'salon_women_glow', 'Glow Facial & Mani-Pedi', 'Bridal Radiance Package', '08 Sep 2026', '04:00 PM', 'B-14, Radhasoami Colony, Dayalbagh Main Road, Agra', 'Dayalbagh', 'Agra', 1299, 150, 'FESTIVE15', 'Card Payment', true, 'COMPLETED', 'pro_meera_3', '8401', 'Disposable sterilized equipment used.'),
(5, 'user_priya_1', 'Priya Sharma', '+91 98765 43210', 'SRV-54911', 'electrician_instant', 'Electrical Short Circuit & Wiring', 'Emergency Repair', '01 Sep 2026', '06:30 PM', 'Villa 7, Shamshabad Road Green Acres, Agra', 'Shamshabad Road', 'Agra', 399, 0, '', 'Cash after service', true, 'COMPLETED', 'pro_rajesh_1', '1928', 'Fixed main MCB trip and neutralized earthing.')
ON CONFLICT (id) DO UPDATE SET
    customer_id = EXCLUDED.customer_id,
    customer_name = EXCLUDED.customer_name,
    customer_phone = EXCLUDED.customer_phone,
    booking_code = EXCLUDED.booking_code,
    service_id = EXCLUDED.service_id,
    service_name = EXCLUDED.service_name,
    package_name = EXCLUDED.package_name,
    scheduled_date = EXCLUDED.scheduled_date,
    scheduled_time = EXCLUDED.scheduled_time,
    address_text = EXCLUDED.address_text,
    locality = EXCLUDED.locality,
    city = EXCLUDED.city,
    total_amount = EXCLUDED.total_amount,
    discount_amount = EXCLUDED.discount_amount,
    promo_code = EXCLUDED.promo_code,
    payment_method = EXCLUDED.payment_method,
    is_paid = EXCLUDED.is_paid,
    status = EXCLUDED.status,
    professional_id = EXCLUDED.professional_id,
    start_otp = EXCLUDED.start_otp,
    special_notes = EXCLUDED.special_notes;

-- DEMO CUSTOMER REVIEWS
INSERT INTO public.reviews (id, service_id, service_name, professional_name, customer_name, rating, comment, tags, date_text)
VALUES
(1, 'ac_service_deep', 'Intense AC Foam Jet Service', 'Rajesh Sharma', 'Ananya V.', 5.0, 'Brilliant service! Rajesh brought proper foam jet pressure equipment and spill-jacket. AC cooling is ice-cold now, zero mess left on the wall.', 'Punctual, Super Clean, Expert', '2 days ago'),
(2, 'deep_home_cleaning', 'Complete Home Deep Cleaning', 'Amit Kumar & Team', 'Vikram Singhania', 4.9, 'Booked for our home in Fatehabad Road Agra before family arrived. Every bathroom tile, balcony rail, and kitchen chimney was scrubbed mirror-shine.', 'Detail Oriented, Professional', 'Last week'),
(3, 'salon_women_glow', 'Glow Facial & Mani-Pedi', 'Meera Saxena', 'Pooja Agarwal', 5.0, 'Meera brought all 100% sanitized disposable kits. The facial massage was so relaxing right in my living room in Dayalbagh. Will book again!', 'Hygienic, Gentle, Punctual', '3 days ago'),
(4, 'plumbing_leak_fix', 'Pipe Leakage & Tap Fix', 'Kavita Singh', 'Rohan Gupta', 4.8, 'Replaced the old rusted angle valve under the kitchen sink in 20 minutes. Clean plumbing work and upfront transparent pricing.', 'Fast Arrival, Fair Price', '5 days ago'),
(5, 'electrician_instant', 'Electrical Short Circuit & Wiring', 'Rajesh Sharma', 'Sunil Mathur', 5.0, 'Our main MCB kept tripping due to heavy geyser load. The electrician diagnosed a neutral wire short and re-terminated it safely within half an hour.', 'Knowledgeable, Safe Work', '1 week ago'),
(6, 'cat_pest', 'Herbal Pest Control', 'Dinesh Rawat', 'Dr. Alok Verma', 5.0, 'Odorless termite and cockroach gel treatment throughout our clinic. No unpleasant smell, very safe for patients.', 'Odorless, Long Lasting', '2 weeks ago')
ON CONFLICT (id) DO UPDATE SET
    service_id = EXCLUDED.service_id,
    service_name = EXCLUDED.service_name,
    professional_name = EXCLUDED.professional_name,
    customer_name = EXCLUDED.customer_name,
    rating = EXCLUDED.rating,
    comment = EXCLUDED.comment,
    tags = EXCLUDED.tags,
    date_text = EXCLUDED.date_text;

-- ==========================================================
-- 10. MIGRATIONS FOR EXISTING INSTANCES
-- (Run this if you already have an existing database)
-- ==========================================================
ALTER TABLE public.bookings
  ADD COLUMN IF NOT EXISTS customer_id    TEXT NOT NULL DEFAULT 'user_priya_1',
  ADD COLUMN IF NOT EXISTS customer_name  TEXT NOT NULL DEFAULT 'Priya Sharma',
  ADD COLUMN IF NOT EXISTS customer_phone TEXT NOT NULL DEFAULT '+91 98765 43210';

ALTER TABLE public.saved_addresses
  ADD COLUMN IF NOT EXISTS user_id TEXT NOT NULL DEFAULT 'user_priya_1';

CREATE INDEX IF NOT EXISTS idx_bookings_customer_id      ON public.bookings (customer_id);
CREATE INDEX IF NOT EXISTS idx_saved_addresses_user_id   ON public.saved_addresses (user_id);

SELECT setval(pg_get_serial_sequence('public.bookings','id'),
              COALESCE((SELECT MAX(id) FROM public.bookings), 1),
              (SELECT COUNT(*) > 0 FROM public.bookings));
SELECT setval(pg_get_serial_sequence('public.saved_addresses','id'),
              COALESCE((SELECT MAX(id) FROM public.saved_addresses), 1),
              (SELECT COUNT(*) > 0 FROM public.saved_addresses));
SELECT setval(pg_get_serial_sequence('public.reviews','id'),
              COALESCE((SELECT MAX(id) FROM public.reviews), 1),
              (SELECT COUNT(*) > 0 FROM public.reviews));

NOTIFY pgrst, 'reload schema';


