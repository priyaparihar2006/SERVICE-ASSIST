-- ⚠ DO NOT RUN ON AN EXISTING PROJECT. Drops every table (bookings, payments, chat…) and recreates the LEGACY chat tables. Chat tables are owned by supabase_chat_schema.sql + v9.
-- ==========================================================
-- SERVICE ASSIST - COMPLETE SUPABASE POSTGRESQL DATABASE SCHEMA
-- Run this SQL in your Supabase Dashboard: SQL Editor -> New Query
-- ==========================================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ==========================================================
-- 1. DROP EXISTING TABLES FOR CLEAN RESET
-- ==========================================================
DROP TABLE IF EXISTS public.chat_cursors CASCADE;
DROP TABLE IF EXISTS public.chat_messages CASCADE;
DROP TABLE IF EXISTS public.chat_conversations CASCADE;
DROP TABLE IF EXISTS public.payment_audit_log CASCADE;
DROP TABLE IF EXISTS public.payments CASCADE;
DROP TABLE IF EXISTS public.app_config CASCADE;
DROP TABLE IF EXISTS public.reviews CASCADE;
DROP TABLE IF EXISTS public.bookings CASCADE;
DROP TABLE IF EXISTS public.saved_addresses CASCADE;
DROP TABLE IF EXISTS public.user_profiles CASCADE;
DROP TABLE IF EXISTS public.service_categories CASCADE;

-- ==========================================================
-- 2. CREATE MASTER TABLES
-- ==========================================================

-- 2.1 USER PROFILES
CREATE TABLE public.user_profiles (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    phone TEXT NOT NULL,
    email TEXT NOT NULL,
    city TEXT NOT NULL DEFAULT 'Agra',
    locality TEXT NOT NULL DEFAULT 'Taj Nagri Phase 2',
    role TEXT NOT NULL DEFAULT 'CUSTOMER',
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now())
);

-- 2.2 SAVED ADDRESSES
CREATE TABLE public.saved_addresses (
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

-- 2.3 BOOKINGS TABLE (Supports full lifecycle: Active, Upcoming, Past, Cancellation & Payments)
CREATE TABLE public.bookings (
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
    status TEXT NOT NULL DEFAULT 'CONFIRMED',
    professional_id TEXT NOT NULL DEFAULT 'pro_rajesh_1',
    start_otp TEXT NOT NULL DEFAULT '4829',
    special_notes TEXT DEFAULT '',
    payment_reference TEXT DEFAULT '',
    paid_at BIGINT DEFAULT NULL,
    cancellation_reason TEXT DEFAULT '',
    cancellation_feedback TEXT DEFAULT '',
    cancelled_at BIGINT DEFAULT NULL,
    created_at BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::bigint
);

-- 2.4 REVIEWS TABLE
CREATE TABLE public.reviews (
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

-- 2.5 SERVICE CATEGORIES TABLE
CREATE TABLE public.service_categories (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT NOT NULL,
    starting_price INTEGER NOT NULL,
    icon_name TEXT NOT NULL,
    tag TEXT DEFAULT '',
    is_featured BOOLEAN DEFAULT false
);

-- 2.6 APP CONFIGURATION (UPI VPAs, platform settings)
CREATE TABLE public.app_config (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL,
    description TEXT DEFAULT ''
);

-- 2.7 PAYMENTS TABLE (Used by App & Edge Functions: payment-init, payment-collect-cash, payment-collect-upi)
CREATE TABLE public.payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id BIGINT UNIQUE NOT NULL,
    amount INTEGER NOT NULL,
    currency TEXT NOT NULL DEFAULT 'INR',
    method TEXT NOT NULL DEFAULT 'UPI',
    status TEXT NOT NULL DEFAULT 'PENDING',
    upi_vpa TEXT DEFAULT '9105830551@upi',
    upi_reference TEXT DEFAULT '',
    qr_payload TEXT DEFAULT '',
    confirmed_by TEXT DEFAULT NULL,
    confirmed_role TEXT DEFAULT NULL,
    confirmation_note TEXT DEFAULT NULL,
    paid_at TIMESTAMP WITH TIME ZONE DEFAULT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now())
);

-- 2.8 PAYMENT AUDIT LOG TABLE
CREATE TABLE public.payment_audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id UUID DEFAULT NULL,
    booking_id BIGINT NOT NULL,
    actor_id TEXT NOT NULL,
    actor_role TEXT NOT NULL DEFAULT 'PARTNER',
    action TEXT NOT NULL,
    detail TEXT DEFAULT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now())
);

