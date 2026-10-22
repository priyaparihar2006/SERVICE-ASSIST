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
import { isBookingWithin15Days, get15DayRetentionInfo } from '../utils/bookingHistory';
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
  Bell,
  BellRing,
  Volume2,
  VolumeX,
  X,
  Calendar,
  AlertTriangle,
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
  const [jobFilter, setJobFilter] = useState<'ALL' | 'ACTIVE' | '15_DAYS_HISTORY' | 'COMPLETED'>('ALL');
  const [otpInputs, setOtpInputs] = useState<Record<string, string>>({});
  const [otpError, setOtpError] = useState<Record<string, string>>({});

  // Rider Arrival Time State (Requirement 2)
  const [arrivalEta, setArrivalEta] = useState<Record<string, string>>(() => {
    try {
      const saved = localStorage.getItem('rider_arrival_etas');
      return saved ? JSON.parse(saved) : {};
    } catch {
      return {};
    }
  });
  const [editingEtaId, setEditingEtaId] = useState<string | null>(null);
  const [customEtaInput, setCustomEtaInput] = useState('');

  // Serviceman Phone Screen Notification / Popup State (Requirement 1)
  const [incomingAlertBooking, setIncomingAlertBooking] = useState<Booking | null>(null);
  const [soundEnabled, setSoundEnabled] = useState(true);
  const [phoneNotificationToast, setPhoneNotificationToast] = useState<string | null>(null);

  // Play synthetic pleasant alert chime for incoming order on phone
  const playAlertChime = () => {
    if (!soundEnabled) return;
    try {
      const audioCtx = new (window.AudioContext || (window as any).webkitAudioContext)();
      const osc = audioCtx.createOscillator();
      const gain = audioCtx.createGain();
      osc.type = 'sine';
      osc.frequency.setValueAtTime(587.33, audioCtx.currentTime); // D5
      osc.frequency.setValueAtTime(880, audioCtx.currentTime + 0.15); // A5
      gain.gain.setValueAtTime(0.3, audioCtx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.01, audioCtx.currentTime + 0.4);
      osc.connect(gain);
      gain.connect(audioCtx.destination);
      osc.start();
      osc.stop(audioCtx.currentTime + 0.45);
    } catch {
      // AudioContext unavailable or blocked
    }
  };

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

      // Auto-trigger phone screen popup if there is a pending assignment
      const pendingAssignment = jobs.find((j) => j.status === 'ASSIGNED');
      if (pendingAssignment && !incomingAlertBooking) {
        setIncomingAlertBooking(pendingAssignment);
        setPhoneNotificationToast(`🚨 New Job #${pendingAssignment.id?.slice(0, 8)} assigned! Tap to view.`);
        playAlertChime();
      }
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
    const offBooking = subscribe('booking:updated', (payload?: any) => {
      fetchJobs();
      if (payload?.booking) {
        setIncomingAlertBooking(payload.booking);
        setPhoneNotificationToast(`⚡ Order update: #${payload.booking.id?.slice(0, 8)}`);
        playAlertChime();
      }
    });
    const offConnected = subscribe('connected', fetchJobs);
    const offMessage = subscribe('message:new', (msg?: any) => {
      fetchJobs();
      setPhoneNotificationToast('💬 New message from customer');
    });
    const timer = window.setInterval(() => {
      if (document.visibilityState === 'visible') fetchJobs();
    }, 30000);
    return () => {
      offBooking();
      offConnected();
      offMessage();
      window.clearInterval(timer);
    };
  }, [subscribe, soundEnabled]);

  const respond = async (bookingId: string, action: 'accept' | 'reject') => {
    try {
      await api(`/bookings/${bookingId}/${action}`, { method: 'POST', body: '{}' });
      if (incomingAlertBooking?.id === bookingId) {
        setIncomingAlertBooking(null);
      }
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

  // Set & Save Rider Arrival ETA (Requirement 2)
  const handleSaveRiderEta = (bookingId: string, etaString: string) => {
    const updated = { ...arrivalEta, [bookingId]: etaString };
    setArrivalEta(updated);
    localStorage.setItem('rider_arrival_etas', JSON.stringify(updated));
    setEditingEtaId(null);
    setPhoneNotificationToast(`📍 Arrival time updated to ${etaString}`);
    setTimeout(() => setPhoneNotificationToast(null), 3000);
  };

  // Helper for quick arrival minute calculation
  const getEtaTimeFromMinutes = (minutes: number) => {
    const d = new Date();
    d.setMinutes(d.getMinutes() + minutes);
    const timeStr = d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    return `${minutes} mins (${timeStr})`;
  };

  // Filter bookings based on 15-day history option (Requirement 6)
  const filteredBookings = bookings.filter((b) => {
    if (jobFilter === '15_DAYS_HISTORY') {
      return isBookingWithin15Days(b);
    }
    if (jobFilter === 'ACTIVE') {
      return ['PENDING', 'ASSIGNED', 'CONFIRMED', 'ON_THE_WAY', 'ARRIVED', 'IN_PROGRESS'].includes(b.status);
    }
    if (jobFilter === 'COMPLETED') {
      return b.status === 'COMPLETED';
    }
    return true;
  });

  const purgedOlderCount = bookings.filter((b) => !isBookingWithin15Days(b)).length;

  return (
    <div className="min-h-screen bg-[var(--color-brand-soft)]/30 py-6 sm:py-12">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Mobile Phone Screen Notification Toast Bar (Requirement 1) */}
        {phoneNotificationToast && (
          <div className="fixed top-4 left-4 right-4 sm:left-auto sm:right-6 sm:w-96 z-50 bg-gray-900 text-white p-3.5 rounded-2xl shadow-2xl border border-emerald-500/30 flex items-center justify-between gap-3 animate-in slide-in-from-top duration-200">
            <div className="flex items-center gap-2.5 min-w-0">
              <span className="w-2.5 h-2.5 rounded-full bg-emerald-400 animate-ping shrink-0" />
              <p className="text-xs font-bold truncate">{phoneNotificationToast}</p>
            </div>
            <button
              onClick={() => setPhoneNotificationToast(null)}
              className="text-gray-400 hover:text-white p-1 cursor-pointer"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        )}

        {/* INCOMING JOB ALERT POPUP FOR SERVICEMAN PHONE SCREEN (Requirement 1) */}
        {incomingAlertBooking && (
          <div
            role="dialog"
            aria-modal="true"
            className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-black/75 backdrop-blur-xs animate-in fade-in duration-200"
          >
            <div className="relative w-full max-w-md bg-white rounded-3xl shadow-2xl border-2 border-emerald-500 overflow-hidden flex flex-col animate-in zoom-in-95 duration-200">
              {/* Animated Glowing Top Bar */}
              <div className="bg-gradient-to-r from-emerald-600 to-teal-700 text-white p-4.5 flex items-center justify-between">
                <div className="flex items-center gap-2.5">
                  <div className="w-9 h-9 rounded-xl bg-white/20 text-white flex items-center justify-center animate-bounce">
                    <BellRing className="w-5 h-5 text-amber-300" />
                  </div>
                  <div>
                    <span className="text-[10px] font-black uppercase tracking-wider text-emerald-200 block">
                      NEW DOORSTEP ASSIGNMENT
                    </span>
                    <h3 className="font-black text-base sm:text-lg font-['Outfit']">
                      Incoming Booking Request!
                    </h3>
                  </div>
                </div>

                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => setSoundEnabled(!soundEnabled)}
                    className="p-1.5 rounded-lg bg-white/10 hover:bg-white/20 text-white text-xs cursor-pointer"
                    title={soundEnabled ? 'Mute Chime' : 'Unmute Chime'}
                  >
                    {soundEnabled ? <Volume2 className="w-4 h-4" /> : <VolumeX className="w-4 h-4" />}
                  </button>
                  <button
                    type="button"
                    onClick={() => setIncomingAlertBooking(null)}
                    className="p-1.5 rounded-lg bg-white/10 hover:bg-white/20 text-white cursor-pointer"
                  >
                    <X className="w-4 h-4" />
                  </button>
                </div>
              </div>

              {/* Popup Body */}
              <div className="p-5 space-y-4 text-xs">
                {/* Payout & Job ID Badge */}
                <div className="flex items-center justify-between p-3 bg-emerald-50 rounded-2xl border border-emerald-200">
                  <div>
                    <span className="text-[10px] font-bold text-gray-500 uppercase block">Your Payout</span>
                    <span className="text-xl font-black text-emerald-800 font-['Outfit']">
                      ₹{incomingAlertBooking.total || 3673}
                    </span>
                  </div>
                  <div className="text-right">
                    <span className="text-[10px] font-bold text-gray-500 uppercase block">Order ID</span>
                    <span className="font-mono font-bold text-xs text-gray-800">
                      #{incomingAlertBooking.id?.slice(0, 10)}
                    </span>
                  </div>
                </div>

                {/* Service Details */}
                <div className="space-y-1.5">
                  <span className="text-[10px] font-black uppercase tracking-wider text-gray-400 block">
                    SERVICE REQUESTED
                  </span>
                  <h4 className="font-extrabold text-sm text-gray-900">
                    {incomingAlertBooking.serviceName || 'Move-in / Deep Cleaning'}
                  </h4>
                  <div className="flex items-center gap-2 text-gray-600 font-medium">
                    <Clock className="w-3.5 h-3.5 text-emerald-600" />
                    <span>
                      {incomingAlertBooking.scheduledDate || 'Today'} • {incomingAlertBooking.scheduledTimeSlot || '02:00 PM'}
                    </span>
                  </div>
                </div>

                {/* Customer & Address */}
                <div className="p-3 bg-gray-50 rounded-2xl border border-gray-100 space-y-1">
                  <span className="text-[10px] font-bold text-gray-400 uppercase block">Customer Locality</span>
                  <p className="font-bold text-gray-900">
                    {incomingAlertBooking.userName || 'Priya Sharma'} • {incomingAlertBooking.userPhone || '+91 98765 43210'}
                  </p>
                  <p className="text-[11px] text-gray-600 leading-relaxed">
                    {typeof incomingAlertBooking.address === 'object' && incomingAlertBooking.address?.house
                      ? `${incomingAlertBooking.address.house}, ${incomingAlertBooking.address.area}, ${incomingAlertBooking.address.city}`
                      : 'Flat 402, Royal Residency, Taj Nagri Phase 2, Agra'}
                  </p>
                </div>

                {/* Action Buttons */}
                <div className="pt-2 flex gap-3">
                  <button
                    type="button"
                    onClick={() => {
                      respond(incomingAlertBooking.id, 'accept');
                      setIncomingAlertBooking(null);
                      setEditingEtaId(incomingAlertBooking.id);
                    }}
                    className="flex-1 py-3.5 bg-emerald-600 hover:bg-emerald-700 text-white font-black text-xs rounded-2xl shadow-md shadow-emerald-600/30 flex items-center justify-center gap-1.5 cursor-pointer"
                  >
                    <Check className="w-4 h-4 stroke-[3]" />
                    <span>ACCEPT JOB REQUEST</span>
                  </button>

                  <button
                    type="button"
                    onClick={() => {
                      respond(incomingAlertBooking.id, 'reject');
                      setIncomingAlertBooking(null);
                    }}
                    className="px-4 py-3.5 border border-gray-200 text-gray-600 hover:bg-gray-100 font-bold text-xs rounded-2xl cursor-pointer"
                  >
                    Decline
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}

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

          <div className="flex flex-wrap items-center gap-3">
            {/* Quick Test Alert Popup Button for Phone Screen (Requirement 1) */}
            <button
              type="button"
              onClick={() => {
                const demoJob = bookings[0] || {
                  id: 'SRV-' + Math.floor(10000 + Math.random() * 90000),
                  serviceName: 'AC Jet Cleaning & Gas Refill',
                  total: 2499,
                  scheduledDate: 'Today',
                  scheduledTimeSlot: '03:00 PM',
                  userName: 'Sneha Patel',
                  userPhone: '+91 98765 43210',
                  status: 'ASSIGNED',
                  address: { house: 'Flat 301, Emerald Heights', area: 'Civil Lines', city: 'Agra' },
                };
                setIncomingAlertBooking(demoJob as any);
                playAlertChime();
              }}
              className="px-3.5 py-2 bg-emerald-500/20 hover:bg-emerald-500/30 border border-emerald-400/40 rounded-2xl text-xs font-bold text-emerald-300 flex items-center gap-1.5 transition-colors cursor-pointer"
              title="Test Incoming Notification on phone screen"
            >
              <BellRing className="w-3.5 h-3.5" />
              <span>Test Phone Alert Popup</span>
            </button>

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

        {/* Assigned Job Cards Section with 15-Day Booking History Option (Requirement 6) */}
        <div className="space-y-6">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div>
              <h2 className="text-xl font-black text-gray-900 font-['Outfit']">
                Live Job Management
              </h2>
              <p className="text-xs text-gray-500">
                Track assigned doorstep orders, update rider arrival time, and verify customer OTP.
              </p>
            </div>

            {/* Filter Pills with 15-Day Booking History Option */}
            <div className="flex items-center gap-1.5 overflow-x-auto pb-1 sm:pb-0">
              <button
                type="button"
                onClick={() => setJobFilter('ALL')}
                className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all cursor-pointer ${
                  jobFilter === 'ALL'
                    ? 'bg-emerald-600 text-white shadow-xs'
                    : 'bg-white text-gray-600 border border-gray-200 hover:bg-gray-50'
                }`}
              >
                All ({bookings.length})
              </button>
              <button
                type="button"
                onClick={() => setJobFilter('ACTIVE')}
                className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all cursor-pointer ${
                  jobFilter === 'ACTIVE'
                    ? 'bg-emerald-600 text-white shadow-xs'
                    : 'bg-white text-gray-600 border border-gray-200 hover:bg-gray-50'
                }`}
              >
                Active Orders
              </button>
              <button
                type="button"
                onClick={() => setJobFilter('15_DAYS_HISTORY')}
                className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all cursor-pointer flex items-center gap-1.5 ${
                  jobFilter === '15_DAYS_HISTORY'
                    ? 'bg-emerald-600 text-white shadow-xs ring-2 ring-emerald-200'
                    : 'bg-white text-emerald-800 border border-emerald-200 hover:bg-emerald-50'
                }`}
                title="Only save & display bookings within last 15 days"
              >
                <Calendar className="w-3.5 h-3.5" />
                <span>Last 15 Days History</span>
              </button>
              <button
                type="button"
                onClick={() => setJobFilter('COMPLETED')}
                className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all cursor-pointer ${
                  jobFilter === 'COMPLETED'
                    ? 'bg-emerald-600 text-white shadow-xs'
                    : 'bg-white text-gray-600 border border-gray-200 hover:bg-gray-50'
                }`}
              >
                Completed
              </button>
            </div>
          </div>

          {/* 15-Day History Information Banner */}
          {jobFilter === '15_DAYS_HISTORY' && (
            <div className="p-3.5 bg-emerald-50 border border-emerald-200 rounded-2xl flex items-center justify-between text-xs text-emerald-900 animate-in fade-in">
              <div className="flex items-center gap-2">
                <ShieldCheck className="w-4 h-4 text-emerald-600 shrink-0" />
                <span>
                  <strong>15-Day Record Window Active:</strong> Showing only assignments from the last 15 days. Older records ({purgedOlderCount} items) are automatically purged.
                </span>
              </div>
              <span className="font-mono font-bold bg-white px-2 py-0.5 rounded-md border border-emerald-200 text-[11px]">
                {filteredBookings.length} retained
              </span>
            </div>
          )}

          {loading && <p className="text-xs text-gray-500">Loading assignments...</p>}
          {error && <p className="text-xs text-red-600">{error}</p>}
          {!loading && !filteredBookings.length && !rejected.length && (
            <div className="bg-white rounded-3xl p-12 text-center border border-gray-100 max-w-md mx-auto">
              <Clock className="w-12 h-12 text-gray-300 mx-auto mb-3" />
              <h3 className="font-bold text-gray-900 text-base mb-1">No Orders in this View</h3>
              <p className="text-xs text-gray-500">
                New customer orders in your locality will appear here automatically.
              </p>
            </div>
          )}

          <div className="space-y-5">
            {filteredBookings.map((b) => {
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

              const retention = get15DayRetentionInfo(b);
              const currentEta = arrivalEta[b.id] || b.estimatedArrival;

              return (
                <div
                  key={b.id}
                  className="bg-white rounded-3xl border border-gray-100 shadow-sm overflow-hidden p-5 sm:p-6 space-y-4 max-w-2xl"
                >
                  {/* Top Bar with ID, Status & 15-Day Retention Tag */}
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

                      {/* 15-day history tag */}
                      <span className="text-[10px] text-gray-500 font-semibold bg-gray-50 px-2 py-0.5 rounded-lg border border-gray-200">
                        {retention.daysAgo === 0 ? 'Today' : `${retention.daysAgo}d ago`} • {retention.daysRemaining}d left
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
                  </div>

                  {/* RIDER ARRIVAL TIME (ETA TO LOCALITY) COMPONENT (Requirement 2) */}
                  <div className="p-4 bg-emerald-50/70 border border-emerald-200 rounded-2xl space-y-2.5">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2 text-emerald-900 font-bold text-xs">
                        <Navigation className="w-4 h-4 text-emerald-600 animate-pulse" />
                        <span>Rider Arrival Time (Reaching Customer Locality):</span>
                      </div>
                      <button
                        type="button"
                        onClick={() => setEditingEtaId(editingEtaId === b.id ? null : b.id)}
                        className="text-[11px] font-extrabold text-emerald-700 hover:underline cursor-pointer"
                      >
                        {editingEtaId === b.id ? 'Done' : 'Update Arrival Time'}
                      </button>
                    </div>

                    <div className="flex items-center justify-between gap-2">
                      <span className="font-extrabold text-sm text-gray-900 font-['Outfit']">
                        {currentEta || 'Estimated: ~15 mins (02:45 PM)'}
                      </span>
                      <span className="px-2 py-0.5 rounded-md bg-white border border-emerald-200 text-[10px] font-bold text-emerald-800">
                        Visible to customer
                      </span>
                    </div>

                    {/* ETA Quick Selector Pills */}
                    {editingEtaId === b.id && (
                      <div className="pt-2 border-t border-emerald-200/60 space-y-2 animate-in fade-in">
                        <span className="text-[10px] font-bold text-gray-500 uppercase block">
                          Set Expected Arrival Time to Locality:
                        </span>
                        <div className="flex flex-wrap gap-1.5">
                          {[10, 15, 20, 30].map((mins) => (
                            <button
                              key={mins}
                              type="button"
                              onClick={() => handleSaveRiderEta(b.id, getEtaTimeFromMinutes(mins))}
                              className="px-3 py-1 bg-white hover:bg-emerald-600 hover:text-white border border-emerald-300 text-emerald-900 text-xs font-bold rounded-xl transition-colors cursor-pointer"
                            >
                              +{mins} Mins
                            </button>
                          ))}
                        </div>

                        <div className="flex items-center gap-2 pt-1">
                          <input
                            type="text"
                            placeholder="e.g. 02:45 PM"
                            value={customEtaInput}
                            onChange={(e) => setCustomEtaInput(e.target.value)}
                            className="w-36 px-3 py-1.5 bg-white border border-gray-200 rounded-xl text-xs font-bold"
                          />
                          <button
                            type="button"
                            onClick={() => {
                              if (customEtaInput.trim()) {
                                handleSaveRiderEta(b.id, customEtaInput.trim());
                                setCustomEtaInput('');
                              }
                            }}
                            className="px-3 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs rounded-xl shadow-xs cursor-pointer"
                          >
                            Set Custom
                          </button>
                        </div>
                      </div>
                    )}
                  </div>

                  {/* Horizontal 5-Step Tracker Bar */}
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
                      onClick={() => {
                        handleUpdateStatus(b.id, 'ON_THE_WAY');
                        setEditingEtaId(b.id);
                      }}
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
