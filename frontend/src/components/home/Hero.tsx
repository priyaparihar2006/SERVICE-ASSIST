import React, { useState, useEffect, useRef, useCallback } from 'react';
import {
  Star,
  ShieldCheck,
  CheckCircle2,
  Clock,
  Users,
  ArrowRight,
  ChevronLeft,
  ChevronRight,
  Sparkles,
  Briefcase,
  Award,
  MapPin,
} from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';
import { ImageWithFallback } from '../common/ImageWithFallback';

interface HeroProps {
  onExplore: () => void;
  onBook: () => void;
}

export interface HeroServiceItem {
  id: string;
  serviceName: string;
  category: string;
  image: string;
  alt: string;
  professional: string;
  role: string;
  rating: string;
  reviewsCount: string;
  avatar: string;
  eta: string;
  startingPrice: string;
}

// Exact 15-service sequence requested in prompt:
// 1. AC Repair, 2. Home Cleaning, 3. Salon & Beauty, 4. Electrician, 5. Plumber,
// 6. Carpenter, 7. Pest Control, 8. Painting, 9. Appliance Repair, 10. Bathroom Cleaning,
// 11. Sofa Cleaning, 12. Packers & Movers, 13. Home Improvement, 14. Water Purifier, 15. Electronics Repair
export const HERO_SERVICES: HeroServiceItem[] = [
  {
    id: 'ac-repair',
    serviceName: 'AC Repair & Jet Servicing',
    category: 'AC & Appliances',
    image: 'https://images.unsplash.com/photo-1621905251189-08b45d6a269e?auto=format&fit=crop&w=1200&q=80',
    alt: 'AC technician servicing an indoor air conditioner unit',
    professional: 'Rahul S.',
    role: 'HVAC Specialist',
    rating: '4.9',
    reviewsCount: '14,200+',
    avatar: 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=150&q=80',
    eta: '15 mins',
    startingPrice: '₹499',
  },
  {
    id: 'home-cleaning',
    serviceName: 'Home Cleaning',
    category: 'Home Cleaning',
    image: 'https://images.unsplash.com/photo-1581578731548-c64695cc6952?auto=format&fit=crop&w=1200&q=80',
    alt: 'Professional cleaner providing home cleaning service in living room',
    professional: 'Anjali M.',
    role: 'Cleaning Specialist',
    rating: '4.9',
    reviewsCount: '11,450+',
    avatar: 'https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?auto=format&fit=crop&w=150&q=80',
    eta: '20 mins',
    startingPrice: '₹999',
  },
  {
    id: 'salon-beauty',
    serviceName: 'Salon & Beauty at Home',
    category: 'Salon & Beauty',
    image: 'https://images.unsplash.com/photo-1560066984-138dadb4c035?auto=format&fit=crop&w=1200&q=80',
    alt: 'Professional salon stylist providing beauty care at home',
    professional: 'Neha S.',
    role: 'Beauty Specialist',
    rating: '4.9',
    reviewsCount: '18,800+',
    avatar: 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&w=150&q=80',
    eta: '25 mins',
    startingPrice: '₹799',
  },
  {
    id: 'electrician',
    serviceName: 'Electrician & Wiring',
    category: 'Electrician',
    image: 'https://images.unsplash.com/photo-1621905252507-b35492cc74b4?auto=format&fit=crop&w=1200&q=80',
    alt: 'Professional electrician repairing home electrical system',
    professional: 'Amit P.',
    role: 'Electrical Specialist',
    rating: '4.9',
    reviewsCount: '16,700+',
    avatar: 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?auto=format&fit=crop&w=150&q=80',
    eta: '12 mins',
    startingPrice: '₹199',
  },
  {
    id: 'plumber',
    serviceName: 'Plumber & Leak Fix',
    category: 'Plumber',
    image: 'https://images.unsplash.com/photo-1585704032915-c3400ca199e7?auto=format&fit=crop&w=1200&q=80',
    alt: 'Professional plumber repairing pipe and tap fittings',
    professional: 'Suresh Y.',
    role: 'Plumbing Specialist',
    rating: '4.8',
    reviewsCount: '13,100+',
    avatar: 'https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?auto=format&fit=crop&w=150&q=80',
    eta: '15 mins',
    startingPrice: '₹149',
  },
  {
    id: 'carpenter',
    serviceName: 'Carpenter & Woodwork',
    category: 'Carpenter',
    image: 'https://images.unsplash.com/photo-1504148455328-c376907d081c?auto=format&fit=crop&w=1200&q=80',
    alt: 'Professional carpenter working on wooden furniture',
    professional: 'Dinesh L.',
    role: 'Carpentry Specialist',
    rating: '4.9',
    reviewsCount: '8,600+',
    avatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=150&q=80',
    eta: '30 mins',
    startingPrice: '₹249',
  },
  {
    id: 'pest-control',
    serviceName: 'Pest Control Treatment',
    category: 'Pest Control',
    image: 'https://images.unsplash.com/photo-1584820927498-cfe5211fd8bf?auto=format&fit=crop&w=1200&q=80',
    alt: 'Professional pest-control technician inspecting residential home',
    professional: 'Manish T.',
    role: 'Pest Control Specialist',
    rating: '4.8',
    reviewsCount: '7,400+',
    avatar: 'https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?auto=format&fit=crop&w=150&q=80',
    eta: '22 mins',
    startingPrice: '₹699',
  },
  {
    id: 'painting',
    serviceName: 'Wall Painting & Waterproofing',
    category: 'Painting',
    image: 'https://images.unsplash.com/photo-1589939705384-5185137a7f0f?auto=format&fit=crop&w=1200&q=80',
    alt: 'Professional painter applying fresh coat on interior wall',
    professional: 'Karan V.',
    role: 'Master Painter',
    rating: '4.9',
    reviewsCount: '6,900+',
    avatar: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=150&q=80',
    eta: '45 mins',
    startingPrice: '₹1,499',
  },
  {
    id: 'appliance-repair',
    serviceName: 'Appliance Repair',
    category: 'Appliance Repair',
    image: 'https://images.unsplash.com/photo-1581092160607-ee22621dd758?auto=format&fit=crop&w=1200&q=80',
    alt: 'Technician repairing washing machine and refrigerator appliance',
    professional: 'Vikram J.',
    role: 'Appliance Engineer',
    rating: '4.8',
    reviewsCount: '9,100+',
    avatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=150&q=80',
    eta: '20 mins',
    startingPrice: '₹299',
  },
  {
    id: 'bathroom-cleaning',
    serviceName: 'Bathroom Deep Cleaning',
    category: 'Bathroom Cleaning',
    image: 'https://images.unsplash.com/photo-1584622650111-993a426fbf0a?auto=format&fit=crop&w=1200&q=80',
    alt: 'Professional cleaning technician descaling bathroom tiles',
    professional: 'Pooja R.',
    role: 'Sanitization Expert',
    rating: '4.9',
    reviewsCount: '15,300+',
    avatar: 'https://images.unsplash.com/photo-1580489944761-15a19d654956?auto=format&fit=crop&w=150&q=80',
    eta: '18 mins',
    startingPrice: '₹449',
  },
  {
    id: 'sofa-cleaning',
    serviceName: 'Sofa & Fabric Cleaning',
    category: 'Sofa Cleaning',
    image: 'https://images.unsplash.com/photo-1527515637462-cff94eecc1ac?auto=format&fit=crop&w=1200&q=80',
    alt: 'Professional deep shampooing fabric sofa upholstery',
    professional: 'Sameer K.',
    role: 'Upholstery Specialist',
    rating: '4.8',
    reviewsCount: '8,400+',
    avatar: 'https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?auto=format&fit=crop&w=150&q=80',
    eta: '25 mins',
    startingPrice: '₹599',
  },
  {
    id: 'packers-movers',
    serviceName: 'Packers & Movers',
    category: 'Packers & Movers',
    image: 'https://images.unsplash.com/photo-1600585154526-990dced4db0d?auto=format&fit=crop&w=1200&q=80',
    alt: 'Household packers and movers shifting furniture safely',
    professional: 'Harish N.',
    role: 'Relocation Lead',
    rating: '4.9',
    reviewsCount: '5,200+',
    avatar: 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?auto=format&fit=crop&w=150&q=80',
    eta: '60 mins',
    startingPrice: '₹1,899',
  },
  {
    id: 'home-improvement',
    serviceName: 'Home Improvement & Lighting',
    category: 'Home Improvement',
    image: 'https://images.unsplash.com/photo-1513694203232-719a280e022f?auto=format&fit=crop&w=1200&q=80',
    alt: 'Modern living room renovation and architectural lighting',
    professional: 'Arjun B.',
    role: 'Interior Craftsman',
    rating: '4.9',
    reviewsCount: '4,280+',
    avatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=150&q=80',
    eta: '30 mins',
    startingPrice: '₹499',
  },
  {
    id: 'water-purifier',
    serviceName: 'Water Purifier & RO Service',
    category: 'Water Purifier',
    image: 'https://images.unsplash.com/photo-1548839140-29a749e1bc4e?auto=format&fit=crop&w=1200&q=80',
    alt: 'Technician inspecting and replacing filters on a water purifier',
    professional: 'Deepak G.',
    role: 'Water Quality Tech',
    rating: '4.9',
    reviewsCount: '8,200+',
    avatar: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=150&q=80',
    eta: '20 mins',
    startingPrice: '₹399',
  },
  {
    id: 'electronics-repair',
    serviceName: 'Electronics & Laptop Repair',
    category: 'Electronics Repair',
    image: 'https://images.unsplash.com/photo-1588872657578-7efd1f1555ed?auto=format&fit=crop&w=1200&q=80',
    alt: 'Technician testing motherboard and laptop hardware diagnostics',
    professional: 'Prashant M.',
    role: 'Hardware Engineer',
    rating: '4.8',
    reviewsCount: '5,120+',
    avatar: 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=150&q=80',
    eta: '35 mins',
    startingPrice: '₹349',
  },
];

