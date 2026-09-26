import { apiFetch } from './services/api';
import React, { useState, useEffect } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import { LocationProvider } from './context/LocationContext';
import { ChatProvider } from './context/ChatContext';
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
import { MessagesPage } from './pages/MessagesPage';

// Types and default seed data
import { Category, Service, Professional, Review, Booking } from './types';
import { api, getAll } from './services/api';
import {
  DEFAULT_CATEGORIES,
  DEFAULT_SERVICES,
  DEFAULT_PROFESSIONALS,
  DEFAULT_REVIEWS,
} from './data/defaultCatalog';

export function AppContent() {
  const [currentPath, setCurrentPath] = useState<string>(() => {
    return window.location.pathname + window.location.search || '/';
  });

  const [categories, setCategories] = useState<Category[]>(DEFAULT_CATEGORIES);
  const [services, setServices] = useState<Service[]>(DEFAULT_SERVICES);
  const [professionals, setProfessionals] = useState<Professional[]>(DEFAULT_PROFESSIONALS);
  const [reviews, setReviews] = useState<Review[]>(DEFAULT_REVIEWS);
  const [isNotificationsOpen, setIsNotificationsOpen] = useState(false);

  const { user, loading: authLoading, openAuthModal } = useAuth();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // Load the persisted catalog from API (syncs dynamically when backend is online)
  useEffect(() => {
    Promise.all([
      api('/categories').catch(() => null),
      getAll('/services', 'services').catch(() => null),
      getAll('/professionals', 'professionals').catch(() => null),
      api('/reviews').catch(() => null),
    ])
      .then(([c, s, p, r]) => {
        if (c?.categories && c.categories.length > 0) setCategories(c.categories);
        if (s && s.length > 0) setServices(s);
        if (p && p.length > 0) setProfessionals(p);
        if (r?.reviews && r.reviews.length > 0) setReviews(r.reviews);
      })
      .catch((e) => {
        console.warn('API sync notice:', e?.message || e);
      });
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

  const handleBookingSuccess = (_booking: Booking) => {
    navigate('/dashboard');
  };

  const isChatRoute = currentPath === '/messages' || currentPath.startsWith('/messages?');

  // Route parsing
  const renderRoute = () => {
    if (loading) return <div className="p-16 text-center text-sm font-semibold text-[var(--color-brand-hover)]">Loading Service Assist...</div>;
    if (error) return <div role="alert" className="p-10 text-center">{error}<button className="ml-4 underline" onClick={() => window.location.reload()}>Retry</button></div>;

    const requiredRole = currentPath.startsWith('/admin') ? 'ADMIN' : currentPath.startsWith('/professional') ? 'PROFESSIONAL' : currentPath.startsWith('/dashboard') ? 'CUSTOMER' : null;

    if (requiredRole && authLoading) {
      return <div className="p-16 text-center text-sm font-semibold text-[var(--color-brand-hover)]">Authenticating account...</div>;
    }

    if (requiredRole && !user) return <div className="p-12 text-center max-w-md mx-auto my-8 bg-white rounded-3xl border border-gray-100 shadow-xs"><p className="font-bold text-gray-800 mb-2">Please sign in to continue</p><p className="text-xs text-gray-500 mb-4">You need an active session to access your dashboard.</p><button onClick={openAuthModal} className="px-5 py-2.5 bg-[var(--color-brand)] text-white rounded-xl text-xs font-bold shadow-xs">Sign In or Register</button></div>;
    if (requiredRole && user?.role !== requiredRole) return <p role="alert" className="p-10 text-center">This page requires a {requiredRole.toLowerCase()} account.</p>;

    // Private chat (customers and professionals only)
    if (currentPath === '/messages' || currentPath.startsWith('/messages?')) {
      if (authLoading) return <div className="p-16 text-center text-sm font-semibold text-[var(--color-brand-hover)]">Loading messages...</div>;
      if (!user) return <div className="p-12 text-center max-w-md mx-auto my-8 bg-white rounded-3xl border border-gray-100 shadow-xs"><p className="font-bold text-gray-800 mb-2">Please sign in to view your messages</p><button onClick={openAuthModal} className="px-5 py-2.5 bg-[var(--color-brand)] text-white rounded-xl text-xs font-bold shadow-xs">Sign In or Register</button></div>;
      if (user.role === 'ADMIN') return <p role="alert" className="p-10 text-center">Chat is available to customers and professionals.</p>;
      return <MessagesPage currentPath={currentPath} onNavigate={navigate} />;
    }

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
      const queryString = currentPath.includes('?') ? currentPath.substring(currentPath.indexOf('?')) : window.location.search;
      const urlParams = new URLSearchParams(queryString);
      const categoryParam = urlParams.get('category') || undefined;
      const searchParam = urlParams.get('search') || urlParams.get('q') || undefined;

      return (
        <ServicesPage
          key={currentPath}
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
      return <ProfessionalDashboardPage onNavigate={navigate} />;
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
    <div className="min-h-screen flex flex-col bg-white text-gray-900 font-sans selection:bg-[var(--color-brand)] selection:text-white">
      {/* Global Header */}
      <Navbar
        currentPath={currentPath}
        onNavigate={navigate}
        onOpenNotifications={() => setIsNotificationsOpen(true)}
      />

      {/* Main Page Body */}
      <main className={`flex-1 flex flex-col w-full ${isChatRoute ? '' : 'pb-16 md:pb-0'}`}>
        {renderRoute()}
      </main>

      {/* Global Footer (the chat screen is a full-height app view, so it has none) */}
      {!isChatRoute && <Footer onNavigate={navigate} />}

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

      {/* AI Assistant Floating Widget (hidden on the chat screen, where it would cover the composer) */}
      {!isChatRoute && <HomeAIAssistant onNavigate={navigate} />}
    </div>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <ChatProvider>
        <LocationProvider>
          <CartProvider>
            <AppContent />
          </CartProvider>
        </LocationProvider>
      </ChatProvider>
    </AuthProvider>
  );
}
