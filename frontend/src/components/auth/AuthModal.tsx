import React, { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { UserRole } from '../../types';
import { X, ShieldCheck, UserCheck, Wrench, Crown, Sparkles, Mail, Lock, Phone, ArrowRight, Home } from 'lucide-react';
import { BrandLogo } from '../common/BrandLogo';

export const AuthModal: React.FC = () => {
  const { isAuthModalOpen, closeAuthModal, login, register } = useAuth();
  const [isRegisterMode, setIsRegisterMode] = useState(false);
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [email, setEmail] = useState('');
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  const [selectedRole, setSelectedRole] = useState<UserRole>('CUSTOMER');
  const [loading, setLoading] = useState(false);

  if (!isAuthModalOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true); setError('');
    try {
      if (isRegisterMode) {
        await register(name, email, phone, password, selectedRole);
      } else {
        await login(email, password);
      }
    } catch (e) { setError(e.message); } finally {
      setLoading(false);
    }
  };


  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-xs animate-in fade-in duration-200">
      <div className="relative w-full max-w-md bg-white rounded-3xl shadow-2xl border border-gray-100 overflow-hidden">
        {/* Top brand header */}
        <div className="bg-[#15252B] p-6 text-white text-center relative border-b-2 border-[#FF7A00]">
          <button
            onClick={closeAuthModal}
            className="absolute right-4 top-4 p-1.5 text-white/80 hover:text-white rounded-full hover:bg-white/10 transition-colors cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
          <div className="flex justify-center mb-2">
            <BrandLogo size="md" variant="light" showTagline={false} />
          </div>
          <p className="text-xs text-orange-200 mt-1">Verified home professionals at your doorstep</p>
        </div>

        <div className="p-6 space-y-5">
          {/* Form */}
          <form onSubmit={handleSubmit} className="space-y-3">
            {error && <p role="alert" className="text-sm text-red-600">{error}</p>}
            <label className="block text-xs font-semibold text-gray-700">Password
              <input type="password" required minLength={isRegisterMode ? 12 : 1} maxLength={72} autoComplete={isRegisterMode ? 'new-password' : 'current-password'} value={password} onChange={e => setPassword(e.target.value)} className="w-full px-3.5 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-sm" />
            </label>
            {isRegisterMode && (
              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">Full Name</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Priya Sharma"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  className="w-full px-3.5 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-[#FF7A00]/30 focus:border-[#FF7A00]"
                />
              </div>
            )}

            <div>
              <label className="block text-xs font-semibold text-gray-700 mb-1">Email Address</label>
              <div className="relative">
                <Mail className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                <input
                  type="email"
                  required
                  placeholder="name@example.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="w-full pl-10 pr-3.5 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-[#FF7A00]/30 focus:border-[#FF7A00]"
                />
              </div>
            </div>

            {isRegisterMode && (
              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">Mobile Phone (India)</label>
                <div className="relative">
                  <Phone className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                  <input
                    type="tel"
                    placeholder="+91 98765 43210"
                    value={phone}
                    onChange={(e) => setPhone(e.target.value)}
                    className="w-full pl-10 pr-3.5 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-[#FF7A00]/30 focus:border-[#FF7A00]"
                  />
                </div>
              </div>
            )}

            <div>
              <label className="block text-xs font-semibold text-gray-700 mb-1">Account type for new registrations</label>
              <div className="grid grid-cols-3 gap-2">
                {(['CUSTOMER', 'PROFESSIONAL'] as UserRole[]).map((r) => (
                  <button
                    key={r}
                    type="button"
                    onClick={() => setSelectedRole(r)}
                    className={`py-2 px-2 text-xs font-medium rounded-xl border capitalize cursor-pointer ${
                      selectedRole === r
                        ? 'bg-[#FFF1E5] border-[#FF7A00] text-[#E85D04] font-bold'
                        : 'border-gray-200 text-gray-600 hover:bg-gray-50'
                    }`}
                  >
                    {r.toLowerCase()}
                  </button>
                ))}
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full flex items-center justify-center gap-2 py-3 bg-[#FF7A00] hover:bg-[#E85D04] text-white font-bold text-sm rounded-xl transition-all shadow-md shadow-orange-500/20 active:scale-[0.99] mt-2 cursor-pointer"
            >
              <span>{loading ? 'Authenticating...' : isRegisterMode ? 'Create Account' : 'Sign In'}</span>
              <ArrowRight className="w-4 h-4" />
            </button>
          </form>

          {/* Toggle Login / Register */}
          <div className="text-center text-xs text-gray-500">
            {isRegisterMode ? (
              <span>
                Already have an account?{' '}
                <button
                  type="button"
                  onClick={() => setIsRegisterMode(false)}
                  className="font-bold text-[#FF7A00] hover:underline cursor-pointer"
                >
                  Sign In
                </button>
              </span>
            ) : (
              <span>
                Don't have an account yet?{' '}
                <button
                  type="button"
                  onClick={() => setIsRegisterMode(true)}
                  className="font-bold text-[#FF7A00] hover:underline cursor-pointer"
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