export const Hero: React.FC<HeroProps> = ({ onExplore, onBook }) => {
  const [currentIndex, setCurrentIndex] = useState(0);
  const [isPaused, setIsPaused] = useState(false);
  const timerRef = useRef<NodeJS.Timeout | null>(null);

  // Preload next images to prevent flickering
  useEffect(() => {
    const nextIdx = (currentIndex + 1) % HERO_SERVICES.length;
    const img = new Image();
    img.src = HERO_SERVICES[nextIdx].image;
  }, [currentIndex]);

  // Strict 1-second auto-cycle (1000ms)
  const nextSlide = useCallback(() => {
    setCurrentIndex((prev) => (prev + 1) % HERO_SERVICES.length);
  }, []);

  const prevSlide = useCallback(() => {
    setCurrentIndex((prev) => (prev - 1 + HERO_SERVICES.length) % HERO_SERVICES.length);
  }, []);

  useEffect(() => {
    if (isPaused) return;
    timerRef.current = setInterval(() => {
      nextSlide();
    }, 1000);

    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, [isPaused, nextSlide]);

  const handleIndicatorClick = (idx: number) => {
    setCurrentIndex(idx);
  };

  const handlePrevClick = (e: React.MouseEvent) => {
    e.stopPropagation();
    prevSlide();
  };

  const handleNextClick = (e: React.MouseEvent) => {
    e.stopPropagation();
    nextSlide();
  };

  const current = HERO_SERVICES[currentIndex];

  return (
    <section className="relative overflow-hidden bg-gradient-to-b from-[#F2FCF7] via-white to-[#F2FCF7]/40 pt-10 sm:pt-16 pb-14 sm:pb-20 border-b border-[#DDF7EC]">
      {/* Subtle organic green ambient glow in background */}
      <div className="absolute top-0 right-1/4 w-96 h-96 bg-[#DDF7EC]/70 rounded-full blur-3xl pointer-events-none -z-10" />
      <div className="absolute bottom-10 left-10 w-80 h-80 bg-[#19C995]/10 rounded-full blur-3xl pointer-events-none -z-10" />

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-10 lg:gap-8 items-center">
          {/* Left Column: Typography & CTAs */}
          <div className="lg:col-span-6 space-y-6 sm:space-y-8">
            {/* Small Trust Badge */}
            <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-[#DDF7EC] border border-[#0B9F6E]/30 text-[#087F5B] text-xs font-bold shadow-2xs">
              <span className="w-2 h-2 rounded-full bg-[#0B9F6E] animate-pulse" />
              <ShieldCheck className="w-4 h-4 text-[#0B9F6E]" />
              <span>Trusted professionals. Right at your doorstep.</span>
            </div>

            {/* Headline */}
            <div className="space-y-2">
              <h1 className="text-4xl sm:text-5xl lg:text-6xl font-black text-[#103C35] tracking-tight leading-[1.08] font-['Outfit']">
                Your home, <br />
                <span className="text-[#0B9F6E] relative inline-block">
                  taken care of.
                  <svg
                    className="absolute -bottom-2 left-0 w-full text-[#19C995]/40 -z-10"
                    height="8"
                    viewBox="0 0 100 8"
                    preserveAspectRatio="none"
                  >
                    <path
                      d="M0 6 Q 50 0, 100 6"
                      stroke="currentColor"
                      strokeWidth="5"
                      fill="none"
                      strokeLinecap="round"
                    />
                  </svg>
                </span>
              </h1>

              <p className="text-sm sm:text-base text-[#6B817C] leading-relaxed max-w-xl pt-2 font-normal">
                From cleaning and repairs to beauty and home improvement, book trusted professionals whenever you need them.
              </p>
            </div>

            {/* Buttons */}
            <div className="flex flex-wrap items-center gap-3 pt-1">
              <button
                id="hero-book-btn"
                onClick={onBook}
                className="flex items-center justify-center gap-2 px-7 py-3.5 bg-[#087F5B] hover:bg-[#0B9F6E] text-white font-bold text-sm rounded-2xl transition-all shadow-lg shadow-[#087F5B]/20 hover:shadow-[#0B9F6E]/35 hover:-translate-y-0.5 active:translate-y-0 cursor-pointer"
              >
                <span>Book a Service</span>
                <ArrowRight className="w-4 h-4" />
              </button>

              <button
                id="hero-explore-btn"
                onClick={onExplore}
                className="px-6 py-3.5 rounded-2xl border-2 border-[#0B9F6E] bg-white hover:bg-[#F2FCF7] text-[#087F5B] hover:text-[#0B9F6E] font-bold text-sm transition-all shadow-2xs cursor-pointer"
              >
                Explore All Services
              </button>
            </div>

            {/* Trust Metrics Bar: 4 Metrics below Hero */}
            <div className="pt-6 border-t border-[#DDF7EC] grid grid-cols-2 sm:grid-cols-4 gap-4">
              <div>
                <div className="flex items-center gap-1.5 text-[#087F5B] font-black text-xl font-['Outfit']">
                  <Star className="w-4.5 h-4.5 fill-[#0B9F6E] text-[#0B9F6E]" />
                  <span>4.8+</span>
                </div>
                <p className="text-xs text-[#6B817C] font-medium mt-0.5">Average Rating</p>
              </div>

              <div>
                <div className="flex items-center gap-1.5 text-[#103C35] font-black text-xl font-['Outfit']">
                  <Users className="w-4.5 h-4.5 text-[#0B9F6E]" />
                  <span>50,000+</span>
                </div>
                <p className="text-xs text-[#6B817C] font-medium mt-0.5">Verified Professionals</p>
              </div>

              <div>
                <div className="flex items-center gap-1.5 text-[#103C35] font-black text-xl font-['Outfit']">
                  <Briefcase className="w-4.5 h-4.5 text-[#0B9F6E]" />
                  <span>1.2M+</span>
                </div>
                <p className="text-xs text-[#6B817C] font-medium mt-0.5">Services Completed</p>
              </div>

              <div>
                <div className="flex items-center gap-1.5 text-[#087F5B] font-black text-xl font-['Outfit']">
                  <ShieldCheck className="w-4.5 h-4.5 text-[#0B9F6E]" />
                  <span>30 Days</span>
                </div>
                <p className="text-xs text-[#6B817C] font-medium mt-0.5">Service Warranty</p>
              </div>
            </div>
          </div>

          {/* Right Column: 1-Second Synchronized Image & Text Carousel */}
          <div className="lg:col-span-6 relative">
            <div
              className="relative mx-auto max-w-lg lg:max-w-none group/carousel"
              onMouseEnter={() => setIsPaused(true)}
              onMouseLeave={() => setIsPaused(false)}
            >
              {/* Soft green ambient background glow */}
              <div className="absolute -inset-4 bg-gradient-to-tr from-[#0B9F6E]/20 via-[#19C995]/15 to-transparent rounded-[3rem] blur-2xl -z-10" />

              {/* Main Image Frame (object-cover, 500-700ms smooth transition) */}
              <div className="relative rounded-[2.5rem] overflow-hidden shadow-2xl border-4 border-white bg-[#DDF7EC]/50 aspect-4/3 sm:aspect-5/4">
                <AnimatePresence mode="popLayout">
                  <motion.div
                    key={current.id}
                    initial={{ opacity: 0, scale: 1.03, x: 6 }}
                    animate={{ opacity: 1, scale: 1, x: 0 }}
                    exit={{ opacity: 0, scale: 0.98, x: -6 }}
                    transition={{ duration: 0.6, ease: [0.25, 1, 0.5, 1] }}
                    className="absolute inset-0 w-full h-full"
                  >
                    <ImageWithFallback
                      src={current.image}
                      alt={current.alt}
                      fallbackTitle={current.serviceName}
                      className="w-full h-full object-cover object-center"
                    />
                  </motion.div>
                </AnimatePresence>

                {/* Subtle gradient overlay for contrast */}
                <div className="absolute inset-0 bg-gradient-to-t from-black/65 via-black/15 to-transparent pointer-events-none" />

                {/* Top Badge: Category Pill */}
                <div className="absolute top-4 left-4 right-4 flex items-center justify-between z-10 pointer-events-none">
                  <div className="bg-[#103C35]/90 backdrop-blur-md text-white rounded-full px-3.5 py-1.5 shadow-lg flex items-center gap-1.5 text-xs font-bold border border-[#19C995]/30">
                    <CheckCircle2 className="w-3.5 h-3.5 text-[#19C995]" />
                    <span>Verified Professional ✓</span>
                  </div>

                  <span className="bg-[#0B9F6E] text-white font-bold text-[11px] px-3 py-1 rounded-full shadow-md">
                    {current.category}
                  </span>
                </div>

                {/* Navigation Arrows: Left & Right buttons directly on image */}
                <button
                  onClick={handlePrevClick}
                  aria-label="Previous Slide"
                  className="absolute left-3 top-1/2 -translate-y-1/2 z-20 w-9 h-9 sm:w-10 sm:h-10 rounded-full bg-black/40 hover:bg-[#087F5B] text-white backdrop-blur-md flex items-center justify-center transition-all opacity-80 group-hover/carousel:opacity-100 cursor-pointer hover:scale-105 active:scale-95 border border-white/20"
                >
                  <ChevronLeft className="w-5 h-5" />
                </button>

                <button
                  onClick={handleNextClick}
                  aria-label="Next Slide"
                  className="absolute right-3 top-1/2 -translate-y-1/2 z-20 w-9 h-9 sm:w-10 sm:h-10 rounded-full bg-black/40 hover:bg-[#087F5B] text-white backdrop-blur-md flex items-center justify-center transition-all opacity-80 group-hover/carousel:opacity-100 cursor-pointer hover:scale-105 active:scale-95 border border-white/20"
                >
                  <ChevronRight className="w-5 h-5" />
                </button>

                {/* Bottom Service Title on Image */}
                <div className="absolute bottom-4 left-4 right-4 z-10 text-white pointer-events-none">
                  <AnimatePresence mode="wait">
                    <motion.div
                      key={current.id}
                      initial={{ opacity: 0, y: 4 }}
                      animate={{ opacity: 1, y: 0 }}
                      exit={{ opacity: 0, y: -4 }}
                      transition={{ duration: 0.3 }}
                      className="flex items-end justify-between"
                    >
                      <div>
                        <span className="text-[10px] font-bold uppercase tracking-wider text-[#DDF7EC] block">
                          Service Assist Doorstep
                        </span>
                        <h3 className="text-xl sm:text-2xl font-black text-white drop-shadow-sm font-['Outfit']">
                          {current.serviceName}
                        </h3>
                      </div>
                      <div className="text-right">
                        <span className="text-[10px] text-gray-200 block font-medium">Starts from</span>
                        <span className="text-lg font-black text-[#19C995] drop-shadow-sm">
                          {current.startingPrice}
                        </span>
                      </div>
                    </motion.div>
                  </AnimatePresence>
                </div>
              </div>

              {/* Synchronized Floating Information Card (as requested in Section 8) */}
              <AnimatePresence mode="wait">
                <motion.div
                  key={current.id}
                  initial={{ opacity: 0, y: 8, scale: 0.96 }}
                  animate={{ opacity: 1, y: 0, scale: 1 }}
                  exit={{ opacity: 0, y: -8, scale: 0.96 }}
                  transition={{ duration: 0.35 }}
                  className="absolute -bottom-6 -left-2 sm:left-4 bg-white/95 backdrop-blur-md rounded-2xl p-3.5 sm:p-4 shadow-xl border border-[#DDF7EC] flex items-center gap-3.5 z-20"
                >
                  <div className="relative shrink-0">
                    <img
                      src={current.avatar}
                      alt={current.professional}
                      className="w-12 h-12 rounded-xl object-cover border-2 border-[#0B9F6E]"
                      referrerPolicy="no-referrer"
                    />
                    <span className="absolute -bottom-1 -right-1 w-3.5 h-3.5 bg-[#0B9F6E] rounded-full border-2 border-white ring-1 ring-[#19C995]" />
                  </div>
                  <div>
                    {/* Synchronized Card details */}
                    <div className="flex items-center gap-1.5 text-[10px] font-bold text-[#087F5B] uppercase tracking-wider">
                      <span className="text-emerald-700">Verified Professional ✓</span>
                      <span>•</span>
                      <span className="text-[#0B9F6E]">{current.category}</span>
                    </div>

                    <h4 className="font-extrabold text-xs sm:text-sm text-[#103C35]">
                      {current.professional}
                      <span className="font-normal text-[#6B817C] text-xs ml-1">
                        — {current.role}
                      </span>
                    </h4>

                    <div className="flex items-center gap-2 mt-0.5">
                      <div className="flex items-center gap-0.5 text-[#087F5B] font-bold text-xs">
                        <Star className="w-3.5 h-3.5 fill-[#0B9F6E] text-[#0B9F6E]" />
                        <span>★★★★★ {current.rating}</span>
                      </div>
                      <span className="text-[10px] text-[#0B9F6E] font-bold flex items-center gap-0.5 bg-[#DDF7EC] px-1.5 py-0.5 rounded-md">
                        <Clock className="w-2.5 h-2.5" />
                        Available Near You
                      </span>
                    </div>
                  </div>
                </motion.div>
              </AnimatePresence>

              {/* Synchronized Floating Card 2: Rating Pill (Top-right) */}
              <div className="absolute top-6 -right-2 sm:right-4 bg-white/95 backdrop-blur-md rounded-2xl p-3 shadow-xl border border-[#DDF7EC] flex items-center gap-2.5 z-20">
                <div className="w-9 h-9 rounded-xl bg-[#DDF7EC] flex items-center justify-center text-[#087F5B]">
                  <Award className="w-5 h-5 stroke-[2.2]" />
                </div>
                <div>
                  <div className="font-extrabold text-xs sm:text-sm text-[#103C35] flex items-center gap-1">
                    <Star className="w-3.5 h-3.5 fill-[#0B9F6E] text-[#0B9F6E]" />
                    <span>{current.rating} / 5.0</span>
                  </div>
                  <div className="text-[10px] text-[#6B817C] font-medium">{current.reviewsCount} Reviews</div>
                </div>
              </div>

              {/* Carousel Indicators below hero image (● ○ ○ ○ ○) */}
              <div className="mt-8 sm:mt-10 flex items-center justify-center gap-1.5 flex-wrap px-4">
                {HERO_SERVICES.map((s, idx) => {
                  const isActive = idx === currentIndex;
                  return (
                    <button
                      key={s.id}
                      onClick={() => handleIndicatorClick(idx)}
                      title={`${s.serviceName} - ${s.professional}`}
                      className={`transition-all duration-300 rounded-full cursor-pointer ${
                        isActive
                          ? 'w-7 h-2 bg-[#0B9F6E] shadow-xs'
                          : 'w-2 h-2 bg-[#DDF7EC] hover:bg-[#0B9F6E]/60'
                      }`}
                      aria-label={`Slide to ${s.serviceName}`}
                    />
                  );
                })}
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};
