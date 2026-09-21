import React, { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import { useLocation } from '../../context/LocationContext';
import { useCart } from '../../context/CartContext';
import { useChat } from '../../context/ChatContext';
import { BrandLogo } from './BrandLogo';
import {
  MapPin,
  Search,
  ShoppingBag,
  Bell,
  MessageSquare,
  Menu,
  X,
  ChevronDown,
  Sparkles,
  CalendarCheck,
  Tag,
  HelpCircle,
  Briefcase,
  Crown,
  LogOut,
  ArrowRight,
} from 'lucide-react';

interface NavbarProps {
  currentPath: string;
  onNavigate: (path: string) => void;
  onOpenNotifications: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({ currentPath, onNavigate, onOpenNotifications }) => {
  const { user, isAuthenticated, role, logout, openAuthModal } = useAuth();
  const { selectedCity, openLocationModal } = useLocation();
  const { items, openCartDrawer } = useCart();
  const { unreadTotal } = useChat();
  const chatEnabled = isAuthenticated && role !== 'ADMIN';

  const [isScrolled, setIsScrolled] = useState(false);
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [isProfileDropdownOpen, setIsProfileDropdownOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');

  useEffect(() => {
    const handleScroll = () => {
      setIsScrolled(window.scrollY > 20);
    };
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      onNavigate(`/services?search=${encodeURIComponent(searchQuery.trim())}`);
      setIsMobileMenuOpen(false);
    }
  };

  const totalCartCount = items.reduce((sum, item) => sum + item.quantity, 0);

  const navLinks = [
    { label: 'Home', path: '/' },
    { label: 'Services', path: '/services' },
    { label: 'Offers', path: '/offers' },
    { label: 'How It Works', path: '/how-it-works' },
    {
      label: 'For Professionals',
      path: '/professional/dashboard',
      action: () => {

        onNavigate('/professional/dashboard');
      },
    },
    { label: 'Help', path: '/support' },
  ];

  return (
    <>
      <header
        className={`sticky top-0 z-40 w-full transition-all duration-300 ${
          isScrolled
            ? 'bg-white/95 backdrop-blur-md shadow-xs py-2.5 border-b border-[var(--color-brand-light)]'
            : 'bg-white py-3.5 border-b border-[var(--color-brand-light)]/70'
        }`}
      >
        <div className="max-w-7xl mx-auto px-2 min-[380px]:px-3 sm:px-6 lg:px-8 flex items-center justify-between gap-1 sm:gap-4">
          {/* Brand Logo & Location */}
          <div className="flex items-center gap-2 sm:gap-6">
            <button
              onClick={() => onNavigate('/')}
              className="text-left cursor-pointer transition-transform hover:opacity-95"
            >
              <BrandLogo size="md" showTagline={false} />
            </button>

            {/* Location Selector */}
            <button
              onClick={openLocationModal}
              className="hidden md:flex items-center gap-2 px-3 py-1.5 rounded-xl border border-[var(--color-brand-light)] bg-[var(--color-brand-soft)]/70 hover:bg-[var(--color-brand-light)]/60 hover:border-[var(--color-brand)]/40 text-xs font-semibold text-[var(--color-ink)] transition-all cursor-pointer shadow-2xs"
            >
              <MapPin className="w-3.5 h-3.5 text-[var(--color-brand)]" />
              <span className="font-semibold text-[var(--color-brand-dark)]">{selectedCity.name}</span>
              <ChevronDown className="w-3 h-3 text-[var(--color-muted)]" />
            </button>
          </div>

          {/* Desktop Search Bar */}
          <form
            onSubmit={handleSearchSubmit}
            className="hidden lg:flex flex-1 max-w-xs relative items-center"
          >
            <Search className="absolute left-3.5 w-4 h-4 text-[var(--color-muted)]" />
            <input
              type="text"
              placeholder="Search AC, cleaning, salon, plumbing..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2 bg-[var(--color-brand-soft)]/80 hover:bg-white focus:bg-white border border-[var(--color-brand-light)] rounded-full text-xs text-[var(--color-ink)] placeholder-[var(--color-muted)]/70 transition-all focus:outline-none focus:ring-2 focus:ring-[var(--color-brand)]/30 focus:border-[var(--color-brand)]"
            />
          </form>

          {/* Desktop Navigation Links */}
          <nav className="hidden xl:flex items-center gap-6 text-xs font-semibold text-[var(--color-muted)]">
            {navLinks.map((link) => {
              const isActive = currentPath === link.path;
              return (
                <button
                  key={link.label}
                  onClick={() => (link.action ? link.action() : onNavigate(link.path))}
                  className={`hover:text-[var(--color-brand)] transition-colors cursor-pointer relative py-1 ${
                    isActive ? 'text-[var(--color-brand-hover)] font-bold' : ''
                  }`}
                >
                  {link.label}
                  {isActive && (
                    <span className="absolute bottom-0 left-0 right-0 h-0.5 bg-[var(--color-brand)] rounded-full" />
                  )}
                </button>
              );
            })}
          </nav>

          {/* Right Action Icons & Auth */}
          <div className="flex items-center gap-1 sm:gap-3">
            {/* Notification Bell */}
            <button
              onClick={onOpenNotifications}
              className="relative p-1.5 sm:p-2 text-[var(--color-brand-dark)] hover:text-[var(--color-brand)] hover:bg-[var(--color-brand-light)] rounded-xl transition-all cursor-pointer"
              aria-label="Notifications"
            >
              <Bell className="w-5 h-5" />
              <span className="absolute top-1.5 right-1.5 w-2 h-2 bg-[var(--color-brand)] rounded-full ring-2 ring-white" />
            </button>

            {/* Private chat */}
            {chatEnabled && (
              <button
                onClick={() => onNavigate('/messages')}
                className="relative p-1.5 sm:p-2 text-[var(--color-brand-dark)] hover:text-[var(--color-brand)] hover:bg-[var(--color-brand-light)] rounded-xl transition-all cursor-pointer"
                aria-label={unreadTotal > 0 ? `Messages, ${unreadTotal} unread` : 'Messages'}
                data-testid="nav-messages"
              >
                <MessageSquare className="w-5 h-5" />
                {unreadTotal > 0 && (
                  <span className="absolute -top-1 -right-1 min-w-[18px] h-[18px] px-1 bg-[var(--color-brand)] text-white text-[10px] font-bold rounded-full flex items-center justify-center shadow-xs">
                    {unreadTotal > 99 ? '99+' : unreadTotal}
                  </span>
                )}
              </button>
            )}

            {/* Cart Button */}
            <button
              onClick={openCartDrawer}
              className="relative p-1.5 sm:p-2 text-[var(--color-brand-dark)] hover:text-[var(--color-brand)] hover:bg-[var(--color-brand-light)] rounded-xl transition-all cursor-pointer"
              aria-label="Shopping Cart"
            >
              <ShoppingBag className="w-5 h-5" />
              {totalCartCount > 0 && (
                <span className="absolute -top-1 -right-1 min-w-[18px] h-[18px] px-1 bg-[var(--color-brand)] text-white text-[10px] font-bold rounded-full flex items-center justify-center shadow-xs">
                  {totalCartCount}
                </span>
              )}
            </button>

            {/* User Profile / Role Dropdown */}
            {isAuthenticated ? (
              <div className="relative">
                <button
                  onClick={() => setIsProfileDropdownOpen(!isProfileDropdownOpen)}
                  className="flex items-center gap-1.5 sm:gap-2 p-1 sm:p-1.5 sm:pl-2.5 rounded-xl border border-[var(--color-brand-light)] hover:border-[var(--color-brand)] hover:bg-[var(--color-brand-soft)] transition-all text-xs font-semibold text-[var(--color-ink)] cursor-pointer"
                >
                  <span className="max-w-[90px] truncate hidden lg:inline">{user?.name}</span>
                  <span
                    className={`hidden sm:inline text-[10px] uppercase font-bold px-1.5 py-0.5 rounded ${
                      role === 'ADMIN'
                        ? 'bg-amber-100 text-amber-800'
                        : role === 'PROFESSIONAL'
                        ? 'bg-brand-light text-brand-dark'
                        : 'bg-[var(--color-brand-light)] text-[var(--color-brand-hover)]'
                    }`}
                  >
                    {role === 'PROFESSIONAL' ? 'PRO' : role}
                  </span>
                  <img
                    src={user?.avatar || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=100&q=80'}
                    alt={user?.name}
                    className="w-7 h-7 rounded-lg object-cover border border-[var(--color-brand-light)]"
                    referrerPolicy="no-referrer"
                  />
                </button>

                {/* Dropdown Menu */}
                {isProfileDropdownOpen && (
                  <div className="absolute right-0 mt-2 w-64 bg-white rounded-2xl shadow-xl border border-[var(--color-brand-light)] p-2 z-50 animate-in fade-in duration-150">
                    <div className="px-3 py-2.5 border-b border-[var(--color-brand-light)]/60">
                      <p className="text-xs font-bold text-[var(--color-brand-dark)] truncate">{user?.name}</p>
                      <p className="text-[11px] text-[var(--color-muted)] truncate">{user?.email}</p>
                    </div>

                    {/* Navigation Items */}
                    <div className="py-1 space-y-0.5 text-xs text-[var(--color-ink)]">
                      {role === 'CUSTOMER' && (
                        <>
                          <button
                            onClick={() => {
                              onNavigate('/dashboard');
                              setIsProfileDropdownOpen(false);
                            }}
                            className="w-full flex items-center gap-2 px-3 py-2 rounded-xl hover:bg-[var(--color-brand-soft)] text-left font-medium cursor-pointer"
                          >
                            <CalendarCheck className="w-4 h-4 text-[var(--color-brand)]" />
                            <span>My Bookings</span>
                          </button>
                          <button
                            onClick={() => {
                              onNavigate('/dashboard/addresses');
                              setIsProfileDropdownOpen(false);
                            }}
                            className="w-full flex items-center gap-2 px-3 py-2 rounded-xl hover:bg-[var(--color-brand-soft)] text-left font-medium cursor-pointer"
                          >
                            <MapPin className="w-4 h-4 text-[var(--color-brand)]" />
                            <span>Saved Addresses</span>
                          </button>
                        </>
                      )}

                      {role === 'PROFESSIONAL' && (
                        <button
                          onClick={() => {
                            onNavigate('/professional/dashboard');
                            setIsProfileDropdownOpen(false);
                          }}
                          className="w-full flex items-center gap-2 px-3 py-2 rounded-xl hover:bg-[var(--color-brand-soft)] text-left font-medium text-[var(--color-brand-dark)] cursor-pointer"
                        >
                          <Briefcase className="w-4 h-4 text-[var(--color-brand)]" />
                          <span>Pro Jobs & Earnings</span>
                        </button>
                      )}

                      {role === 'ADMIN' && (
                        <button
                          onClick={() => {
                            onNavigate('/admin');
                            setIsProfileDropdownOpen(false);
                          }}
                          className="w-full flex items-center gap-2 px-3 py-2 rounded-xl hover:bg-[var(--color-brand-soft)] text-left font-medium text-[var(--color-brand-dark)] cursor-pointer"
                        >
                          <Crown className="w-4 h-4 text-[var(--color-brand)]" />
                          <span>Admin Control Center</span>
                        </button>
                      )}

                      <button
                        onClick={() => {
                          logout();
                          setIsProfileDropdownOpen(false);
                        }}
                        className="w-full flex items-center gap-2 px-3 py-2 rounded-xl hover:bg-red-50 text-left font-medium text-red-600 cursor-pointer"
                      >
                        <LogOut className="w-4 h-4 text-red-400" />
                        <span>Sign Out</span>
                      </button>
                    </div>
                  </div>
                )}
              </div>
            ) : (
              <button
                onClick={openAuthModal}
                className="hidden sm:inline-flex px-4 py-2 border border-[var(--color-brand-light)] hover:border-[var(--color-brand)] text-[var(--color-brand-dark)] hover:text-[var(--color-brand-hover)] text-xs font-bold rounded-xl transition-all cursor-pointer"
              >
                Sign In
              </button>
            )}

            {/* Primary Book CTA */}
            <button
              onClick={() => onNavigate('/services')}
              className="hidden lg:inline-flex items-center justify-center px-4.5 py-2.5 bg-[var(--color-brand)] hover:bg-[var(--color-brand-hover)] text-white text-xs font-bold rounded-xl transition-all shadow-md shadow-[var(--color-brand)]/20 active:scale-95 cursor-pointer"
            >
              Book a Service
            </button>

            {/* Mobile Menu Toggle */}
            <button
              onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
              className="xl:hidden p-1.5 sm:p-2 text-[var(--color-brand-dark)] hover:text-[var(--color-brand)] rounded-lg cursor-pointer"
              aria-label="Toggle mobile menu"
            >
              {isMobileMenuOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
            </button>
          </div>
        </div>

        {/* Mobile Dropdown Menu */}
        {isMobileMenuOpen && (
          <div className="xl:hidden bg-white border-b border-[var(--color-brand-light)] px-4 pt-3 pb-6 space-y-3 animate-in slide-in-from-top-2 duration-150">
            {/* Mobile Location Picker */}
            <button
              onClick={() => {
                openLocationModal();
                setIsMobileMenuOpen(false);
              }}
              className="w-full flex items-center justify-between p-2.5 rounded-xl bg-[var(--color-brand-soft)] border border-[var(--color-brand-light)] text-xs font-semibold text-[var(--color-ink)] cursor-pointer"
            >
              <div className="flex items-center gap-2">
                <MapPin className="w-4 h-4 text-[var(--color-brand)]" />
                <span>City: {selectedCity.name}</span>
              </div>
              <ChevronDown className="w-4 h-4 text-[var(--color-muted)]" />
            </button>

            {/* Mobile Search */}
            <form onSubmit={handleSearchSubmit} className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-[var(--color-muted)]" />
              <input
                type="text"
                placeholder="Search any service..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full pl-9 pr-4 py-2.5 bg-[var(--color-brand-soft)] border border-[var(--color-brand-light)] rounded-xl text-xs text-[var(--color-ink)] focus:outline-none focus:ring-2 focus:ring-[var(--color-brand)]"
              />
            </form>

            {/* Mobile Navigation Links */}
            {!isAuthenticated && (
              <button
                onClick={() => {
                  openAuthModal();
                  setIsMobileMenuOpen(false);
                }}
                className="w-full rounded-xl border border-[var(--color-brand)] bg-[var(--color-brand-light)] px-3 py-2.5 text-left text-xs font-bold text-[var(--color-brand-dark)] sm:hidden"
              >
                Sign In or Register
              </button>
            )}
            <div className="grid grid-cols-2 gap-2 pt-1">
              {navLinks.map((link) => (
                <button
                  key={link.label}
                  onClick={() => {
                    if (link.action) {
                      link.action();
                    } else {
                      onNavigate(link.path);
                    }
                    setIsMobileMenuOpen(false);
                  }}
                  className={`p-2.5 rounded-xl text-left text-xs font-semibold cursor-pointer transition-colors ${
                    currentPath === link.path
                      ? 'bg-[var(--color-brand-light)] text-[var(--color-brand-hover)] font-bold border border-[var(--color-brand)]/40'
                      : 'bg-[var(--color-brand-soft)] text-[var(--color-ink)] hover:bg-[var(--color-brand-light)]'
                  }`}
                >
                  {link.label}
                </button>
              ))}
            </div>

            {/* Mobile CTA */}
            <div className="pt-2">
              <button
                onClick={() => {
                  onNavigate('/services');
                  setIsMobileMenuOpen(false);
                }}
                className="w-full py-3 bg-[var(--color-brand)] hover:bg-[var(--color-brand-hover)] text-white font-bold text-xs rounded-xl shadow-md shadow-[var(--color-brand)]/20 flex items-center justify-center gap-1.5 cursor-pointer"
              >
                <span>Book a Service Now</span>
                <ArrowRight className="w-4 h-4" />
              </button>
            </div>
          </div>
        )}
      </header>
    </>
  );
};
