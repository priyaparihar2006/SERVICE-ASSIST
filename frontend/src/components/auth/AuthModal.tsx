import React, { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { UserRole } from '../../types';
import { X, Mail, Lock, Phone, User, ArrowRight } from 'lucide-react';
import { BrandLogo } from '../common/BrandLogo';

export const AuthModal: React.FC = () => {
  const { isAuthModalOpen, closeAuthModal, login, register } = useAuth();
  const [isRegisterMode, setIsRegisterMode] = useState(false);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  const [selectedRole, setSelectedRole] = useState<UserRole>('CUSTOMER');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  if (!isAuthModalOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    const cleanEmail = email.trim().toLowerCase();
    if (!cleanEmail) {
      setError('Please enter your email address.');
      return;
    }

    const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
    if (!emailRegex.test(cleanEmail)) {
      setError('Please enter a valid email address (e.g. name@example.com).');
      return;
    }

    if (isRegisterMode) {
      const cleanName = name.trim();
      if (!cleanName) {
        setError('Please enter your full name.');
        return;
      }
      if (cleanName.length < 2) {
        setError('Full name must be at least 2 characters long.');
        return;
      }
      if (!/^[a-zA-Z\s]+$/.test(cleanName)) {
        setError('Full name must contain only letters and spaces (numbers and symbols are not allowed).');
        return;
      }

      const cleanPhone = phone.trim();
      if (cleanPhone) {
        if (!/^[6-9]\d{9}$/.test(cleanPhone)) {
          setError('Please enter a valid 10-digit Indian mobile number starting with 6, 7, 8, or 9.');
          return;
        }
      }

      if (password.length < 12) {
        setError('Password must be at least 12 characters long.');
        return;
      }
    } else {
      if (!password) {
        setError('Please enter your password.');
        return;
      }
    }

    setLoading(true);
    try {
      if (isRegisterMode) {
        await register(name.trim(), cleanEmail, phone.trim(), password, selectedRole);
      } else {
        await login(cleanEmail, password);
      }
    } catch (e: any) {
      setError(e.message || 'Authentication failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    setError('');
    closeAuthModal();
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="auth-modal-title"
      className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-black/60 backdrop-blur-xs overflow-y-auto animate-in fade-in duration-200"
    >
      <div className="relative w-full max-w-[calc(100%-1rem)] sm:max-w-md my-auto bg-white rounded-2xl sm:rounded-3xl shadow-2xl border border-gray-100 overflow-hidden max-h-[calc(100vh-2rem)] flex flex-col">
        {/* Top brand header */}
        <div className="bg-[var(--color-ink)] p-4 sm:p-6 text-white text-center relative border-b-2 border-[var(--color-brand)] shrink-0">
          <button
            type="button"
            onClick={handleClose}
            aria-label="Close authentication modal"
            className="absolute right-3 sm:right-4 top-3 sm:top-4 p-1.5 text-white/80 hover:text-white rounded-full hover:bg-white/10 transition-colors cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
          <div className="flex justify-center mb-1 sm:mb-2">
            <BrandLogo size="md" variant="light" showTagline={false} />
          </div>
          <p id="auth-modal-title" className="text-[11px] sm:text-xs text-brand-light mt-0.5 sm:mt-1 font-medium">
            Verified home professionals at your doorstep
          </p>
        </div>

        {/* Modal body */}
        <div className="p-4 sm:p-6 space-y-4 sm:space-y-5 overflow-y-auto">
          <form onSubmit={handleSubmit} className="space-y-3 sm:space-y-3.5">
            {error && (
              <div
                role="alert"
                className="p-2.5 sm:p-3 text-xs sm:text-sm text-red-700 bg-red-50 border border-red-200 rounded-xl leading-relaxed break-words font-medium"
              >
                {error}
              </div>
            )}

            {/* Full Name (Register only - only letters and spaces allowed) */}
            {isRegisterMode && (
              <div>
                <label htmlFor="auth-name" className="block text-xs font-semibold text-gray-700 mb-1">
                  Full Name
                </label>
                <div className="relative">
                  <User className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400 pointer-events-none" />
                  <input
                    id="auth-name"
                    name="name"
                    type="text"
                    required
                    autoComplete="name"
                    placeholder="e.g. Priya Sharma"
                    value={name}
                    onChange={(e) => {
                      // Strictly filter out digits and special characters
                      const clean = e.target.value.replace(/[^a-zA-Z\s]/g, '');
                      setName(clean);
                    }}
                    className="w-full pl-10 pr-3.5 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-base sm:text-sm text-gray-900 placeholder:text-gray-400 focus:outline-none focus:ring-2 focus:ring-[var(--color-brand)]/30 focus:border-[var(--color-brand)] focus:bg-white transition-all"
                  />
                </div>
              </div>
            )}

            {/* 1. Email Address (Always first) */}
            <div>
              <label htmlFor="auth-email" className="block text-xs font-semibold text-gray-700 mb-1">
                Email Address
              </label>
              <div className="relative">
                <Mail className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400 pointer-events-none" />
                <input
                  id="auth-email"
                  name="email"
                  type="email"
                  required
                  autoComplete="email"
                  placeholder="name@example.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="w-full pl-10 pr-3.5 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-base sm:text-sm text-gray-900 placeholder:text-gray-400 focus:outline-none focus:ring-2 focus:ring-[var(--color-brand)]/30 focus:border-[var(--color-brand)] focus:bg-white transition-all"
                />
              </div>
            </div>

            {/* Mobile Phone (Register only - strictly 10 digits numeric) */}
            {isRegisterMode && (
              <div>
                <label htmlFor="auth-phone" className="block text-xs font-semibold text-gray-700 mb-1">
                  Mobile Phone (India)
                </label>
                <div className="relative">
                  <Phone className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400 pointer-events-none" />
                  <input
                    id="auth-phone"
                    name="phone"
                    type="tel"
                    inputMode="numeric"
                    maxLength={10}
                    autoComplete="tel"
                    placeholder="9876543210 (10 digits)"
                    value={phone}
                    onChange={(e) => {
                      // Strictly digits only, max 10 chars
                      const digits = e.target.value.replace(/\D/g, '').slice(0, 10);
                      setPhone(digits);
                    }}
                    className="w-full pl-10 pr-3.5 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-base sm:text-sm text-gray-900 placeholder:text-gray-400 focus:outline-none focus:ring-2 focus:ring-[var(--color-brand)]/30 focus:border-[var(--color-brand)] focus:bg-white transition-all font-mono"
                  />
                </div>
              </div>
            )}

            {/* 2. Password (Always below email) */}
            <div>
              <label htmlFor="auth-password" className="block text-xs font-semibold text-gray-700 mb-1">
                Password
              </label>
              <div className="relative">
                <Lock className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400 pointer-events-none" />
                <input
                  id="auth-password"
                  name="password"
                  type="password"
                  required
                  minLength={isRegisterMode ? 12 : 1}
                  maxLength={72}
                  autoComplete={isRegisterMode ? 'new-password' : 'current-password'}
                  placeholder="••••••••••••"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="w-full pl-10 pr-3.5 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-base sm:text-sm text-gray-900 placeholder:text-gray-400 focus:outline-none focus:ring-2 focus:ring-[var(--color-brand)]/30 focus:border-[var(--color-brand)] focus:bg-white transition-all"
                />
              </div>
            </div>

            {/* Account type selection */}
            <div>
              <span className="block text-xs font-semibold text-gray-700 mb-1.5">
                Account type for new registrations
              </span>
              <div className="grid grid-cols-2 gap-2 w-full">
                {(['CUSTOMER', 'PROFESSIONAL'] as UserRole[]).map((r) => {
                  const isSelected = selectedRole === r;
                  return (
                    <button
                      key={r}
                      type="button"
                      onClick={() => setSelectedRole(r)}
                      className={`w-full py-2.5 px-2 text-xs font-semibold rounded-xl border capitalize transition-all cursor-pointer text-center truncate ${
                        isSelected
                          ? 'bg-[var(--color-brand-light)] border-[var(--color-brand)] text-[var(--color-brand-hover)] font-bold shadow-xs'
                          : 'border-gray-200 bg-gray-50/50 text-gray-600 hover:bg-gray-100 hover:border-gray-300'
                      }`}
                    >
                      {r.toLowerCase()}
                    </button>
                  );
                })}
              </div>
            </div>

            {/* Submit Button */}
            <button
              type="submit"
              disabled={loading}
              className="w-full flex items-center justify-center gap-2 py-3 px-4 bg-[var(--color-brand)] hover:bg-[var(--color-brand-hover)] text-white font-bold text-sm rounded-xl transition-all shadow-md shadow-brand/20 active:scale-[0.99] mt-2 cursor-pointer disabled:opacity-60 disabled:cursor-not-allowed"
            >
              <span>{loading ? 'Authenticating...' : isRegisterMode ? 'Create Account' : 'Sign In'}</span>
              <ArrowRight className="w-4 h-4 shrink-0" />
            </button>
          </form>

          {/* Toggle Login / Register */}
          <div className="text-center text-xs text-gray-500 pt-1">
            {isRegisterMode ? (
              <span>
                Already have an account?{' '}
                <button
                  type="button"
                  onClick={() => {
                    setError('');
                    setIsRegisterMode(false);
                  }}
                  className="font-bold text-[var(--color-brand)] hover:underline cursor-pointer ml-1"
                >
                  Sign In
                </button>
              </span>
            ) : (
              <span>
                Don't have an account yet?{' '}
                <button
                  type="button"
                  onClick={() => {
                    setError('');
                    setIsRegisterMode(true);
                  }}
                  className="font-bold text-[var(--color-brand)] hover:underline cursor-pointer ml-1"
                >
                  Create New Account
                </button>
              </span>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
