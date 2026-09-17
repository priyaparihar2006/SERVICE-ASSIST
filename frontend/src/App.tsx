import React, { useState, useEffect } from 'react';
import { AuthProvider } from './context/AuthContext';
import { LocationProvider } from './context/LocationContext';
import { CartProvider } from './context/CartContext';
import { Navbar } from './components/common/Navbar';
import { Footer } from './components/common/Footer';
import { LocationModal } from './components/common/LocationModal';
import { NotificationDrawer } from './components/common/NotificationDrawer';
import { CartDrawer } from './components/common/CartDrawer';
import { AuthModal } from './components/auth/AuthModal';
import { CheckoutModal } from './components/checkout/CheckoutModal';
import { HomeAIAssistant } from './components/ai/HomeAIAssistant';

// Pages
import { HomePage } from './pages/HomePage';
import { ServicesPage } from './pages/ServicesPage';
import { ServiceDetailPage } from './pages/ServiceDetailPage';
import { CustomerDashboardPage } from './pages/CustomerDashboardPage';
import { ProfessionalDashboardPage } from './pages/ProfessionalDashboardPage';
import { AdminDashboardPage } from './pages/AdminDashboardPage';
import { OffersPage } from './pages/OffersPage';
import { HowItWorksPage } from './pages/HowItWorksPage';
import { SupportPage } from './pages/SupportPage';

// Types and default seed data
import { Category, Service, Professional, Review, Booking } from './types';
import { CATEGORIES, SERVICES, PROFESSIONALS, REVIEWS } from '../server/seedData';

export function AppContent() {
  const [currentPath, setCurrentPath] = useState<string>(() => {
    return window.location.pathname || '/';
  });

  const [categories, setCategories] = useState<Category[]>(CATEGORIES);
  const [services, setServices] = useState<Service[]>(SERVICES);
  const [professionals, setProfessionals] = useState<Professional[]>(PROFESSIONALS);
  const [reviews, setReviews] = useState<Review[]>(REVIEWS);
  const [isNotificationsOpen, setIsNotificationsOpen] = useState(false);

  // Fetch initial data from backend with fallback
  useEffect(() => {
    const loadData = async () => {
      try {
        const [catRes, srvRes, proRes, revRes] = await Promise.all([
          fetch('/api/categories').catch(() => null),
          fetch('/api/services').catch(() => null),
          fetch('/api/professionals').catch(() => null),
          fetch('/api/reviews').catch(() => null),
        ]);

        if (catRes && catRes.ok) {
          const catData = await catRes.json();
          if (catData.categories?.length) setCategories(catData.categories);
        }
        if (srvRes && srvRes.ok) {
          const srvData = await srvRes.json();
          if (srvData.services?.length) setServices(srvData.services);
        }
        if (proRes && proRes.ok) {
          const proData = await proRes.json();
          if (proData.professionals?.length) setProfessionals(proData.professionals);
        }
        if (revRes && revRes.ok) {
          const revData = await revRes.json();
          if (revData.reviews?.length) setReviews(revData.reviews);
        }
      } catch (err) {
        console.error('Error loading data from server, using seed defaults:', err);
      }
    };
    loadData();
  }, []);

  // Listen to browser popstate (back/forward)
  useEffect(() => {
    const handlePopState = () => {
      setCurrentPath(window.location.pathname);
    };
    window.addEventListener('popstate', handlePopState);
    return () => window.removeEventListener('popstate', handlePopState);
  }, []);

  const navigate = (path: string) => {
    window.history.pushState({}, '', path);
    setCurrentPath(path);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleBookingSuccess = (booking: Booking) => {
    // Optionally redirect to customer dashboard
    navigate('/dashboard');
  };

  // Route parsing
  const renderRoute = () => {
    // Service Detail Route: /services/:slug
    if (currentPath.startsWith('/services/') && currentPath !== '/services') {
      const slug = currentPath.replace('/services/', '').split('?')[0];
      const found = services.find((s) => s.slug === slug || s.id === slug);
      if (found) {
        return (
          <ServiceDetailPage
            service={found}
            onBack={() => navigate('/services')}
            onNavigate={navigate}
          />
        );
      }
    }

    // All Services or Search
    if (currentPath === '/services' || currentPath.startsWith('/services?') || currentPath.startsWith('/search')) {
      const urlParams = new URLSearchParams(window.location.search);
      const categoryParam = urlParams.get('category') || undefined;
      const searchParam = urlParams.get('search') || urlParams.get('q') || undefined;

      return (
        <ServicesPage
          services={services}
          categories={categories}
          initialCategory={categoryParam}
          initialSearch={searchParam}
          onSelectService={(slug) => navigate(`/services/${slug}`)}
          onNavigate={navigate}
        />
      );
    }

    // Customer Dashboard
    if (currentPath.startsWith('/dashboard')) {
      return (
        <CustomerDashboardPage
          services={services}
          onSelectService={(slug) => navigate(`/services/${slug}`)}
          onNavigate={navigate}
        />
      );
    }

    // Professional Partner Dashboard
    if (currentPath.startsWith('/professional')) {
      return <ProfessionalDashboardPage />;
    }

    // Admin Dashboard
    if (currentPath.startsWith('/admin')) {
      return <AdminDashboardPage services={services} professionals={professionals} />;
    }

    // Offers & Coupons
    if (currentPath === '/offers') {
      return <OffersPage onNavigate={navigate} />;
    }

    // How it works
    if (currentPath === '/how-it-works') {
      return <HowItWorksPage onNavigate={navigate} />;
    }

    // Support
    if (currentPath === '/support') {
      return <SupportPage />;
    }

    // Default: Home Page
    return (
      <HomePage
        categories={categories}
        services={services}
        professionals={professionals}
        reviews={reviews}
        onNavigate={navigate}
        onSelectCategory={(categoryId) => navigate(`/services?category=${categoryId}`)}
        onSelectService={(slug) => navigate(`/services/${slug}`)}
      />
    );
  };

  return (
    <div className="min-h-screen flex flex-col bg-white text-gray-900 font-sans selection:bg-[#FF7A00] selection:text-white">
      {/* Global Header */}
      <Navbar
        currentPath={currentPath}
        onNavigate={navigate}
        onOpenNotifications={() => setIsNotificationsOpen(true)}
      />

      {/* Main Page Body */}
      <main className="flex-1 pb-16 md:pb-0">{renderRoute()}</main>

      {/* Global Footer */}
      <Footer onNavigate={navigate} />

      {/* Overlays & Drawers */}
      <LocationModal />
      <NotificationDrawer
        isOpen={isNotificationsOpen}
        onClose={() => setIsNotificationsOpen(false)}
        onNavigate={navigate}
      />
      <CartDrawer onNavigate={navigate} />
      <CheckoutModal onSuccess={handleBookingSuccess} />
      <AuthModal />

      {/* AI Assistant Floating Widget */}
      <HomeAIAssistant onNavigate={navigate} />
    </div>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <LocationProvider>
        <CartProvider>
          <AppContent />
        </CartProvider>
      </LocationProvider>
    </AuthProvider>
  );
}