-- 2.9 CHAT CONVERSATIONS TABLE
CREATE TABLE public.chat_conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id BIGINT,
    booking_code TEXT,
    customer_id TEXT NOT NULL,
    customer_name TEXT DEFAULT '',
    partner_id TEXT NOT NULL DEFAULT 'pro_rajesh_1',
    professional_id TEXT DEFAULT 'pro_rajesh_1',
    professional_name TEXT DEFAULT 'Rajesh Sharma',
    service_name TEXT DEFAULT 'Doorstep Service',
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    key_version INTEGER DEFAULT 1,
    wrapped_dek TEXT DEFAULT '',
    opened_by TEXT DEFAULT '',
    last_message TEXT DEFAULT '',
    last_message_seq INTEGER DEFAULT 0,
    last_message_time BIGINT DEFAULT (extract(epoch from now()) * 1000)::bigint,
    unread_customer_count INTEGER DEFAULT 0,
    unread_pro_count INTEGER DEFAULT 0,
    closes_at TIMESTAMP WITH TIME ZONE DEFAULT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now())
);

-- 2.10 CHAT MESSAGES TABLE
CREATE TABLE public.chat_messages (
    id BIGSERIAL PRIMARY KEY,
    conversation_id UUID NOT NULL,
    seq INTEGER DEFAULT 1,
    sender_id TEXT NOT NULL,
    sender_role TEXT NOT NULL DEFAULT 'CUSTOMER',
    message_text TEXT NOT NULL,
    ciphertext TEXT DEFAULT '',
    iv TEXT DEFAULT '',
    tag TEXT DEFAULT '',
    is_read BOOLEAN DEFAULT false,
    timestamp BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::bigint,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now())
);

-- 2.11 CHAT READ CURSORS TABLE
CREATE TABLE public.chat_cursors (
    conversation_id UUID NOT NULL,
    user_id TEXT NOT NULL,
    last_read_seq INTEGER DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()),
    PRIMARY KEY (conversation_id, user_id)
);

-- ==========================================================
-- 3. PERFORMANCE INDEXES
-- ==========================================================
CREATE INDEX idx_bookings_customer_id ON public.bookings (customer_id);
CREATE INDEX idx_bookings_status ON public.bookings (status);
CREATE INDEX idx_bookings_scheduled_date ON public.bookings (scheduled_date);
CREATE INDEX idx_saved_addresses_user_id ON public.saved_addresses (user_id);
CREATE INDEX idx_payments_booking_id ON public.payments (booking_id);
CREATE INDEX idx_chat_messages_conv_id ON public.chat_messages (conversation_id);
CREATE INDEX idx_chat_conversations_customer ON public.chat_conversations (customer_id);
CREATE INDEX idx_chat_conversations_partner ON public.chat_conversations (partner_id);

-- ==========================================================
-- 4. ENABLE ROW LEVEL SECURITY (RLS)
-- ==========================================================
ALTER TABLE public.user_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.saved_addresses ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.bookings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.reviews ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.service_categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.app_config ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.payments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.payment_audit_log ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.chat_conversations ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.chat_messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.chat_cursors ENABLE ROW LEVEL SECURITY;

-- ==========================================================
-- 5. RLS POLICIES (Full CRUD access for anonymous and authenticated mobile client)
-- ==========================================================
CREATE POLICY "Allow public all user_profiles" ON public.user_profiles FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow public all saved_addresses" ON public.saved_addresses FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow public all bookings" ON public.bookings FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow public all reviews" ON public.reviews FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow public all service_categories" ON public.service_categories FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow public all app_config" ON public.app_config FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow public all payments" ON public.payments FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow public all payment_audit_log" ON public.payment_audit_log FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow public all chat_conversations" ON public.chat_conversations FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow public all chat_messages" ON public.chat_messages FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow public all chat_cursors" ON public.chat_cursors FOR ALL USING (true) WITH CHECK (true);

-- ==========================================================
-- 6. ENABLE SUPABASE REALTIME REPLICATION
-- ==========================================================
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_publication WHERE pubname = 'supabase_realtime') THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.bookings;
        ALTER PUBLICATION supabase_realtime ADD TABLE public.saved_addresses;
        ALTER PUBLICATION supabase_realtime ADD TABLE public.reviews;
        ALTER PUBLICATION supabase_realtime ADD TABLE public.user_profiles;
        ALTER PUBLICATION supabase_realtime ADD TABLE public.payments;
        ALTER PUBLICATION supabase_realtime ADD TABLE public.chat_messages;
        ALTER PUBLICATION supabase_realtime ADD TABLE public.chat_conversations;
    END IF;
EXCEPTION
    WHEN duplicate_object THEN NULL;
    WHEN others THEN NULL;
END $$;

