-- ============================================================================
-- SUPABASE SEPARATE SCHEMA FOR WEBSITE CONTENT (ServiceAssist / Servora Web)
-- ============================================================================

-- 1. Create dedicated isolated schema for website content
CREATE SCHEMA IF NOT EXISTS website;

-- Grant schema usage to standard Supabase roles
GRANT USAGE ON SCHEMA website TO postgres, anon, authenticated, service_role;
ALTER DEFAULT PRIVILEGES IN SCHEMA website GRANT ALL ON TABLES TO postgres, service_role;
ALTER DEFAULT PRIVILEGES IN SCHEMA website GRANT SELECT ON TABLES TO anon, authenticated;

-- ============================================================================
-- 2. WEBSITE CATEGORIES
-- ============================================================================
CREATE TABLE IF NOT EXISTS website.categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    slug TEXT NOT NULL UNIQUE,
    description TEXT NOT NULL DEFAULT '',
    image TEXT NOT NULL,
    icon TEXT NOT NULL DEFAULT 'Sparkles',
    color TEXT NOT NULL DEFAULT '#0B9F6E',
    bg_pastel TEXT NOT NULL DEFAULT '#DDF7EC',
    badge TEXT,
    display_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================================
-- 3. WEBSITE SERVICES & PACKAGES
-- ============================================================================
CREATE TABLE IF NOT EXISTS website.services (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id UUID NOT NULL REFERENCES website.categories(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    slug TEXT NOT NULL UNIQUE,
    description TEXT NOT NULL,
    short_desc TEXT NOT NULL DEFAULT '',
    subcategory TEXT NOT NULL DEFAULT 'General',
    price_type TEXT NOT NULL DEFAULT 'FIXED',
    service_type TEXT NOT NULL DEFAULT 'HOME_VISIT',
    starting_price NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    duration_min INT NOT NULL DEFAULT 60,
    rating NUMERIC(3, 2) NOT NULL DEFAULT 4.8,
    reviews_count INT NOT NULL DEFAULT 0,
    is_popular BOOLEAN NOT NULL DEFAULT false,
    is_trending BOOLEAN NOT NULL DEFAULT false,
    is_active BOOLEAN NOT NULL DEFAULT true,
    image TEXT NOT NULL,
    gallery_images TEXT[] NOT NULL DEFAULT '{}',
    what_included TEXT[] NOT NULL DEFAULT '{}',
    what_excluded TEXT[] NOT NULL DEFAULT '{}',
    why_choose TEXT[] NOT NULL DEFAULT '{}',
    steps TEXT[] NOT NULL DEFAULT '{}',
    warranty_policy TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Service Variants / Pricing Tiers
CREATE TABLE IF NOT EXISTS website.service_variants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_id UUID NOT NULL REFERENCES website.services(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    price NUMERIC(12, 2) NOT NULL,
    original_price NUMERIC(12, 2),
    duration_min INT NOT NULL DEFAULT 60,
    description TEXT NOT NULL DEFAULT '',
    included TEXT[] NOT NULL DEFAULT '{}',
    is_popular BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Service FAQs
CREATE TABLE IF NOT EXISTS website.service_faqs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_id UUID NOT NULL REFERENCES website.services(id) ON DELETE CASCADE,
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    display_order INT NOT NULL DEFAULT 0
);

-- ============================================================================
-- 4. WEBSITE OFFERS & PROMOTIONS
-- ============================================================================
CREATE TABLE IF NOT EXISTS website.offers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code TEXT NOT NULL UNIQUE,
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    discount_type TEXT NOT NULL DEFAULT 'PERCENTAGE', -- PERCENTAGE or FIXED
    value NUMERIC(12, 2) NOT NULL,
    min_booking_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    max_discount NUMERIC(12, 2),
    badge TEXT DEFAULT 'SPECIAL DEAL',
    banner_image TEXT,
    bg_gradient TEXT DEFAULT 'from-emerald-500 to-teal-600',
    expiry TIMESTAMPTZ NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================================
-- 5. WEBSITE HERO BANNERS & PROMOTION SECTIONS
-- ============================================================================
CREATE TABLE IF NOT EXISTS website.hero_banners (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title TEXT NOT NULL,
    subtitle TEXT NOT NULL,
    tagline TEXT DEFAULT 'VERIFIED EXPERTS AT YOUR DOORSTEP',
    button_text TEXT DEFAULT 'Book Now',
    button_link TEXT DEFAULT '/services',
    image_url TEXT NOT NULL,
    bg_color TEXT DEFAULT '#009051',
    is_active BOOLEAN NOT NULL DEFAULT true,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Customer Reviews & Testimonials for Website
CREATE TABLE IF NOT EXISTS website.testimonials (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    author_name TEXT NOT NULL,
    author_location TEXT NOT NULL,
    author_avatar TEXT,
    service_name TEXT NOT NULL,
    rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    review_text TEXT NOT NULL,
    verified BOOLEAN NOT NULL DEFAULT true,
    display_order INT NOT NULL DEFAULT 0,
    is_featured BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- General FAQs
CREATE TABLE IF NOT EXISTS website.general_faqs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category TEXT NOT NULL DEFAULT 'General',
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT true
);

-- ============================================================================
-- 6. ENABLE ROW LEVEL SECURITY (RLS)
-- ============================================================================
ALTER TABLE website.categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE website.services ENABLE ROW LEVEL SECURITY;
ALTER TABLE website.service_variants ENABLE ROW LEVEL SECURITY;
ALTER TABLE website.service_faqs ENABLE ROW LEVEL SECURITY;
ALTER TABLE website.offers ENABLE ROW LEVEL SECURITY;
ALTER TABLE website.hero_banners ENABLE ROW LEVEL SECURITY;
ALTER TABLE website.testimonials ENABLE ROW LEVEL SECURITY;
ALTER TABLE website.general_faqs ENABLE ROW LEVEL SECURITY;

-- Read policies for public visitors (anon & authenticated)
CREATE POLICY "Public read categories" ON website.categories FOR SELECT USING (is_active = true);
CREATE POLICY "Public read services" ON website.services FOR SELECT USING (is_active = true);
CREATE POLICY "Public read variants" ON website.service_variants FOR SELECT USING (true);
CREATE POLICY "Public read service faqs" ON website.service_faqs FOR SELECT USING (true);
CREATE POLICY "Public read offers" ON website.offers FOR SELECT USING (is_active = true AND expiry > now());
CREATE POLICY "Public read hero banners" ON website.hero_banners FOR SELECT USING (is_active = true);
CREATE POLICY "Public read testimonials" ON website.testimonials FOR SELECT USING (is_featured = true);
CREATE POLICY "Public read general faqs" ON website.general_faqs FOR SELECT USING (is_active = true);

-- Full management policies for service_role / backend admin
CREATE POLICY "Admin manage categories" ON website.categories FOR ALL TO service_role USING (true) WITH CHECK (true);
CREATE POLICY "Admin manage services" ON website.services FOR ALL TO service_role USING (true) WITH CHECK (true);
CREATE POLICY "Admin manage variants" ON website.service_variants FOR ALL TO service_role USING (true) WITH CHECK (true);
CREATE POLICY "Admin manage faqs" ON website.service_faqs FOR ALL TO service_role USING (true) WITH CHECK (true);
CREATE POLICY "Admin manage offers" ON website.offers FOR ALL TO service_role USING (true) WITH CHECK (true);
CREATE POLICY "Admin manage banners" ON website.hero_banners FOR ALL TO service_role USING (true) WITH CHECK (true);
CREATE POLICY "Admin manage testimonials" ON website.testimonials FOR ALL TO service_role USING (true) WITH CHECK (true);
CREATE POLICY "Admin manage general faqs" ON website.general_faqs FOR ALL TO service_role USING (true) WITH CHECK (true);

-- ============================================================================
-- 7. SEED INITIAL WEBSITE CONTENT
-- ============================================================================

-- Categories Seed
INSERT INTO website.categories (name, slug, description, image, icon, color, bg_pastel, badge, display_order)
VALUES
('AC & Appliances', 'ac-appliances', 'Repair, servicing, and installation for all major household appliances', '/service-images/appliances.svg', 'Snowflake', '#2563EB', '#DBEAFE', 'Trending', 1),
('Cleaning & Pest', 'cleaning-pest', 'Deep home cleaning, kitchen & bathroom sanitation, and pest control', '/service-images/cleaning.svg', 'Sparkles', '#059669', '#D1FAE5', 'Popular', 2),
('Electrician & Plumber', 'electrician-plumber', 'Fix wiring, switchboards, leakages, and bathroom fixtures in 30 mins', '/service-images/electrical.svg', 'Zap', '#D97706', '#FEF3C7', 'Quick Help', 3),
('Women''s Salon & Spa', 'womens-salon', 'Facials, waxing, manicure, pedicure, and bridal packages at home', '/service-images/beauty.svg', 'Heart', '#DB2777', '#FCE7F3', 'Top Rated', 4),
('Men''s Grooming', 'mens-grooming', 'Haircuts, beard styling, massage, and facial care tailored for men', '/service-images/hair.svg', 'Scissors', '#4F46E5', '#E0E7FF', 'New', 5),
('RO & Water Purifier', 'ro-purifier', 'Comprehensive RO water purifier servicing, filter replacement & repairs', '/service-images/ro-purifier/servicing.png', 'Droplets', '#0284C7', '#E0F2FE', 'Essential', 6),
('Painting & Wall Care', 'painting', 'Interior/exterior wall painting, waterproofing, and texture finish', '/service-images/smart-home.svg', 'Paintbrush', '#7C3AED', '#EDE9FE', 'Guaranteed', 7),
('Carpentry & Furniture', 'carpentry', 'Custom woodwork, furniture repairs, lock replacement, and installations', '/service-images/electronics.svg', 'Hammer', '#EA580C', '#FFEDD5', 'Expert Craft', 8)
ON CONFLICT (slug) DO NOTHING;

-- Offers Seed
INSERT INTO website.offers (code, title, description, discount_type, value, min_booking_amount, max_discount, badge, bg_gradient, expiry)
VALUES
('SERVORA50', 'Flat 50% Off First Booking', 'Get flat 50% discount up to ₹150 on your first service booking.', 'PERCENTAGE', 50.00, 299.00, 150.00, 'WELCOME DEAL', 'from-emerald-500 to-teal-600', now() + interval '90 days'),
('SUMMERAC', 'AC Service Festival ₹200 Off', 'Beat the heat! Save ₹200 on all AC deep cleans and repairs.', 'FIXED', 200.00, 499.00, 200.00, 'AC SPECIAL', 'from-blue-500 to-cyan-600', now() + interval '60 days'),
('CLEANPRO', '25% Off Full Home Cleaning', 'Get instant 25% off on deep home cleaning and sanitation.', 'PERCENTAGE', 25.00, 999.00, 500.00, 'BESTSELLER', 'from-amber-500 to-orange-600', now() + interval '45 days'),
('PRIMECARE', '₹100 Off On Grooming & Spa', 'Relax and recharge at home with ₹100 off on salon services.', 'FIXED', 100.00, 399.00, 100.00, 'SELF CARE', 'from-rose-500 to-pink-600', now() + interval '30 days')
ON CONFLICT (code) DO NOTHING;

-- Testimonials Seed
INSERT INTO website.testimonials (author_name, author_location, service_name, rating, review_text, verified, display_order)
VALUES
('Priya Sharma', 'Agra, UP', 'AC Foam Jet Servicing', 5, 'The technician arrived exactly on time and cleaned the AC thoroughly. Cooling is 100% better now!', true, 1),
('Rahul Verma', 'Noida, Sector 62', 'Deep Kitchen Cleaning', 5, 'Exceptional attention to detail. Every corner of the kitchen looks brand new. Highly recommended!', true, 2),
('Ananya Patel', 'Delhi NCR', 'Salon Prime Facial & Spa', 5, 'Super hygienic setup, polite beautician, and very relaxing experience right in my living room.', true, 3),
('Vikram Singh', 'Jaipur, Rajasthan', 'RO Water Purifier Repair', 5, 'Fast diagnostics, genuine filter replacements, and instant resolution. Transparent pricing.', true, 4);
