import { apiFetch } from './services/api';
import React, { useState, useEffect } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
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
import { api, getAll } from './services/api';

export function AppContent() {
  const [currentPath, setCurrentPath] = useState<string>(() => {
    return window.location.pathname + window.location.search || '/';
  });

  const [categories, setCategories] = useState<Category[]>([]);
  const [services, setServices] = useState<Service[]>([]);
  const [professionals, setProfessionals] = useState<Professional[]>([]);
  const [reviews, setReviews] = useState<Review[]>([]);
  const [isNotificationsOpen, setIsNotificationsOpen] = useState(false);

  const { user, loading: authLoading, openAuthModal } = useAuth();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  // Load the persisted catalog
  useEffect(() => {
    Promise.all([api('/categories'), getAll('/services', 'services'), getAll('/professionals', 'professionals'), api('/reviews')])
      .then(([c, s, p, r]) => { setCategories(c.categories); setServices(s); setProfessionals(p); setReviews(r.reviews); })
      .catch(e => setError(e.message)).finally(() => setLoading(false));
  }, []);

  // Listen to browser popstate (back/forward)
  useEffect(() => {
    const handlePopState = () => {
      setCurrentPath(window.location.pathname + window.location.search);
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
    if (loading || authLoading) return <p role="status" className="p-10 text-center">Loading Service Assist...</p>;
    if (error) return <div role="alert" className="p-10 text-center">{error}<button className="ml-4 underline" onClick={() => window.location.reload()}>Retry</button></div>;
    const requiredRole = currentPath.startsWith('/admin') ? 'ADMIN' : currentPath.startsWith('/professional') ? 'PROFESSIONAL' : currentPath.startsWith('/dashboard') ? 'CUSTOMER' : null;
    if (requiredRole && !user) return <div className="p-10 text-center"><p>Please sign in to continue.</p><button onClick={openAuthModal} className="mt-4 text-orange-600">Sign in or register</button></div>;
    if (requiredRole && user?.role !== requiredRole) return <p role="alert" className="p-10 text-center">This page requires a {requiredRole.toLowerCase()} account.</p>;

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
      return <p className="p-10 text-center">Service not found.</p>;
    }

    // All Services or Search
    if (currentPath === '/services' || currentPath.startsWith('/services?') || currentPath.startsWith('/search')) {
      const urlParams = new URLSearchParams(window.location.search);
      const categoryParam = urlParams.get('category') || undefined;
      const searchParam = urlParams.get('search') || urlParams.get('q') || undefined;

      return (
        <ServicesPage key={currentPath}
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
