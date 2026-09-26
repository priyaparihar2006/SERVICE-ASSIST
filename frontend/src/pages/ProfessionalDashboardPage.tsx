import { api, getAll } from '../services/api';
import { useAuth } from '../context/AuthContext';
import { ProfileSettings } from '../components/account/ProfileSettings';
import { ProfessionalSettings } from '../components/account/ProfessionalSettings';
import { apiFetch } from '../services/api';
import { ImageWithFallback } from '../components/common/ImageWithFallback';
import { getProfessionalImage } from '../utils/professionalImages';
import React, { useState, useEffect } from 'react';
import { Booking } from '../types';
import { useChat } from '../context/ChatContext';
import { chatApi } from '../services/chat';
import {
  Star,
  CheckCircle2,
  Clock,
  MessageSquare,
  ShieldCheck,
  TrendingUp,
  Phone,
  Navigation,
  FileText,
  KeyRound,
  Check,
  Copy,
} from 'lucide-react';

export const ProfessionalDashboardPage: React.FC<{ onNavigate?: (path: string) => void }> = ({ onNavigate }) => {
  const { user } = useAuth();
  const { subscribe } = useChat();
  const [unreadByBooking, setUnreadByBooking] = useState<Record<string, number>>({});
  const [profile, setProfile] = useState<any>(null);
  const [earnings, setEarnings] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [rejected, setRejected] = useState<{ bookingId: string; serviceName: string; scheduledDate: string; scheduledTimeSlot: string; rejectedAt: string }[]>([]);
  const [jobFilter, setJobFilter] = useState('ALL');
  const [otpInputs, setOtpInputs] = useState<Record<string, string>>({});
  const [otpError, setOtpError] = useState<Record<string, string>>({});

  const fetchJobs = async () => {
    try {
      setLoading(true);
      setError('');
      const [jobs, p, e, conversations, declined] = await Promise.all([
        getAll('/professionals/bookings', 'bookings'),
        api('/professionals/profile'),
        api('/professionals/earnings'),
        chatApi.list().catch(() => []),
        api('/professionals/rejected-bookings'),
      ]);
      setBookings(jobs);
      setProfile(p.professional);
      setEarnings(e.earnings);
      setRejected(declined.requests);
      setUnreadByBooking(Object.fromEntries(conversations.map((c) => [c.booking.id, c.unreadCount])));
    } catch (e: any) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchJobs();
  }, []);

  useEffect(() => {
    const offBooking = subscribe('booking:updated', fetchJobs);
    const offConnected = subscribe('connected', fetchJobs);
    const offMessage = subscribe('message:new', fetchJobs);
    const timer = window.setInterval(() => {
      if (document.visibilityState === 'visible') fetchJobs();
    }, 30000);
    return () => {
      offBooking();
      offConnected();
      offMessage();
      window.clearInterval(timer);
    };
  }, [subscribe]);

  const respond = async (bookingId: string, action: 'accept' | 'reject') => {
    try {
      await api(`/bookings/${bookingId}/${action}`, { method: 'POST', body: '{}' });
      await fetchJobs();
    } catch (e: any) {
      setError(e.message);
    }
  };

  const handleUpdateStatus = async (bookingId: string, newStatus: Booking['status'], otp?: string) => {
    try {
      const res = await apiFetch(`/api/bookings/${bookingId}/status`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ status: newStatus, otp }),
      });
      if (res.ok) {
        fetchJobs();
      } else {
        const data = await res.json();
        setOtpError((prev) => ({ ...prev, [bookingId]: data.message || 'Verification failed' }));
      }
    } catch (e: any) {
      setOtpError((prev) => ({ ...prev, [bookingId]: e.message }));
    }
  };

  const handleVerifyOtpAndStart = (booking: Booking) => {
    const otp = otpInputs[booking.id];
    if (!otp || otp.length !== 4) {
      setOtpError((prev) => ({ ...prev, [booking.id]: 'Please enter the 4-digit start OTP provided by customer.' }));
      return;
    }
    handleUpdateStatus(booking.id, 'IN_PROGRESS', otp);
  };

  const copyToClipboard = (text: string) => {
    navigator.clipboard?.writeText(text);
  };

  return (
    <div className="min-h-screen bg-[var(--color-brand-soft)]/30 py-8 sm:py-12">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Partner Header Banner */}
        <div className="bg-gradient-to-br from-[var(--color-ink)] to-[var(--color-ink)] text-white rounded-3xl p-6 sm:p-8 shadow-xl mb-8 flex flex-col md:flex-row md:items-center justify-between gap-6 border border-brand/20">
          <div className="flex items-center gap-4">
            <ImageWithFallback
              src={user?.avatar || getProfessionalImage(user)}
              fallbackProfession={profile?.profession || 'HVAC Specialist'}
              alt={user?.name || 'Partner Profile'}
              className="w-16 h-16 rounded-2xl object-cover object-top border-2 border-[var(--color-brand)]"
              referrerPolicy="no-referrer"
            />
            <div>
              <div className="flex items-center gap-2">
                <h1 className="text-xl sm:text-2xl font-black font-['Outfit']">{user?.name}</h1>
                <span className="text-[10px] font-bold bg-[var(--color-brand)] text-white px-2.5 py-0.5 rounded-full flex items-center gap-1 shadow-xs">
                  <ShieldCheck className="w-3 h-3" />
                  <span>{profile?.verificationStatus || 'Verified Partner'}</span>
                </span>
              </div>
              <p className="text-xs text-gray-300 mt-1">
                {profile?.businessName || 'Master Technician'} · {profile?.id || user?.id?.slice(0, 8)}
              </p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <div className="px-3.5 py-2 bg-white/10 rounded-2xl border border-white/10 text-center">
              <span className="text-[10px] uppercase font-bold text-gray-400 block">Status</span>
              <span className="text-xs font-bold text-brand-bright flex items-center justify-center gap-1">
                <span className="w-2 h-2 rounded-full bg-brand-bright animate-pulse" />
                {profile?.isAvailableToday ? 'Accepting Assignments' : 'Online'}
              </span>
            </div>
          </div>
        </div>

        <ProfileSettings />
        <ProfessionalSettings onSaved={fetchJobs} />

        {/* Metrics Grid */}
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
          <div className="bg-white p-5 rounded-3xl border border-gray-100 shadow-xs">
            <div className="flex items-center justify-between text-gray-400 mb-2">
              <span className="text-xs font-bold uppercase tracking-wider">Total Earnings</span>
              <div className="w-8 h-8 rounded-xl bg-brand-soft text-brand flex items-center justify-center">
                <TrendingUp className="w-4 h-4" />
              </div>
            </div>
            <div className="text-2xl font-black text-gray-900 font-['Outfit']">₹{earnings.toLocaleString()}</div>
            <p className="text-[11px] text-brand font-semibold mt-1">Collected on completed jobs</p>
          </div>

          <div className="bg-white p-5 rounded-3xl border border-gray-100 shadow-xs">
            <div className="flex items-center justify-between text-gray-400 mb-2">
              <span className="text-xs font-bold uppercase tracking-wider">Partner Rating</span>
              <div className="w-8 h-8 rounded-xl bg-amber-50 text-amber-500 flex items-center justify-center">
                <Star className="w-4 h-4 fill-amber-400" />
              </div>
            </div>
            <div className="text-2xl font-black text-gray-900 font-['Outfit']">{profile?.rating || 4.92} / 5</div>
            <p className="text-[11px] text-gray-500 font-medium mt-1">{profile?.reviewsCount || 124} verified ratings</p>
          </div>

          <div className="bg-white p-5 rounded-3xl border border-gray-100 shadow-xs">
            <div className="flex items-center justify-between text-gray-400 mb-2">
              <span className="text-xs font-bold uppercase tracking-wider">Active Assignments</span>
              <div className="w-8 h-8 rounded-xl bg-blue-50 text-blue-500 flex items-center justify-center">
                <Clock className="w-4 h-4" />
              </div>
            </div>
            <div className="text-2xl font-black text-gray-900 font-['Outfit']">
              {bookings.filter((b) => b.status !== 'COMPLETED' && b.status !== 'CANCELLED').length}
            </div>
            <p className="text-[11px] text-gray-500 font-medium mt-1">Scheduled doorstep tasks</p>
          </div>

          <div className="bg-white p-5 rounded-3xl border border-gray-100 shadow-xs">
            <div className="flex items-center justify-between text-gray-400 mb-2">
              <span className="text-xs font-bold uppercase tracking-wider">Service Guarantee</span>
              <div className="w-8 h-8 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center">
                <ShieldCheck className="w-4 h-4" />
              </div>
            </div>
            <div className="text-2xl font-black text-gray-900 font-['Outfit']">100%</div>
            <p className="text-[11px] text-emerald-600 font-semibold mt-1">Direct payout on verification</p>
          </div>
        </div>

        {/* Assigned Job Cards Section (Matching Screenshot 4) */}
        <div className="space-y-6">
          <div className="flex items-center justify-between">
            <h2 className="text-xl font-black text-gray-900 font-['Outfit']">
              Live Job Management
            </h2>
          </div>

          {loading && <p className="text-xs text-gray-500">Loading assignments...</p>}
          {error && <p className="text-xs text-red-600">{error}</p>}
          {!loading && !bookings.length && !rejected.length && (
            <div className="bg-white rounded-3xl p-12 text-center border border-gray-100 max-w-md mx-auto">
              <Clock className="w-12 h-12 text-gray-300 mx-auto mb-3" />
              <h3 className="font-bold text-gray-900 text-base mb-1">No Active Orders</h3>
              <p className="text-xs text-gray-500">
                New customer orders in your locality will appear here automatically.
              </p>
            </div>
          )}

          <div className="space-y-5">
            {bookings.map((b) => {
              const mainItem = b.items?.[0];
              const serviceName = mainItem?.service?.name || b.serviceName || 'Doorstep Service';
              const variantName = mainItem?.variant?.name || b.variantName || 'Standard Package';
              const serviceImage = mainItem?.service?.image || '/service-images/cleaning.svg';
              const duration = mainItem?.service?.durationMin || 90;

              const isAssigned = ['PENDING', 'ASSIGNED', 'CONFIRMED', 'ON_THE_WAY', 'ARRIVED', 'IN_PROGRESS', 'COMPLETED'].includes(b.status);
              const isOnWay = ['ON_THE_WAY', 'ARRIVED', 'IN_PROGRESS', 'COMPLETED'].includes(b.status);
              const isArrived = ['ARRIVED', 'IN_PROGRESS', 'COMPLETED'].includes(b.status);
              const isStarted = ['IN_PROGRESS', 'COMPLETED'].includes(b.status);
              const isPaid = b.status === 'COMPLETED';

              return (
                <div
                  key={b.id}
                  className="bg-white rounded-3xl border border-gray-100 shadow-sm overflow-hidden p-5 sm:p-6 space-y-4 max-w-2xl"
                >
                  {/* Top Bar matching Screenshot 4: # SRV-50068 📋 | ₹3673 | ● Job Assigned */}
                  <div className="flex flex-wrap items-center justify-between gap-3 pb-3 border-b border-gray-100">
                    <div className="flex items-center gap-2">
                      <div className="flex items-center gap-1.5 px-3 py-1 bg-gray-100 rounded-xl text-xs font-mono font-bold text-gray-700">
                        <span># {b.id?.slice(0, 10) || 'SRV-50068'}</span>
                        <button
                          type="button"
                          onClick={() => copyToClipboard(b.id)}
                          className="text-gray-400 hover:text-gray-700 cursor-pointer"
                        >
                          <Copy className="w-3.5 h-3.5" />
                        </button>
                      </div>

                      <span className="px-3 py-1 rounded-xl bg-emerald-50 text-emerald-700 text-xs font-bold flex items-center gap-1.5">
                        <span className="w-2 h-2 rounded-full bg-emerald-500" />
                        <span>{b.status === 'COMPLETED' ? 'Completed' : 'Job Assigned'}</span>
                      </span>
                    </div>

                    <div className="px-3 py-1 bg-emerald-100/70 text-emerald-800 rounded-xl text-sm font-black font-['Outfit']">
                      ₹{b.total}
                    </div>
                  </div>

                  {/* Service Item Overview */}
                  <div className="flex items-center gap-3.5">
                    <img
                      src={serviceImage}
                      alt={serviceName}
                      className="w-16 h-16 rounded-2xl object-cover border border-gray-100 shrink-0"
                    />
                    <div className="min-w-0 flex-1">
                      <h3 className="font-black text-sm text-gray-900 leading-tight truncate font-['Outfit']">
                        {serviceName}
                      </h3>
                      <div className="inline-flex items-center px-2.5 py-0.5 rounded-md bg-emerald-50 text-emerald-800 text-[11px] font-semibold mt-1">
                        {variantName} • {duration} Mins
                      </div>
                      <p className="text-xs text-gray-500 flex items-center gap-1.5 mt-1 font-medium">
                        <Clock className="w-3.5 h-3.5 text-emerald-600" />
                        <span>{b.scheduledDate || 'Today'} • {b.scheduledTimeSlot || '02:00 PM'}</span>
                      </p>
                    </div>
                  </div>

                  {/* Customer Information Card */}
                  <div className="bg-gray-50/90 rounded-2xl p-4 border border-gray-100 space-y-3">
                    <div className="flex items-center justify-between gap-3">
                      <div className="flex items-center gap-3 min-w-0">
                        <div className="w-10 h-10 rounded-full bg-emerald-100 text-emerald-800 flex items-center justify-center shrink-0">
                          <span className="font-black text-xs">{b.userName ? b.userName.slice(0, 2).toUpperCase() : 'PS'}</span>
                        </div>
                        <div className="min-w-0">
                          <span className="font-extrabold text-sm text-gray-900 block truncate">
                            {b.userName || 'Priya Sharma'}
                          </span>
                          <span className="text-xs text-gray-500 block truncate">
                            {b.userPhone || '+91 98765 43210'}
                          </span>
                        </div>
                      </div>

                      <div className="flex items-center gap-2 shrink-0">
                        <a
                          href={`tel:${b.userPhone || '+919876543210'}`}
                          className="w-9 h-9 rounded-full bg-emerald-600 hover:bg-emerald-700 text-white flex items-center justify-center shadow-2xs transition-colors"
                        >
                          <Phone className="w-4 h-4" />
                        </a>
                        <button
                          type="button"
                          onClick={() => onNavigate?.(`/messages?booking=${b.id}`)}
                          className="w-9 h-9 rounded-full bg-white border border-gray-200 text-emerald-700 hover:bg-gray-100 flex items-center justify-center shadow-2xs transition-colors cursor-pointer"
                        >
                          <MessageSquare className="w-4 h-4" />
                        </button>
                      </div>
                    </div>

                    {/* Destination Address */}
                    <div className="pt-2 border-t border-gray-200/60 flex items-start justify-between gap-2 text-xs">
                      <div>
                        <span className="text-[10px] font-bold text-gray-400 block uppercase">
                          Destination Address
                        </span>
                        <p className="text-gray-700 mt-0.5 leading-relaxed">
                          {b.address?.house ? `${b.address.house}, ${b.address.area}, ${b.address.city}` : 'Flat 402, Royal Residency, Taj Nagri Phase 2, Taj Nagri, Agra'}
                        </p>
                      </div>

                      <a
                        href={`https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(
                          b.address?.house ? `${b.address.house} ${b.address.area} ${b.address.city}` : 'Agra'
                        )}`}
                        target="_blank"
                        rel="noreferrer"
                        className="px-2.5 py-1 bg-white border border-gray-200 rounded-xl text-gray-700 text-[11px] font-bold flex items-center gap-1 hover:bg-gray-100 shrink-0"
                      >
                        <Navigation className="w-3 h-3 text-emerald-600" />
                        <span>Map</span>
                      </a>
                    </div>

                    <div className="text-[11px] text-gray-500 flex items-center gap-1.5">
                      <FileText className="w-3.5 h-3.5 text-gray-400" />
                      <span>Duration: {duration} Minutes (1x slot)</span>
                    </div>
                  </div>

                  {/* Horizontal 5-Step Tracker Bar (Screenshot 4) */}
                  <div className="bg-gray-50/70 p-3.5 rounded-2xl border border-gray-100">
                    <div className="flex items-center justify-between text-[11px] font-bold">
                      <div className="flex flex-col items-center gap-1">
                        <div className={`w-6 h-6 rounded-full flex items-center justify-center text-white ${isAssigned ? 'bg-emerald-600' : 'bg-gray-200'}`}>
                          {isAssigned ? <Check className="w-3.5 h-3.5 stroke-[3]" /> : null}
                        </div>
                        <span className={isAssigned ? 'text-gray-900' : 'text-gray-400'}>Assigned</span>
                      </div>

                      <div className={`flex-1 h-0.5 -mt-4 mx-1 ${isOnWay ? 'bg-emerald-600' : 'bg-gray-200'}`} />

                      <div className="flex flex-col items-center gap-1">
                        <div className={`w-6 h-6 rounded-full flex items-center justify-center text-white ${isOnWay ? 'bg-emerald-600' : 'bg-gray-200'}`}>
                          {isOnWay ? <Check className="w-3.5 h-3.5 stroke-[3]" /> : null}
                        </div>
                        <span className={isOnWay ? 'text-gray-900' : 'text-gray-400'}>On Way</span>
                      </div>

                      <div className={`flex-1 h-0.5 -mt-4 mx-1 ${isArrived ? 'bg-emerald-600' : 'bg-gray-200'}`} />

                      <div className="flex flex-col items-center gap-1">
                        <div className={`w-6 h-6 rounded-full flex items-center justify-center text-white ${isArrived ? 'bg-emerald-600' : 'bg-gray-200'}`}>
                          {isArrived ? <Check className="w-3.5 h-3.5 stroke-[3]" /> : null}
                        </div>
                        <span className={isArrived ? 'text-gray-900' : 'text-gray-400'}>Arrived</span>
                      </div>

                      <div className={`flex-1 h-0.5 -mt-4 mx-1 ${isStarted ? 'bg-emerald-600' : 'bg-gray-200'}`} />

                      <div className="flex flex-col items-center gap-1">
                        <div className={`w-6 h-6 rounded-full flex items-center justify-center text-white ${isStarted ? 'bg-emerald-600' : 'bg-gray-200'}`}>
                          {isStarted ? <Check className="w-3.5 h-3.5 stroke-[3]" /> : null}
                        </div>
                        <span className={isStarted ? 'text-gray-900' : 'text-gray-400'}>Started</span>
                      </div>

                      <div className={`flex-1 h-0.5 -mt-4 mx-1 ${isPaid ? 'bg-emerald-600' : 'bg-gray-200'}`} />

                      <div className="flex flex-col items-center gap-1">
                        <div className={`w-6 h-6 rounded-full flex items-center justify-center text-white ${isPaid ? 'bg-emerald-600' : 'bg-gray-200'}`}>
                          {isPaid ? <Check className="w-3.5 h-3.5 stroke-[3]" /> : null}
                        </div>
                        <span className={isPaid ? 'text-gray-900' : 'text-gray-400'}>Payment</span>
                      </div>
                    </div>
                  </div>

                  {/* Actions & OTP Section */}
                  {b.status === 'ASSIGNED' && (
                    <div className="flex gap-3">
                      <button
                        type="button"
                        onClick={() => respond(b.id, 'accept')}
                        className="flex-1 py-3 bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs rounded-xl shadow-xs cursor-pointer"
                      >
                        Accept Booking Request ✓
                      </button>
                      <button
                        type="button"
                        onClick={() => respond(b.id, 'reject')}
                        className="px-4 py-3 border border-gray-200 text-gray-600 hover:bg-gray-50 text-xs font-bold rounded-xl cursor-pointer"
                      >
                        Reject
                      </button>
                    </div>
                  )}

                  {b.status === 'CONFIRMED' && (
                    <button
                      type="button"
                      onClick={() => handleUpdateStatus(b.id, 'ON_THE_WAY')}
                      className="w-full py-3.5 bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs rounded-2xl shadow-xs cursor-pointer"
                    >
                      Start Driving (On the Way) 🚗
                    </button>
                  )}

                  {b.status === 'ON_THE_WAY' && (
                    <button
                      type="button"
                      onClick={() => handleUpdateStatus(b.id, 'ARRIVED')}
                      className="w-full py-3.5 bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs rounded-2xl shadow-xs cursor-pointer"
                    >
                      I Have Arrived at Doorstep 📍
                    </button>
                  )}

                  {b.status === 'ARRIVED' && (
                    <div className="space-y-3">
                      <div className="p-3 bg-amber-50 border border-amber-200 rounded-2xl flex items-center gap-2 text-xs text-amber-900 font-semibold">
                        <KeyRound className="w-4 h-4 text-amber-600 shrink-0" />
                        <span>Ask customer for the 4-digit start OTP to begin work.</span>
                      </div>

                      <div className="flex items-center gap-2">
                        <input
                          type="text"
                          maxLength={4}
                          placeholder="Enter 4-Digit OTP"
                          value={otpInputs[b.id] || ''}
                          onChange={(e) => {
                            const val = e.target.value.replace(/\D/g, '').slice(0, 4);
                            setOtpInputs((prev) => ({ ...prev, [b.id]: val }));
                          }}
                          className="w-40 px-4 py-3 bg-white border border-gray-200 rounded-xl text-center font-mono font-black text-sm tracking-widest focus:outline-none focus:ring-2 focus:ring-emerald-500"
                        />

                        <button
                          type="button"
                          onClick={() => handleVerifyOtpAndStart(b)}
                          className="flex-1 py-3 bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs rounded-xl shadow-xs flex items-center justify-center gap-1.5 cursor-pointer"
                        >
                          <KeyRound className="w-4 h-4" />
                          <span>Verify Customer OTP & Start</span>
                        </button>
                      </div>

                      {otpError[b.id] && (
                        <p className="text-xs font-semibold text-red-600">{otpError[b.id]}</p>
                      )}
                    </div>
                  )}

                  {b.status === 'IN_PROGRESS' && (
                    <button
                      type="button"
                      onClick={() => handleUpdateStatus(b.id, 'COMPLETED')}
                      className="w-full py-3.5 bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs rounded-2xl shadow-xs cursor-pointer"
                    >
                      Mark Job as Completed ✓ (Collect ₹{b.total})
                    </button>
                  )}

                  {b.status === 'COMPLETED' && (
                    <div className="p-3 bg-emerald-50 border border-emerald-200 rounded-2xl flex items-center justify-between text-xs text-emerald-800 font-bold">
                      <span>✓ Job Completed & Payment Verified</span>
                      <span className="font-mono">Payout: ₹{b.total}</span>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
};
