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
  Award,
  TrendingUp,
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
      setLoading(true); setError('');
      const [jobs, p, e, conversations, declined] = await Promise.all([getAll('/professionals/bookings', 'bookings'), api('/professionals/profile'), api('/professionals/earnings'), chatApi.list().catch(() => []), api('/professionals/rejected-bookings')]);
      setBookings(jobs); setProfile(p.professional); setEarnings(e.earnings);
      setRejected(declined.requests);
      setUnreadByBooking(Object.fromEntries(conversations.map((c) => [c.booking.id, c.unreadCount])));
    } catch (e) {
      setError(e.message);
    } finally { setLoading(false); }
  };

  useEffect(() => {
    fetchJobs();
  }, []);
  useEffect(() => {
    const offBooking = subscribe('booking:updated', fetchJobs);
    const offConnected = subscribe('connected', fetchJobs);
    const offMessage = subscribe('message:new', fetchJobs);
    const timer = window.setInterval(() => { if (document.visibilityState === 'visible') fetchJobs(); }, 30000);
    return () => { offBooking(); offConnected(); offMessage(); window.clearInterval(timer); };
  }, [subscribe]);

  const respond = async (bookingId: string, action: 'accept' | 'reject') => {
    try {
      await api(`/bookings/${bookingId}/${action}`, { method: 'POST', body: '{}' });
      await fetchJobs();
    } catch (e) { setError(e.message); }
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
      }
    } catch (e) {
      alert(e.message);
    }
  };

  const handleVerifyOtpAndStart = (booking: Booking) => handleUpdateStatus(booking.id, 'IN_PROGRESS', otpInputs[booking.id]);

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
                <span className="text-[10px] font-bold bg-[var(--color-brand)] text-white px-2.5 py-0.5 rounded-full flex items-center gap-1 shadow-sm">
                  <ShieldCheck className="w-3 h-3" />
                  <span>{profile?.verificationStatus || 'Pending verification'}</span>
                </span>
              </div>
              <p className="text-xs text-gray-300 mt-1">
                {profile?.businessName} · {profile?.id}
              </p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <div className="px-3.5 py-2 bg-white/10 rounded-2xl border border-white/10 text-center">
              <span className="text-[10px] uppercase font-bold text-gray-400 block">Status</span>
              <span className="text-xs font-bold text-brand-bright flex items-center justify-center gap-1">
                <span className="w-2 h-2 rounded-full bg-brand-bright animate-pulse" />
                {profile?.isAvailableToday ? 'Accepting assignments' : 'Unavailable'}
              </span>
            </div>
          </div>
        </div>

        <ProfileSettings /><ProfessionalSettings onSaved={fetchJobs} />
        {loading && <p role="status">Loading jobs...</p>}{error && <p role="alert">{error}</p>}
        {!loading && !error && !bookings.length && !rejected.length && <p>No assigned bookings yet.</p>}
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
            <p className="text-[11px] text-brand font-semibold mt-1">Collected payments on completed jobs</p>
          </div>

          <div className="bg-white p-5 rounded-3xl border border-gray-100 shadow-xs">
            <div className="flex items-center justify-between text-gray-400 mb-2">
              <span className="text-xs font-bold uppercase tracking-wider">Partner Rating</span>
              <div className="w-8 h-8 rounded-xl bg-amber-50 text-amber-500 flex items-center justify-center">
                <Star className="w-4 h-4 fill-amber-400" />
              </div>
            </div>
            <div className="text-2xl font-black text-gray-900 font-['Outfit']">{profile?.rating || 0} / 5</div>
            <p className="text-[11px] text-gray-500 font-medium mt-1">{profile?.reviewsCount || 0} verified reviews</p>
          </div>

          <div className="bg-white p-5 rounded-3xl border border-gray-100 shadow-xs">
            <div className="flex items-center justify-between text-gray-400 mb-2">
              <span className="text-xs font-bold uppercase tracking-wider">Jobs Completed</span>
              <div className="w-8 h-8 rounded-xl bg-[var(--color-brand-light)] text-[var(--color-brand)] flex items-center justify-center">
                <CheckCircle2 className="w-4 h-4" />
              </div>
            </div>
            <div className="text-2xl font-black text-gray-900 font-['Outfit']">{profile?.completedJobs || 0}</div>
            <p className="text-[11px] text-gray-500 font-medium mt-1">Completed bookings</p>
          </div>

          <div className="bg-white p-5 rounded-3xl border border-gray-100 shadow-xs">
            <div className="flex items-center justify-between text-gray-400 mb-2">
              <span className="text-xs font-bold uppercase tracking-wider">Assigned Jobs</span>
              <div className="w-8 h-8 rounded-xl bg-brand-soft text-brand flex items-center justify-center">
                <Award className="w-4 h-4" />
              </div>
            </div>
            <div className="text-2xl font-black text-gray-900 font-['Outfit']">{bookings.filter(b => !['COMPLETED', 'CANCELLED'].includes(b.status)).length}</div>
            <p className="text-[11px] text-brand font-semibold mt-1">Current active assignments</p>
          </div>
        </div>

        {/* Live Service Dispatch Queue */}
        <div className="space-y-6">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-lg sm:text-xl font-black text-[var(--color-ink)] font-['Outfit']">
                Booking Requests & Assigned Jobs
              </h2>
              <p className="text-xs text-gray-500">
                Review bookings assigned to your zone, verify customer OTP, and complete servicing.
              </p>
            </div>
            <button
              onClick={fetchJobs}
              className="px-3 py-1.5 rounded-xl border border-gray-200 bg-white hover:bg-gray-50 text-xs font-semibold cursor-pointer"
            >
              Refresh Feed
            </button>
          </div>

          <div className="space-y-4">
            <div className="flex flex-wrap gap-2" aria-label="Booking status filter">
              {[
                ['ALL', 'All'], ['ASSIGNED', 'New requests'], ['CONFIRMED', 'Accepted'],
                ['ON_THE_WAY', 'On the way'], ['ARRIVED', 'Arrived'], ['IN_PROGRESS', 'In progress'],
                ['COMPLETED', 'Completed'], ['CANCELLED', 'Cancelled'], ['REJECTED', 'Rejected'],
              ].map(([value, label]) => (
                <button key={value} onClick={() => setJobFilter(value)}
                  className={`rounded-xl px-3 py-1.5 text-xs font-semibold ${jobFilter === value ? 'bg-brand text-white' : 'bg-brand-light text-brand-dark'}`}>
                  {label} ({value === 'REJECTED' ? rejected.length : value === 'ALL' ? bookings.length : bookings.filter(b => b.status === value).length})
                </button>
              ))}
            </div>
            {jobFilter === 'REJECTED' && rejected.map((r) => (
              <div key={`${r.bookingId}-${r.rejectedAt}`} className="rounded-2xl border border-line bg-white p-4 text-sm">
                <strong>Rejected request · {r.bookingId}</strong>
                <p>{r.serviceName} · {r.scheduledDate} at {r.scheduledTimeSlot}</p>
                <p className="text-gray-500">This request is back in the admin dispatch queue. Chat access has ended.</p>
              </div>
            ))}
            {jobFilter !== 'REJECTED' && bookings.filter(b => jobFilter === 'ALL' || b.status === jobFilter).map((b) => (
              <div
                key={b.id}
                className="bg-white rounded-3xl p-6 border border-gray-100 shadow-xs hover:border-[var(--color-brand-bright)] transition-all space-y-4"
              >
                <div className="flex flex-wrap items-center justify-between gap-3 pb-3 border-b border-gray-100">
                  <div className="flex items-center gap-2.5">
                    <span className="font-extrabold text-sm text-gray-900 font-['Outfit']">{b.id}</span>
                    <span
                      className={`px-2.5 py-0.5 rounded-full text-[10px] font-bold ${
                        b.status === 'COMPLETED'
                          ? 'bg-brand-light text-brand-dark'
                          : b.status === 'IN_PROGRESS'
                          ? 'bg-[var(--color-brand-light)] text-[var(--color-brand-hover)] border border-[var(--color-brand-bright)]/40 animate-pulse'
                          : 'bg-brand-light text-brand-dark'
                      }`}
                    >
                      {b.status}
                    </span>
                  </div>

                  <div className="text-right">
                    <span className="text-[10px] text-gray-400 font-bold block uppercase">Payout</span>
                    <span className="text-base font-black text-brand">
                      ₹{b.total} <span className="text-xs font-normal text-gray-400">(booking value)</span>
                    </span>
                  </div>
                </div>

                {/* Customer Info & Slot */}
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-xs">
                  <div className="p-3 bg-gray-50 rounded-2xl">
                    <span className="text-gray-400 font-bold uppercase text-[10px] block mb-1">
                      Customer
                    </span>
                    <span className="font-bold text-gray-900 block font-['Outfit']">{b.userName}</span>
                    <span className="text-gray-500">Contact through in-app chat</span>
                  </div>

                  <div className="p-3 bg-gray-50 rounded-2xl">
                    <span className="text-gray-400 font-bold uppercase text-[10px] block mb-1">
                      Scheduled Time
                    </span>
                    <span className="font-bold text-gray-900 block">{b.scheduledDate}</span>
                    <span className="text-[var(--color-brand-hover)] font-medium">{b.scheduledTimeSlot}</span>
                  </div>

                  <div className="p-3 bg-gray-50 rounded-2xl">
                    <span className="text-gray-400 font-bold uppercase text-[10px] block mb-1">
                      Address
                    </span>
                    <span className="font-semibold text-gray-800 block truncate">
                      {b.address.house}, {b.address.area}
                    </span>
                    <span className="text-gray-400">{b.address.city}</span>
                  </div>
                </div>

                {/* Service items */}
                <div className="text-xs text-gray-600">
                  <span className="font-bold text-gray-800 block mb-1">Tasks to perform:</span>
                  <div className="flex flex-wrap gap-2">
                    {b.items && b.items.length > 0 ? (
                      b.items.map((item, idx) => (
                        <span
                          key={idx}
                          className="px-2.5 py-1 rounded-lg bg-[var(--color-brand-light)] text-[var(--color-brand-hover)] font-semibold text-[11px]"
                        >
                          {item.service?.name} ({item.variant?.name}) × {item.quantity}
                        </span>
                      ))
                    ) : (
                      <span className="px-2.5 py-1 rounded-lg bg-[var(--color-brand-light)] text-[var(--color-brand-hover)] font-semibold text-[11px]">
                        {b.serviceName} ({b.variantName})
                      </span>
                    )}
                  </div>
                </div>

                {b.status === 'ASSIGNED' && <div className="flex gap-3"><button onClick={() => respond(b.id, 'accept')} className="bg-brand hover:bg-brand-hover text-white rounded-xl p-2">Accept booking request</button><button onClick={() => respond(b.id, 'reject')} className="border rounded-xl p-2">Reject request</button></div>}
                {b.status === 'CONFIRMED' && <button onClick={() => handleUpdateStatus(b.id, 'ON_THE_WAY')}>On my way</button>}
                {b.status === 'ON_THE_WAY' && <button onClick={() => handleUpdateStatus(b.id, 'ARRIVED')}>I have arrived</button>}
                {/* Partner Actions & OTP Verification */}
                <div className="pt-3 border-t border-gray-100 flex flex-wrap items-center justify-between gap-4">
                  {b.status === 'ARRIVED' && (
                    <div className="flex flex-wrap items-center gap-3">
                      <div className="flex items-center gap-2">
                        <input
                          type="text"
                          maxLength={4}
                          placeholder="Enter Customer OTP"
                          value={otpInputs[b.id] || ''}
                          onChange={(e) =>
                            setOtpInputs((prev) => ({ ...prev, [b.id]: e.target.value }))
                          }
                          className="w-36 px-3 py-2 bg-gray-50 border border-gray-200 rounded-xl text-xs font-mono font-bold text-center focus:outline-none focus:ring-1 focus:ring-[var(--color-brand)]"
                        />
                        <button
                          onClick={() => handleVerifyOtpAndStart(b)}
                          className="px-4 py-2 bg-[var(--color-brand)] hover:bg-[var(--color-brand-hover)] text-white font-bold text-xs rounded-xl shadow-xs cursor-pointer shadow-brand/20"
                        >
                          Verify & Start Job
                        </button>
                      </div>

                      {otpError[b.id] && (
                        <span className="text-[11px] text-red-600 font-semibold">{otpError[b.id]}</span>
                      )}

                    </div>
                  )}

                  {b.status === 'IN_PROGRESS' && (
                    <div className="flex items-center gap-3">
                      <span className="text-xs font-bold text-[var(--color-brand-hover)] flex items-center gap-1.5">
                        <Clock className="w-4 h-4 text-[var(--color-brand)] animate-spin" />
                        <span>Job in progress...</span>
                      </span>
                      <button
                        onClick={() => handleUpdateStatus(b.id, 'COMPLETED')}
                        className="px-5 py-2 bg-brand hover:bg-brand-hover text-white font-bold text-xs rounded-xl shadow-xs cursor-pointer"
                      >
                        Mark Job as Completed ✓
                      </button>
                    </div>
                  )}

                  {b.status === 'COMPLETED' && (
                    <div className="flex items-center gap-1 text-xs font-bold text-brand-hover bg-brand-soft px-3 py-1 rounded-xl">
                      <CheckCircle2 className="w-4 h-4 text-brand" />
                      <span>Service completed · Payment: {b.paymentStatus}</span>
                    </div>
                  )}

                  <button
                    type="button"
                    disabled={['COMPLETED', 'CANCELLED'].includes(b.status)}
                    onClick={() => onNavigate?.(`/messages?booking=${b.id}`)}
                    className="ml-auto px-3.5 py-2 rounded-xl border border-gray-200 hover:bg-gray-50 text-xs font-semibold flex items-center gap-1.5 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    <MessageSquare className="w-3.5 h-3.5 text-[var(--color-brand)]" />
                    <span>Chat with Customer</span>
                    {!!unreadByBooking[b.id] && <span className="rounded-full bg-brand px-1.5 text-white">{unreadByBooking[b.id]}</span>}
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};