-- ==========================================================
-- 7. INITIAL DEMO SEED DATA
-- ==========================================================

-- 7.1 APP CONFIG (UPI configuration for doorstep payments)
INSERT INTO public.app_config (key, value, description)
VALUES 
('upi_payee_vpa', '9105830551@upi', 'Default Servora UPI ID for QR code generation'),
('upi_payee_name', 'Servora Doorstep Services', 'Display business name on UPI Apps (GPay, PhonePe, Paytm)'),
('currency', 'INR', 'Currency code'),
('platform_fee_percent', '5', 'Standard commission fee');

-- 7.2 USERS
INSERT INTO public.user_profiles (id, name, phone, email, city, locality, role)
VALUES 
('user_priya_1', 'Priya Sharma', '+91 98765 43210', 'priya.sharma@example.com', 'Agra', 'Taj Nagri Phase 2', 'CUSTOMER'),
('user_rajesh_pro', 'Rajesh Sharma', '+91 98234 56789', 'rajesh.pro@serviceassist.in', 'Agra', 'Fatehabad Road', 'PROFESSIONAL'),
('user_admin_sneha', 'Sneha Patel', '+91 94120 11223', 'admin@serviceassist.in', 'Agra', 'Sanjay Place', 'ADMIN');

-- 7.3 SAVED ADDRESSES
INSERT INTO public.saved_addresses (id, user_id, title, full_address, locality, city, landmark, is_default)
VALUES
(1, 'user_priya_1', 'Home', 'Flat 402, Royal Residency, Taj Nagri Phase 2', 'Taj Nagri Phase 2', 'Agra', 'Near Shilpgram East Gate', true),
(2, 'user_priya_1', 'Office', 'Suite 305, Corporate Plaza, Sanjay Place', 'Sanjay Place', 'Agra', 'Opposite LIC Building', false),
(3, 'user_priya_1', 'Parents Home', 'B-14, Radhasoami Colony, Dayalbagh Main Road', 'Dayalbagh', 'Agra', 'Near Radhasoami Temple', false),
(4, 'user_priya_1', 'Farmhouse', 'Villa 7, Shamshabad Road Green Acres', 'Shamshabad Road', 'Agra', 'Behind Toll Plaza', false);

-- 7.4 SERVICE CATEGORIES
INSERT INTO public.service_categories (id, name, description, starting_price, icon_name, tag, is_featured)
VALUES
('cat_ac', 'AC & Appliance Repair', 'Deep jet foam cleaning, gas refill, repair & uninstallation', 399, 'AcUnit', 'HOT', true),
('cat_cleaning', 'Home Deep Cleaning', 'Bathroom scrub, sofa shampoo, kitchen chimney & floor buffing', 799, 'CleaningServices', 'POPULAR', true),
('cat_salon', 'Salon & Spa for Women', 'Mani-pedi, waxing, facials & haircut with hygienic disposable kits', 499, 'Spa', 'TRENDING', true),
('cat_electrician', 'Electrician & Plumber', 'Fan installation, short circuit fix, pipe leak & tap fitting', 149, 'Bolt', 'FAST 30M', true),
('cat_painting', 'Painting & Waterproofing', 'Laser wall inspection, Asian Paints Royale & seepage diagnosis', 1999, 'FormatPaint', 'WARRANTY', false),
('cat_pest', 'Pest Control', 'Eco-friendly odorless herbal gel for termites, cockroaches & bedbugs', 699, 'PestControl', 'ODORLESS', false);

