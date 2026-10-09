import React, { useState } from 'react';
import { X, ShieldCheck, CheckCircle2, ArrowRight, Lock, Sparkles, User, Phone } from 'lucide-react';
import { ServiceItem } from '../../types/landing';

interface AuthModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (phone: string, name?: string) => void;
  bookingService?: ServiceItem | null;
  isCartCheckout?: boolean;
  cartCount?: number;
  cartTotal?: number;
}

export const AuthModal: React.FC<AuthModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
  bookingService,
  isCartCheckout,
  cartCount = 0,
  cartTotal = 0,
}) => {
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  const [step, setStep] = useState<'phone' | 'otp'>('phone');
  const [otp, setOtp] = useState('');
  const [loading, setLoading] = useState(false);

  if (!isOpen) return null;

  const handleSendOtp = (e: React.FormEvent) => {
    e.preventDefault();
    if (phone.length >= 10) {
      setLoading(true);
      setTimeout(() => {
        setLoading(false);
        setStep('otp');
      }, 400);
    }
  };

  const handleVerifyOtp = (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setTimeout(() => {
      setLoading(false);
      onSuccess(phone, name.trim() || 'Homeowner');
      onClose();
    }, 450);
  };

  const handleQuickFillOtp = () => {
    setOtp('1234');
  };

  const isBookingFlow = Boolean(bookingService || isCartCheckout);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/70 backdrop-blur-sm animate-in fade-in duration-150">
      <div className="bg-white rounded-3xl max-w-sm sm:max-w-md w-full p-6 sm:p-7 shadow-2xl relative border border-slate-100 max-h-[90vh] overflow-y-auto">
        <button
          onClick={onClose}
          className="absolute top-5 right-5 p-2 rounded-full hover:bg-slate-100 text-slate-400 hover:text-slate-700 transition-colors cursor-pointer"
          aria-label="Close"
        >
          <X className="w-5 h-5" />
        </button>

        {/* Header Icon & Title */}
        <div className="text-center mb-5">
          <div className="w-12 h-12 rounded-2xl bg-emerald-50 text-emerald-600 flex items-center justify-center mx-auto mb-3">
            {isBookingFlow ? (
              <Lock className="w-6 h-6 stroke-[2.2] text-emerald-600" />
            ) : (
              <ShieldCheck className="w-6 h-6 stroke-[2.2] text-emerald-600" />
            )}
          </div>
          <h3 className="text-xl font-extrabold text-slate-900 font-display">
            {step === 'phone'
              ? isBookingFlow
                ? 'Sign In to Confirm Booking'
                : 'Welcome to Service Assist'
              : 'Verify Mobile OTP'}
          </h3>
          <p className="text-xs text-slate-500 mt-1 max-w-xs mx-auto">
            {step === 'phone'
              ? isBookingFlow
                ? 'Enter your mobile number to lock in verified technician dispatch and track your service.'
                : 'Enter your phone number to sign in or view past bookings'
              : `Enter the 4-digit verification code sent to +91 ${phone}`}
          </p>
        </div>

        {/* Booking Context Banner */}
        {isBookingFlow && step === 'phone' && (
          <div className="mb-5 p-3.5 bg-gradient-to-r from-emerald-50/90 to-teal-50/90 border border-emerald-200/80 rounded-2xl flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-emerald-600 text-white flex items-center justify-center shrink-0 shadow-xs">
              <Sparkles className="w-4 h-4" />
            </div>
            <div className="min-w-0 flex-1">
              <span className="text-[10px] font-black uppercase tracking-wider text-emerald-700 block">
                {isCartCheckout ? 'Checkout Service Order' : 'Instant Service Booking'}
              </span>
              <p className="text-xs font-bold text-slate-900 truncate">
                {bookingService ? bookingService.title : `${cartCount} Selected Service${cartCount > 1 ? 's' : ''}`}
              </p>
              <p className="text-[11px] font-bold text-emerald-700">
                Verified Pro Dispatch · 30-Day Rework Guarantee
              </p>
            </div>
          </div>
        )}

        {step === 'phone' ? (
          <form onSubmit={handleSendOtp} className="space-y-4">
            <div>
              <label className="text-xs font-bold text-slate-700 block mb-1">
                Full Name <span className="text-slate-400 font-normal">(Optional)</span>
              </label>
              <div className="relative">
                <span className="absolute left-3 top-2.5 text-slate-400">
                  <User className="w-4 h-4" />
                </span>
                <input
                  type="text"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="e.g. Aditi Sharma"
                  className="w-full bg-slate-50 border border-slate-200 rounded-xl py-2 pl-9 pr-3 text-sm text-slate-800 focus:outline-emerald-500 font-medium"
                />
              </div>
            </div>

            <div>
              <label className="text-xs font-bold text-slate-700 block mb-1">
                Mobile Number <span className="text-emerald-600">*</span>
              </label>
              <div className="relative">
                <span className="absolute left-3 top-2.5 text-xs font-bold text-slate-500">+91</span>
                <input
                  type="tel"
                  required
                  autoFocus
                  value={phone}
                  onChange={(e) => setPhone(e.target.value.replace(/\D/g, '').slice(0, 10))}
                  placeholder="98765 43210"
                  maxLength={10}
                  className="w-full bg-slate-50 border border-slate-200 rounded-xl py-2 pl-12 pr-3 text-sm text-slate-800 focus:outline-emerald-500 font-medium"
                />
              </div>
              <p className="text-[11px] text-slate-400 mt-1">
                We'll send a 4-digit OTP via SMS to verify your booking request.
              </p>
            </div>

            <button
              type="submit"
              disabled={loading || phone.length < 10}
              className="w-full py-3 bg-emerald-600 hover:bg-emerald-700 disabled:opacity-50 text-white rounded-xl text-xs sm:text-sm font-bold shadow-md shadow-emerald-600/30 transition-all flex items-center justify-center gap-2 cursor-pointer"
            >
              <span>{loading ? 'Sending OTP...' : 'Continue with OTP'}</span>
              <ArrowRight className="w-4 h-4" />
            </button>
          </form>
        ) : (
          <form onSubmit={handleVerifyOtp} className="space-y-4">
            <div>
              <div className="flex items-center justify-between mb-1">
                <label className="text-xs font-bold text-slate-700 block">
                  4-Digit Security Code
                </label>
                <button
                  type="button"
                  onClick={handleQuickFillOtp}
                  className="text-[11px] text-emerald-600 hover:text-emerald-700 font-bold underline cursor-pointer"
                >
                  Auto-fill Demo (1234)
                </button>
              </div>
              <input
                type="text"
                required
                autoFocus
                value={otp}
                onChange={(e) => setOtp(e.target.value.replace(/\D/g, '').slice(0, 4))}
                placeholder="• • • •"
                maxLength={4}
                className="w-full bg-slate-50 border border-slate-200 rounded-xl py-3 px-3 text-center text-2xl font-mono font-bold tracking-widest text-slate-800 focus:outline-emerald-500"
              />
              <p className="text-[11px] text-slate-400 text-center mt-2">
                Demo code: enter <span className="font-bold text-slate-700">1234</span> or any 4 digits
              </p>
            </div>

            <button
              type="submit"
              disabled={loading || otp.length < 4}
              className="w-full py-3 bg-emerald-600 hover:bg-emerald-700 disabled:opacity-50 text-white rounded-xl text-xs sm:text-sm font-bold shadow-md shadow-emerald-600/30 transition-all flex items-center justify-center gap-2 cursor-pointer"
            >
              <CheckCircle2 className="w-4 h-4" />
              <span>
                {loading
                  ? 'Verifying...'
                  : isBookingFlow
                  ? 'Verify & Proceed to Booking'
                  : 'Verify & Sign In'}
              </span>
            </button>

            <button
              type="button"
              onClick={() => {
                setStep('phone');
                setOtp('');
              }}
              className="w-full text-xs text-slate-500 hover:text-slate-800 text-center block pt-1 cursor-pointer"
            >
              ← Change Mobile Number
            </button>
          </form>
        )}
      </div>
    </div>
  );
};


