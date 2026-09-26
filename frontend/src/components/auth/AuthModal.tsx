import React, { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { useLocation } from '../../context/LocationContext';
import { UserRole } from '../../types';
import { X, Phone, Lock, User, Wrench, Shield, CheckCircle2, ArrowRight } from 'lucide-react';

export const AuthModal: React.FC = () => {
  const { isAuthModalOpen, closeAuthModal, login, register } = useAuth();
  const { selectedCity } = useLocation();

  const [accountType, setAccountType] = useState<'CUSTOMER' | 'PROFESSIONAL' | 'ADMIN'>('CUSTOMER');
  const [isRegisterMode, setIsRegisterMode] = useState(false);
  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  if (!isAuthModalOpen) return null;

  const handleQuickLogin = async (role: 'CUSTOMER' | 'PROFESSIONAL' | 'ADMIN') => {
    setError('');
    setLoading(true);
    try {
      if (role === 'CUSTOMER') {
        await login('priya@service-assist.test', 'Priya@123456');
      } else if (role === 'PROFESSIONAL') {
        await login('rajesh@service-assist.test', 'Rajesh@123456');
      } else {
        await login('admin@service-assist.test', 'Admin@123456');
      }
    } catch (e: any) {
      setError(e.message || 'Quick login failed.');
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (isRegisterMode) {
      const cleanName = name.trim();
      if (!cleanName) {
        setError('Please enter your full name.');
        return;
      }
      if (!/^[a-zA-Z\s]+$/.test(cleanName)) {
        setError('Full name must contain only letters and spaces.');
        return;
      }

      const cleanEmail = email.trim().toLowerCase();
      if (!cleanEmail || !/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(cleanEmail)) {
        setError('Please enter a valid email address.');
        return;
      }

      const cleanPhone = phone.trim();
      if (cleanPhone && !/^[6-9]\d{9}$/.test(cleanPhone)) {
        setError('Please enter a valid 10-digit Indian mobile number.');
        return;
      }

      if (password.length < 8) {
        setError('Password must be at least 8 characters long.');
        return;
      }

      setLoading(true);
      try {
        await register(cleanName, cleanEmail, cleanPhone, password, accountType as UserRole);
      } catch (err: any) {
        setError(err.message || 'Registration failed.');
      } finally {
        setLoading(false);
      }
    } else {
      const cleanIdentifier = identifier.trim();
      if (!cleanIdentifier) {
        setError('Please enter your mobile number or email.');
        return;
      }

      // If user typed only mobile or demo name, resolve fallback demo credentials
      let loginEmail = cleanIdentifier;
      let loginPassword = password || 'Demo@123456';

      if (/^\d{10}$/.test(cleanIdentifier)) {
        if (cleanIdentifier === '9876543210' || accountType === 'CUSTOMER') {
          loginEmail = 'priya@service-assist.test';
          loginPassword = password || 'Priya@123456';
        } else if (accountType === 'PROFESSIONAL') {
          loginEmail = 'rajesh@service-assist.test';
          loginPassword = password || 'Rajesh@123456';
        } else {
          loginEmail = 'admin@service-assist.test';
          loginPassword = password || 'Admin@123456';
        }
      }

      setLoading(true);
      try {
        await login(loginEmail, loginPassword);
      } catch (err: any) {
        // Fallback demo try if password wasn't provided
        if (!password) {
          try {
            if (accountType === 'CUSTOMER') await login('priya@service-assist.test', 'Priya@123456');
            else if (accountType === 'PROFESSIONAL') await login('rajesh@service-assist.test', 'Rajesh@123456');
            else await login('admin@service-assist.test', 'Admin@123456');
            return;
          } catch {}
        }
        setError(err.message || 'Authentication failed. Please check credentials.');
      } finally {
        setLoading(false);
      }
    }
  };

  const portalDescriptions = {
    CUSTOMER: 'Book AC repair, deep cleaning & spa at home',
    PROFESSIONAL: 'Manage doorstep bookings, track jobs & earn',
    ADMIN: 'Administer service catalog, bookings & analytics',
  };

  const portalRoleNames = {
    CUSTOMER: 'Customer',
    PROFESSIONAL: 'Partner Pro',
    ADMIN: 'Admin',
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-black/60 backdrop-blur-xs overflow-y-auto animate-in fade-in duration-200"
    >
      <div className="relative w-full max-w-[420px] my-auto bg-white rounded-3xl shadow-2xl border border-gray-100 overflow-hidden flex flex-col max-h-[92vh]">
        {/* Close Button */}
        <button
          type="button"
          onClick={closeAuthModal}
          aria-label="Close"
          className="absolute right-4 top-4 z-10 p-1.5 text-gray-400 hover:text-gray-700 rounded-full hover:bg-gray-100 transition-colors cursor-pointer"
        >
          <X className="w-5 h-5" />
        </button>

        {/* Header with SA Logo */}
        <div className="pt-6 pb-2 px-6 text-center flex flex-col items-center">
          <div className="w-16 h-16 rounded-2xl bg-white p-1.5 shadow-md shadow-[var(--color-brand)]/20 border border-[var(--color-brand-light)] mb-3 flex items-center justify-center">
            <img src="/sa-logo.png" alt="Service Assist SA Logo" className="w-full h-full object-contain" />
          </div>

          <h2 className="text-xl font-black text-gray-900 tracking-tight font-['Outfit']">
            SERVICE ASSIST
          </h2>
          <p className="text-xs text-gray-500 flex items-center gap-1 mt-0.5 font-medium">
            <span className="text-[var(--color-brand)] font-bold">📍 Doorstep Services</span> • {selectedCity.name}, {selectedCity.state}
          </p>
        </div>

        <div className="px-6 pb-6 pt-2 overflow-y-auto space-y-4">
          {/* Account Type Selector */}
          <div>
            <label className="block text-[10px] font-extrabold uppercase tracking-wider text-gray-400 mb-2">
              Select Your Account Type
            </label>
            <div className="grid grid-cols-3 gap-2">
              <button
                type="button"
                onClick={() => setAccountType('CUSTOMER')}
                className={`py-2.5 px-2 rounded-2xl text-xs font-bold flex flex-col items-center gap-1 transition-all cursor-pointer border ${
                  accountType === 'CUSTOMER'
                    ? 'bg-[var(--color-brand)] text-white border-[var(--color-brand)] shadow-sm'
                    : 'bg-white text-gray-700 border-gray-200 hover:bg-gray-50'
                }`}
              >
                <User className="w-4 h-4" />
                <span>Customer</span>
              </button>

              <button
                type="button"
                onClick={() => setAccountType('PROFESSIONAL')}
                className={`py-2.5 px-2 rounded-2xl text-xs font-bold flex flex-col items-center gap-1 transition-all cursor-pointer border ${
                  accountType === 'PROFESSIONAL'
                    ? 'bg-[var(--color-brand)] text-white border-[var(--color-brand)] shadow-sm'
                    : 'bg-white text-gray-700 border-gray-200 hover:bg-gray-50'
                }`}
              >
                <Wrench className="w-4 h-4" />
                <span>Partner Pro</span>
              </button>

              <button
                type="button"
                onClick={() => setAccountType('ADMIN')}
                className={`py-2.5 px-2 rounded-2xl text-xs font-bold flex flex-col items-center gap-1 transition-all cursor-pointer border ${
                  accountType === 'ADMIN'
                    ? 'bg-[var(--color-brand)] text-white border-[var(--color-brand)] shadow-sm'
                    : 'bg-white text-gray-700 border-gray-200 hover:bg-gray-50'
                }`}
              >
                <Shield className="w-4 h-4" />
                <span>Admin</span>
              </button>
            </div>
          </div>

          {/* Portal Info Pill */}
          <div className="p-3 bg-[var(--color-brand-soft)] rounded-2xl border border-[var(--color-brand-light)] flex items-start gap-2 text-xs">
            <div className="w-5 h-5 rounded-full bg-[var(--color-brand)] text-white flex items-center justify-center shrink-0 mt-0.5">
              <CheckCircle2 className="w-3.5 h-3.5" />
            </div>
            <div>
              <span className="font-bold text-gray-900 block leading-tight">
                {portalRoleNames[accountType]} Portal
              </span>
              <span className="text-gray-500 text-[11px] block mt-0.5">
                {portalDescriptions[accountType]}
              </span>
            </div>
          </div>

          {/* Form */}
          <form onSubmit={handleSubmit} className="space-y-3">
            <div className="flex items-center justify-between">
              <h3 className="text-sm font-black text-gray-900 font-['Outfit']">
                {isRegisterMode ? 'Create Your Account' : 'Sign In to Your Account'}
              </h3>
            </div>

            {error && (
              <div className="p-2.5 text-xs text-red-700 bg-red-50 border border-red-200 rounded-xl font-medium">
                {error}
              </div>
            )}

            {isRegisterMode ? (
              <>
                <div>
                  <div className="relative">
                    <User className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                    <input
                      type="text"
                      placeholder="Full Name"
                      value={name}
                      onChange={(e) => setName(e.target.value.replace(/[^a-zA-Z\s]/g, ''))}
                      className="w-full pl-10 pr-3.5 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-xs sm:text-sm text-gray-900 focus:bg-white focus:outline-none focus:ring-2 focus:ring-[var(--color-brand)]/30 focus:border-[var(--color-brand)]"
                      required
                    />
                  </div>
                </div>

                <div>
                  <div className="relative">
                    <Phone className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                    <input
                      type="tel"
                      maxLength={10}
                      placeholder="Mobile Phone (10 digits)"
                      value={phone}
                      onChange={(e) => setPhone(e.target.value.replace(/\D/g, '').slice(0, 10))}
                      className="w-full pl-10 pr-3.5 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-xs sm:text-sm text-gray-900 font-mono focus:bg-white focus:outline-none focus:ring-2 focus:ring-[var(--color-brand)]/30 focus:border-[var(--color-brand)]"
                    />
                  </div>
                </div>

                <div>
                  <div className="relative">
                    <User className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                    <input
                      type="email"
                      placeholder="Email Address"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      className="w-full pl-10 pr-3.5 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-xs sm:text-sm text-gray-900 focus:bg-white focus:outline-none focus:ring-2 focus:ring-[var(--color-brand)]/30 focus:border-[var(--color-brand)]"
                      required
                    />
                  </div>
                </div>

                <div>
                  <div className="relative">
                    <Lock className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                    <input
                      type="password"
                      placeholder="Create Password (min 8 chars)"
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      className="w-full pl-10 pr-3.5 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-xs sm:text-sm text-gray-900 focus:bg-white focus:outline-none focus:ring-2 focus:ring-[var(--color-brand)]/30 focus:border-[var(--color-brand)]"
                      required
                    />
                  </div>
                </div>
              </>
            ) : (
              <>
                <div>
                  <div className="relative">
                    <Phone className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-emerald-600" />
                    <input
                      type="text"
                      placeholder="Mobile Number or Email"
                      value={identifier}
                      onChange={(e) => setIdentifier(e.target.value)}
                      className="w-full pl-10 pr-3.5 py-3 bg-white border border-gray-200 rounded-2xl text-xs sm:text-sm text-gray-900 focus:outline-none focus:ring-2 focus:ring-[var(--color-brand)]/30 focus:border-[var(--color-brand)] shadow-2xs"
                      required
                    />
                  </div>
                </div>

                <div>
                  <div className="relative">
                    <Lock className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-emerald-600" />
                    <input
                      type="password"
                      placeholder="Password / OTP (or leave blank)"
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      className="w-full pl-10 pr-3.5 py-3 bg-white border border-gray-200 rounded-2xl text-xs sm:text-sm text-gray-900 focus:outline-none focus:ring-2 focus:ring-[var(--color-brand)]/30 focus:border-[var(--color-brand)] shadow-2xs"
                    />
                  </div>
                </div>
              </>
            )}

            <button
              type="submit"
              disabled={loading}
              className="w-full py-3.5 px-4 bg-[var(--color-brand)] hover:bg-[var(--color-brand-hover)] text-white font-bold text-xs sm:text-sm rounded-2xl transition-all shadow-md shadow-brand/20 active:scale-[0.99] flex items-center justify-center gap-2 cursor-pointer disabled:opacity-60"
            >
              <span>
                {loading
                  ? 'Authenticating...'
                  : isRegisterMode
                  ? `Register as ${portalRoleNames[accountType]}`
                  : `Log In as ${portalRoleNames[accountType]}`}
              </span>
              <ArrowRight className="w-4 h-4" />
            </button>
          </form>

          {/* Toggle Register / Login */}
          <div className="text-center text-xs text-gray-500">
            {isRegisterMode ? (
              <span>
                Already have an account?{' '}
                <button
                  type="button"
                  onClick={() => setIsRegisterMode(false)}
                  className="font-bold text-[var(--color-brand)] hover:underline cursor-pointer"
                >
                  Sign In
                </button>
              </span>
            ) : (
              <span>
                New to Service Assist?{' '}
                <button
                  type="button"
                  onClick={() => setIsRegisterMode(true)}
                  className="font-bold text-[var(--color-brand)] hover:underline cursor-pointer"
                >
                  Create Account
                </button>
              </span>
            )}
          </div>

          {/* Quick One-Tap Demo Logins */}
          <div className="pt-2 border-t border-gray-100">
            <span className="block text-[10px] font-extrabold uppercase tracking-wider text-amber-600 mb-2">
              ⚡ Quick One-Tap Demo Logins
            </span>

            <div className="space-y-2">
              <div className="p-2.5 bg-gray-50/80 rounded-2xl border border-gray-100 flex items-center justify-between gap-3">
                <div className="flex items-center gap-2.5 min-w-0">
                  <div className="w-8 h-8 rounded-full bg-emerald-100 text-emerald-700 flex items-center justify-center shrink-0">
                    <User className="w-4 h-4" />
                  </div>
                  <div className="min-w-0">
                    <span className="font-bold text-xs text-gray-900 block truncate">Priya Sharma</span>
                    <span className="text-[10px] text-gray-500 block truncate">Customer • Taj Nagri Phase 2, Agra</span>
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() => handleQuickLogin('CUSTOMER')}
                  className="px-3 py-1.5 bg-[var(--color-brand)] hover:bg-[var(--color-brand-hover)] text-white text-[11px] font-bold rounded-xl shrink-0 cursor-pointer shadow-2xs"
                >
                  Login
                </button>
              </div>

              <div className="p-2.5 bg-gray-50/80 rounded-2xl border border-gray-100 flex items-center justify-between gap-3">
                <div className="flex items-center gap-2.5 min-w-0">
                  <div className="w-8 h-8 rounded-full bg-blue-100 text-blue-700 flex items-center justify-center shrink-0">
                    <Wrench className="w-4 h-4" />
                  </div>
                  <div className="min-w-0">
                    <span className="font-bold text-xs text-gray-900 block truncate">Rajesh Sharma</span>
                    <span className="text-[10px] text-gray-500 block truncate">Partner Pro • Master AC & Appliance Tech</span>
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() => handleQuickLogin('PROFESSIONAL')}
                  className="px-3 py-1.5 bg-[var(--color-brand)] hover:bg-[var(--color-brand-hover)] text-white text-[11px] font-bold rounded-xl shrink-0 cursor-pointer shadow-2xs"
                >
                  Login
                </button>
              </div>

              <div className="p-2.5 bg-gray-50/80 rounded-2xl border border-gray-100 flex items-center justify-between gap-3">
                <div className="flex items-center gap-2.5 min-w-0">
                  <div className="w-8 h-8 rounded-full bg-purple-100 text-purple-700 flex items-center justify-center shrink-0">
                    <Shield className="w-4 h-4" />
                  </div>
                  <div className="min-w-0">
                    <span className="font-bold text-xs text-gray-900 block truncate">Operations Admin</span>
                    <span className="text-[10px] text-gray-500 block truncate">Admin • Service Assist Central</span>
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() => handleQuickLogin('ADMIN')}
                  className="px-3 py-1.5 bg-[var(--color-brand)] hover:bg-[var(--color-brand-hover)] text-white text-[11px] font-bold rounded-xl shrink-0 cursor-pointer shadow-2xs"
                >
                  Login
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