-- 7.5 BOOKINGS (Active, Upcoming, and Past)
INSERT INTO public.bookings (id, customer_id, customer_name, customer_phone, booking_code, service_id, service_name, package_name, scheduled_date, scheduled_time, address_text, locality, city, total_amount, discount_amount, promo_code, payment_method, is_paid, status, professional_id, start_otp, special_notes, payment_reference, paid_at, cancellation_reason, cancellation_feedback, cancelled_at)
VALUES
(1, 'user_priya_1', 'Priya Sharma', '+91 98765 43210', 'SRV-84920', 'ac_service_deep', 'Intense AC Foam Jet Service', '1 Split AC Deep Jet Cleaning', 'Today', '02:30 PM', 'Flat 402, Royal Residency, Taj Nagri Phase 2, Agra', 'Taj Nagri Phase 2', 'Agra', 499, 100, 'FIRST20', 'Cash after service', false, 'ON_THE_WAY', 'pro_rajesh_1', '6824', 'Please ring bell twice, AC is in master bedroom.', '', NULL, '', '', NULL),
(2, 'user_priya_1', 'Priya Sharma', '+91 98765 43210', 'SRV-95012', 'plumbing_leak_fix', 'Pipe Leakage & Tap Fix', 'Drain & Pipe Repair', 'Tomorrow', '11:00 AM', 'Suite 305, Corporate Plaza, Sanjay Place, Agra', 'Sanjay Place', 'Agra', 249, 50, 'AGRA50', 'UPI (Google Pay)', true, 'ASSIGNED', 'pro_kavita_4', '3912', 'Pantry sink tap is leaking continuously.', 'PAY-UPI-95012', 1727164800000, '', '', NULL),
(3, 'user_priya_1', 'Priya Sharma', '+91 98765 43210', 'SRV-73105', 'deep_home_cleaning', 'Complete Home Deep Cleaning', '2 BHK Intensive Deep Clean', '12 Sep 2026', '10:00 AM', 'Flat 402, Royal Residency, Taj Nagri Phase 2, Agra', 'Taj Nagri Phase 2', 'Agra', 1899, 200, 'CLEAN100', 'UPI (PhonePe)', true, 'COMPLETED', 'pro_amit_2', '3194', 'Completed thoroughly with hospital-grade sanitizers.', 'PAY-UPI-73105', 1726135200000, '', '', NULL),
(4, 'user_priya_1', 'Priya Sharma', '+91 98765 43210', 'SRV-61294', 'salon_women_glow', 'Glow Facial & Mani-Pedi', 'Bridal Radiance Package', '08 Sep 2026', '04:00 PM', 'B-14, Radhasoami Colony, Dayalbagh Main Road, Agra', 'Dayalbagh', 'Agra', 1299, 150, 'FESTIVE15', 'Card Payment', true, 'COMPLETED', 'pro_meera_3', '8401', 'Disposable sterilized equipment used.', 'PAY-CARD-61294', 1725793200000, '', '', NULL),
(5, 'user_priya_1', 'Priya Sharma', '+91 98765 43210', 'SRV-54911', 'electrician_instant', 'Electrical Short Circuit & Wiring', 'Emergency Repair', '01 Sep 2026', '06:30 PM', 'Villa 7, Shamshabad Road Green Acres, Agra', 'Shamshabad Road', 'Agra', 399, 0, '', 'Cash after service', true, 'COMPLETED', 'pro_rajesh_1', '1928', 'Fixed main MCB trip and neutralized earthing.', 'PAY-CASH-54911', 1725195000000, '', '', NULL);

-- 7.6 CUSTOMER REVIEWS
INSERT INTO public.reviews (id, service_id, service_name, professional_name, customer_name, rating, comment, tags, date_text)
VALUES
(1, 'ac_service_deep', 'Intense AC Foam Jet Service', 'Rajesh Sharma', 'Ananya V.', 5.0, 'Brilliant service! Rajesh brought proper foam jet pressure equipment and spill-jacket. AC cooling is ice-cold now, zero mess left on the wall.', 'Punctual, Super Clean, Expert', '2 days ago'),
(2, 'deep_home_cleaning', 'Complete Home Deep Cleaning', 'Amit Kumar & Team', 'Vikram Singhania', 4.9, 'Booked for our home in Fatehabad Road Agra before family arrived. Every bathroom tile, balcony rail, and kitchen chimney was scrubbed mirror-shine.', 'Detail Oriented, Professional', 'Last week'),
(3, 'salon_women_glow', 'Glow Facial & Mani-Pedi', 'Meera Saxena', 'Pooja Agarwal', 5.0, 'Meera brought all 100% sanitized disposable kits. The facial massage was so relaxing right in my living room in Dayalbagh. Will book again!', 'Hygienic, Gentle, Punctual', '3 days ago'),
(4, 'plumbing_leak_fix', 'Pipe Leakage & Tap Fix', 'Kavita Singh', 'Rohan Gupta', 4.8, 'Replaced the old rusted angle valve under the kitchen sink in 20 minutes. Clean plumbing work and upfront transparent pricing.', 'Fast Arrival, Fair Price', '5 days ago'),
(5, 'electrician_instant', 'Electrical Short Circuit & Wiring', 'Rajesh Sharma', 'Sunil Mathur', 5.0, 'Our main MCB kept tripping due to heavy geyser load. The electrician diagnosed a neutral wire short and re-terminated it safely within half an hour.', 'Knowledgeable, Safe Work', '1 week ago'),
(6, 'cat_pest', 'Herbal Pest Control', 'Dinesh Rawat', 'Dr. Alok Verma', 5.0, 'Odorless termite and cockroach gel treatment throughout our clinic. No unpleasant smell, very safe for patients.', 'Odorless, Long Lasting', '2 weeks ago');

-- ==========================================================
-- 8. SERIAL SEQUENCES & SCHEMA CACHE RELOAD
-- ==========================================================
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
