import React, { useState } from 'react';
import { Hero } from '../components/landing/Hero';
import { CategoriesGrid } from '../components/landing/CategoriesGrid';
import { AiDiagnosticWidget } from '../components/landing/AiDiagnosticWidget';
import { PopularServices } from '../components/landing/PopularServices';
import { HowItWorks } from '../components/landing/HowItWorks';
import { TrustGuarantees } from '../components/landing/TrustGuarantees';
import { Testimonials } from '../components/landing/Testimonials';
import { PreFooterCta } from '../components/landing/PreFooterCta';
import { ProPartnerCta } from '../components/landing/ProPartnerCta';
import { BookingModal } from '../components/landing/BookingModal';
import { AuthModal } from '../components/landing/AuthModal';
import { Category, Service, Professional, Review } from '../types';
import { ServiceItem, CartItem, UserProfile } from '../types/landing';
import { SERVICES, CITIES } from '../data/landing/servicesData';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../context/CartContext';
import { useLocation } from '../context/LocationContext';
import { CheckCircle2 } from 'lucide-react';

interface HomePageProps {
  categories: Category[];
  services: Service[];
  professionals: Professional[];
  reviews: Review[];
  onNavigate: (path: string) => void;
  onSelectCategory: (categoryId: string) => void;
  onSelectService: (slug: string) => void;
}

export const HomePage: React.FC<HomePageProps> = ({
  onNavigate,
  onSelectCategory,
  onSelectService,
}) => {
  const { user, openAuthModal } = useAuth();
  const { selectedCity } = useLocation();
  const { addToCart } = useCart();

  const [selectedCategory, setSelectedCategory] = useState('all');
  const [partnerModalOpen, setPartnerModalOpen] = useState(false);
  const [bookingModalOpen, setBookingModalOpen] = useState(false);
  const [activeBookingService, setActiveBookingService] = useState<ServiceItem | null>(null);
  const [authModalOpen, setAuthModalOpen] = useState(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  const currentUser: UserProfile | null = user
    ? {
        name: user.name,
        phone: user.phone,
        isLoggedIn: true,
      }
    : null;

  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => {
      setToastMessage(null);
    }, 3000);
  };

  const handleInstantBook = (service?: ServiceItem) => {
    const targetService = service || SERVICES[0];
    if (!user) {
      setActiveBookingService(targetService);
      openAuthModal();
      return;
    }
    setActiveBookingService(targetService);
    setBookingModalOpen(true);
  };

  const handleAddToCart = (serviceItem: ServiceItem) => {
    // Map landing service to full catalog format or trigger cart notification
    showToast(`Added "${serviceItem.title}" to cart`);
  };

  const handleCategoryClick = (catId: string) => {
    setSelectedCategory(catId);
    const categoryMapping: Record<string, string> = {
      'ac-appliance': 'cat-appliances',
      'deep-cleaning': 'cat-cleaning',
      'electrician-plumber': 'cat-electrician',
      'carpentry-handyman': 'cat-carpentry',
      'pest-control': 'cat-pest-control',
      'painting': 'cat-painting',
      'water-purifier': 'cat-appliances',
      'salon-spa': 'cat-salon-women',
    };

    if (categoryMapping[catId]) {
      // Smooth scroll to services or route
      const el = document.getElementById('services');
      if (el) {
        el.scrollIntoView({ behavior: 'smooth' });
      }
    }
  };

  return (
    <div className="min-h-screen bg-white text-slate-900 flex flex-col font-sans selection:bg-emerald-500 selection:text-white">
      {/* Toast Notification */}
      {toastMessage && (
        <div className="fixed bottom-6 right-6 z-50 bg-slate-900 text-white text-xs font-semibold px-4 py-3 rounded-2xl shadow-2xl border border-slate-800 flex items-center gap-2 animate-in fade-in slide-in-from-bottom-3 duration-200">
          <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
          <span>{toastMessage}</span>
        </div>
      )}

      {/* 1. Hero Section: 3D Animated atmosphere, video backdrop, technician cutout & floating badges */}
      <Hero
        currentCity={selectedCity?.name || 'Delhi NCR'}
        onSelectCategory={handleCategoryClick}
        onInstantBook={() => handleInstantBook(SERVICES[0])}
        onOpenAiDiagnostic={() => {
          const el = document.getElementById('ai-diagnostic');
          if (el) el.scrollIntoView({ behavior: 'smooth' });
        }}
      />

      {/* 2. Popular Category Cards Grid */}
      <CategoriesGrid
        selectedCategory={selectedCategory}
        onSelectCategory={handleCategoryClick}
      />

      {/* 3. AI Diagnostic Widget with 3D Robot & Problem Solver */}
      <AiDiagnosticWidget
        currentUser={currentUser}
        onBookService={(service) => handleInstantBook(service)}
      />

      {/* 4. Most Booked Home Services Showcase */}
      <PopularServices
        selectedCategory={selectedCategory}
        onCategoryChange={(catId) => setSelectedCategory(catId)}
        onAddToCart={handleAddToCart}
        onInstantBook={handleInstantBook}
        currentUser={currentUser}
      />

      {/* 5. How It Works: 3-Step Flow */}
      <HowItWorks
        onBookService={() => {
          const el = document.getElementById('services');
          if (el) el.scrollIntoView({ behavior: 'smooth' });
        }}
      />

      {/* 6. Why Thousands of Families Trust Us */}
      <TrustGuarantees />

      {/* 7. Verified Customer Testimonials */}
      <Testimonials />

      {/* 8. Pre-Footer Call to Action Banner */}
      <PreFooterCta
        currentUser={currentUser}
        onInstantBook={() => handleInstantBook(SERVICES[0])}
      />

      {/* Interactive Booking Modal */}
      <BookingModal
        isOpen={bookingModalOpen}
        onClose={() => {
          setBookingModalOpen(false);
          setActiveBookingService(null);
        }}
        cartItems={[]}
        currentCity={selectedCity?.name || 'Delhi NCR'}
        onClearCart={() => {}}
        initialService={activeBookingService}
        currentUser={currentUser}
        onRequireAuth={() => {
          setBookingModalOpen(false);
          openAuthModal();
        }}
      />

      {/* Pro Partner Registration Modal */}
      <ProPartnerCta
        isOpen={partnerModalOpen}
        onOpen={() => setPartnerModalOpen(true)}
        onClose={() => setPartnerModalOpen(false)}
        showBanner={false}
      />

      {/* Visitor Auth Modal */}
      <AuthModal
        isOpen={authModalOpen}
        onClose={() => setAuthModalOpen(false)}
        onSuccess={(phone, name) => {
          showToast(`Welcome ${name || 'Customer'}!`);
          setAuthModalOpen(false);
          if (activeBookingService) {
            setBookingModalOpen(true);
          }
        }}
        bookingService={activeBookingService}
        isCartCheckout={false}
        cartCount={0}
        cartTotal={0}
      />
    </div>
  );
};
