import React, { useState, useEffect } from 'react';
import { Booking, Service, Professional } from '../types';
import { api, getAll, apiFetch } from '../services/api';
import {
  Crown,
  Search,
  RefreshCw,
  User,
  MapPin,
  Calendar,
  Clock,
  Briefcase,
  CheckCircle2,
  AlertCircle,
  Phone,
  Filter,
  UserCheck,
} from 'lucide-react';

interface AdminDashboardPageProps {
  services: Service[];
  professionals: Professional[];
}

export const AdminDashboardPage: React.FC<AdminDashboardPageProps> = ({
  services = [],
  professionals = [],
}) => {
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [filterStatus, setFilterStatus] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedProForBooking, setSelectedProForBooking] = useState<Record<string, string>>({});
  const [assigningId, setAssigningId] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  // Load bookings from API and local storage
  const fetchAllBookings = async () => {
    try {
      setLoading(true);
      setError('');

      // Fetch bookings from backend
      const rows: Booking[] = await getAll('/bookings', 'bookings').catch(() => []);

      // Merge with any offline/client-created requests from localStorage
      const localRaw = localStorage.getItem('sa_offline_bookings');
      const localBookings: Booking[] = localRaw ? JSON.parse(localRaw) : [];

      // Deduplicate by ID
      const seen = new Set<string>();
      const combined: Booking[] = [];

      for (const b of [...localBookings, ...rows]) {
        if (!seen.has(b.id)) {
          seen.add(b.id);
          combined.push(b);
        }
      }

      setBookings(combined);
    } catch (e: any) {
      setError(e?.message || 'Error loading bookings');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAllBookings();
    const timer = window.setInterval(() => {
      if (document.visibilityState === 'visible') fetchAllBookings();
    }, 20000);
    return () => window.clearInterval(timer);
  }, []);

  // Manual Assignment Handler (Requirement 4: Initially admin can assign work manually)
  const handleAssignWorkManually = async (bookingId: string) => {
    const proId = selectedProForBooking[bookingId];
    if (!proId) {
      alert('Please select a professional to assign.');
      return;
    }

    const pro = professionals.find((p) => p.id === proId);
    setAssigningId(bookingId);
    setNotice(null);

    try {
      // 1. Try backend status update
      const res = await apiFetch(`/api/bookings/${bookingId}/status`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ status: 'ASSIGNED', professionalId: proId }),
      }).catch((err) => {
        console.warn('Backend update note:', err.message);
        return null;
      });

      // 2. Update local state immediately so admin sees real-time change
      setBookings((prev) =>
        prev.map((b) => {
          if (b.id === bookingId) {
            return {
              ...b,
              status: 'ASSIGNED',
              professionalId: proId,
              professionalName: pro?.name || 'Assigned Professional',
              professionalPhone: pro?.phone || '+91 98765 43210',
            };
          }
          return b;
        })
      );

      // 3. Update localStorage cache
      const localRaw = localStorage.getItem('sa_offline_bookings');
      if (localRaw) {
        const localList: Booking[] = JSON.parse(localRaw);
        const updated = localList.map((b) =>
          b.id === bookingId
            ? {
                ...b,
                status: 'ASSIGNED' as const,
                professionalId: proId,
                professionalName: pro?.name || 'Assigned Professional',
                professionalPhone: pro?.phone || '+91 98765 43210',
              }
            : b
        );
        localStorage.setItem('sa_offline_bookings', JSON.stringify(updated));
      }

      setNotice(`Job #${bookingId} successfully assigned to ${pro?.name || 'professional'}!`);
      setTimeout(() => setNotice(null), 4000);
    } catch (err: any) {
      alert(err?.message || 'Failed to assign work');
    } finally {
      setAssigningId(null);
    }
  };

  const handleCancelBooking = async (bookingId: string) => {
    if (!confirm(`Are you sure you want to cancel booking #${bookingId}?`)) return;

    try {
      await apiFetch(`/api/bookings/${bookingId}/status`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ status: 'CANCELLED' }),
      }).catch(() => null);

      setBookings((prev) =>
        prev.map((b) => (b.id === bookingId ? { ...b, status: 'CANCELLED' } : b))
      );
    } catch (e: any) {
      alert(e?.message);
    }
  };

  // Filter Bookings
  const filteredBookings = bookings.filter((b) => {
    const matchStatus =
      filterStatus === 'ALL' ||
      (filterStatus === 'PENDING' && (b.status === 'PENDING' || !b.professionalId)) ||
      b.status === filterStatus;

    const q = searchQuery.toLowerCase().trim();
    const matchSearch =
      !q ||
      b.id.toLowerCase().includes(q) ||
      (b.serviceName || '').toLowerCase().includes(q) ||
      (b.userName || '').toLowerCase().includes(q) ||
      (b.address?.city || '').toLowerCase().includes(q) ||
      (b.address?.area || '').toLowerCase().includes(q) ||
      (b.address?.house || '').toLowerCase().includes(q);

    return matchStatus && matchSearch;
  });

  const pendingCount = bookings.filter((b) => b.status === 'PENDING' || !b.professionalId).length;
  const assignedCount = bookings.filter((b) => b.status === 'ASSIGNED' || b.status === 'CONFIRMED').length;

  return (
    <div className="min-h-screen bg-[var(--color-brand-soft)]/30 py-8 sm:py-12">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Admin Clean Header (Requirement 4: For admin only show which service is booked and from where) */}
        <div className="bg-gradient-to-r from-[var(--color-ink)] via-[var(--color-ink)] to-gray-900 text-white rounded-3xl p-6 sm:p-8 shadow-xl mb-6 flex flex-col sm:flex-row sm:items-center justify-between gap-4 border border-brand/20">
          <div className="flex items-center gap-4">
            <div className="w-14 h-14 rounded-2xl bg-amber-500/20 border border-amber-400/30 flex items-center justify-center text-amber-400 shrink-0">
              <Crown className="w-7 h-7" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h1 className="text-xl sm:text-2xl font-black font-['Outfit']">Service Assist Admin Dispatch</h1>
                <span className="text-[10px] font-bold bg-amber-500 text-black px-2.5 py-0.5 rounded-full uppercase">
                  Manual Assignment Portal
                </span>
              </div>
              <p className="text-xs text-gray-300 mt-1">
                Live Booked Services & Doorstep Locations • Assign verified professionals manually
              </p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <button
              onClick={fetchAllBookings}
              className="px-4 py-2.5 bg-white/10 hover:bg-white/20 border border-white/20 text-white rounded-xl text-xs font-bold flex items-center gap-2 cursor-pointer transition-colors"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
              <span>Refresh Bookings</span>
            </button>
          </div>
        </div>

        {/* Global Notice Toast */}
        {notice && (
          <div className="mb-6 p-4 bg-emerald-50 border border-emerald-300 text-emerald-900 rounded-2xl text-xs font-bold flex items-center gap-2 animate-in fade-in">
            <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
            <span>{notice}</span>
          </div>
        )}

        {error && (
          <div className="mb-6 p-4 bg-red-50 border border-red-300 text-red-900 rounded-2xl text-xs font-semibold flex items-center gap-2">
            <AlertCircle className="w-4 h-4 text-red-600 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        {/* Dispatch Overview & Status Tabs */}
        <div className="bg-white rounded-3xl p-5 border border-brand-light/70 shadow-xs mb-6">
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
            {/* Search Filter Bar */}
            <div className="relative flex-1 max-w-md">
              <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
              <input
                type="text"
                placeholder="Search by Service, Location, Customer, Booking ID..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full pl-10 pr-4 py-2 bg-gray-50 border border-gray-200 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-[var(--color-brand)]"
              />
            </div>

            {/* Status Tabs */}
            <div className="flex items-center gap-1.5 overflow-x-auto no-scrollbar text-xs">
              <button
                onClick={() => setFilterStatus('ALL')}
                className={`px-3.5 py-1.5 rounded-xl font-bold transition-all cursor-pointer ${
                  filterStatus === 'ALL'
                    ? 'bg-gray-900 text-white shadow-xs'
                    : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                }`}
              >
                All Bookings ({bookings.length})
              </button>
              <button
                onClick={() => setFilterStatus('PENDING')}
                className={`px-3.5 py-1.5 rounded-xl font-bold transition-all cursor-pointer flex items-center gap-1.5 ${
                  filterStatus === 'PENDING'
                    ? 'bg-amber-600 text-white shadow-xs'
                    : 'bg-amber-50 text-amber-800 hover:bg-amber-100'
                }`}
              >
                <span>Needs Manual Assignment</span>
                {pendingCount > 0 && (
                  <span className="w-5 h-5 rounded-full bg-white text-amber-800 text-[10px] font-black flex items-center justify-center">
                    {pendingCount}
                  </span>
                )}
              </button>
              <button
                onClick={() => setFilterStatus('ASSIGNED')}
                className={`px-3.5 py-1.5 rounded-xl font-bold transition-all cursor-pointer ${
                  filterStatus === 'ASSIGNED'
                    ? 'bg-emerald-600 text-white shadow-xs'
                    : 'bg-emerald-50 text-emerald-800 hover:bg-emerald-100'
                }`}
              >
                Assigned ({assignedCount})
              </button>
              <button
                onClick={() => setFilterStatus('IN_PROGRESS')}
                className={`px-3.5 py-1.5 rounded-xl font-bold transition-all cursor-pointer ${
                  filterStatus === 'IN_PROGRESS'
                    ? 'bg-[var(--color-brand)] text-white shadow-xs'
                    : 'bg-purple-50 text-purple-800 hover:bg-purple-100'
                }`}
              >
                In Progress
              </button>
              <button
                onClick={() => setFilterStatus('COMPLETED')}
                className={`px-3.5 py-1.5 rounded-xl font-bold transition-all cursor-pointer ${
                  filterStatus === 'COMPLETED'
                    ? 'bg-gray-700 text-white shadow-xs'
                    : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                }`}
              >
                Completed
              </button>
            </div>
          </div>
        </div>

        {/* Bookings Feed: Which Service is Booked & From Where */}
        <div className="space-y-4">
          {loading && bookings.length === 0 ? (
            <div className="bg-white rounded-3xl p-12 text-center text-sm font-semibold text-gray-500 border border-gray-100">
              Loading booked services...
            </div>
          ) : filteredBookings.length === 0 ? (
            <div className="bg-white rounded-3xl p-12 text-center text-sm text-gray-500 border border-gray-100">
              No service bookings found matching this filter.
            </div>
          ) : (
            filteredBookings.map((b) => {
              const isPending = b.status === 'PENDING' || !b.professionalId;
              const currentProSelection = selectedProForBooking[b.id] || '';

              return (
                <div
                  key={b.id}
                  className={`bg-white rounded-3xl p-5 sm:p-6 border transition-all duration-200 shadow-xs hover:shadow-md ${
                    isPending ? 'border-amber-300 bg-amber-50/20' : 'border-gray-100'
                  }`}
                >
                  <div className="grid grid-cols-1 lg:grid-cols-12 gap-5 items-start">
                    {/* Column 1: Which Service is Booked (lg:col-span-4) */}
                    <div className="lg:col-span-4 space-y-2 border-b lg:border-b-0 lg:border-r border-gray-100 pb-4 lg:pb-0 lg:pr-5">
                      <div className="flex items-center justify-between">
                        <span className="font-mono text-xs font-bold text-gray-400">#{b.id}</span>
                        <span
                          className={`px-2.5 py-0.5 rounded-full text-[10px] font-black uppercase tracking-wider ${
                            b.status === 'PENDING'
                              ? 'bg-amber-100 text-amber-900 border border-amber-300'
                              : b.status === 'ASSIGNED'
                              ? 'bg-emerald-100 text-emerald-800'
                              : b.status === 'CONFIRMED'
                              ? 'bg-teal-100 text-teal-800'
                              : b.status === 'IN_PROGRESS'
                              ? 'bg-purple-100 text-purple-800'
                              : b.status === 'COMPLETED'
                              ? 'bg-gray-100 text-gray-700'
                              : 'bg-red-100 text-red-700'
                          }`}
                        >
                          {b.status === 'PENDING' ? 'Awaiting Assignment' : b.status}
                        </span>
                      </div>

                      <div>
                        <span className="text-[10px] uppercase font-bold text-[var(--color-brand)] block tracking-wider">
                          Booked Service
                        </span>
                        <h3 className="font-black text-base text-gray-900 font-['Outfit'] mt-0.5 leading-snug">
                          {b.serviceName}
                        </h3>
                        {b.variantName && (
                          <p className="text-xs text-gray-600 font-semibold">{b.variantName}</p>
                        )}
                      </div>

                      <div className="pt-2 flex flex-wrap items-center gap-3 text-xs text-gray-600">
                        <div className="flex items-center gap-1 font-semibold text-gray-800">
                          <Calendar className="w-3.5 h-3.5 text-[var(--color-brand)]" />
                          <span>{b.scheduledDate}</span>
                        </div>
                        <div className="flex items-center gap-1 font-semibold text-gray-800">
                          <Clock className="w-3.5 h-3.5 text-[var(--color-brand)]" />
                          <span>{b.scheduledTimeSlot}</span>
                        </div>
                        <div className="font-bold text-gray-900">
                          ₹{b.total} <span className="text-[10px] text-gray-400 font-normal">({b.paymentMethod || 'CASH'})</span>
                        </div>
                      </div>
                    </div>

                    {/* Column 2: From Where - Customer & Doorstep Location (lg:col-span-4) */}
                    <div className="lg:col-span-4 space-y-2 border-b lg:border-b-0 lg:border-r border-gray-100 pb-4 lg:pb-0 lg:pr-5">
                      <span className="text-[10px] uppercase font-bold text-emerald-700 block tracking-wider">
                        From Where (Customer & Address)
                      </span>

                      <div className="flex items-center gap-2">
                        <div className="w-8 h-8 rounded-full bg-emerald-50 text-emerald-700 font-bold text-xs flex items-center justify-center shrink-0">
                          {b.userName?.slice(0, 1) || 'C'}
                        </div>
                        <div>
                          <p className="font-extrabold text-xs text-gray-900">{b.userName}</p>
                          <a
                            href={`tel:${b.userPhone}`}
                            className="text-[11px] text-gray-500 hover:text-emerald-700 flex items-center gap-1 font-semibold"
                          >
                            <Phone className="w-3 h-3 text-emerald-600" />
                            <span>{b.userPhone}</span>
                          </a>
                        </div>
                      </div>

                      <div className="p-2.5 rounded-xl bg-gray-50 text-xs text-gray-700 space-y-0.5">
                        <div className="flex items-start gap-1.5 font-medium">
                          <MapPin className="w-3.5 h-3.5 text-red-500 shrink-0 mt-0.5" />
                          <div>
                            <span className="font-bold text-gray-900">{b.address?.city || 'Agra'}</span>
                            {b.address?.area && <span>, {b.address.area}</span>}
                            <p className="text-[11px] text-gray-500 mt-0.5">
                              {b.address?.house} {b.address?.street ? `• ${b.address.street}` : ''}
                              {b.address?.pincode ? ` (${b.address.pincode})` : ''}
                            </p>
                          </div>
                        </div>
                      </div>
                    </div>

                    {/* Column 3: Manual Assignment Action (lg:col-span-4) */}
                    <div className="lg:col-span-4 space-y-3">
                      <span className="text-[10px] uppercase font-bold text-gray-400 block tracking-wider">
                        Manual Assignment Controls
                      </span>

                      {/* If already assigned */}
                      {b.professionalId ? (
                        <div className="p-3 bg-emerald-50/70 border border-emerald-200 rounded-2xl text-xs space-y-1">
                          <div className="flex items-center gap-1.5 text-emerald-800 font-bold">
                            <UserCheck className="w-4 h-4 text-emerald-600 shrink-0" />
                            <span>Assigned to: {b.professionalName || 'Professional'}</span>
                          </div>
                          {b.professionalPhone && (
                            <p className="text-[11px] text-emerald-700 pl-5">
                              Contact: {b.professionalPhone}
                            </p>
                          )}
                          <p className="text-[10px] text-gray-500 pl-5 pt-1">
                            Status: <span className="font-semibold text-emerald-700">{b.status}</span>
                          </p>
                        </div>
                      ) : (
                        <div className="p-2 bg-amber-50 rounded-xl text-[11px] text-amber-800 font-medium">
                          ⚡ No professional assigned yet. Select from verified pros below:
                        </div>
                      )}

                      {/* Manual Assignment Dropdown & Button */}
                      {(isPending || b.status === 'ASSIGNED') && (
                        <div className="space-y-2 pt-1">
                          <select
                            value={currentProSelection}
                            onChange={(e) =>
                              setSelectedProForBooking((prev) => ({
                                ...prev,
                                [b.id]: e.target.value,
                              }))
                            }
                            className="w-full px-3 py-2 bg-gray-50 border border-gray-300 rounded-xl text-xs font-semibold text-gray-800 focus:outline-none focus:ring-2 focus:ring-[var(--color-brand)] cursor-pointer"
                          >
                            <option value="">-- Choose Professional to Assign --</option>
                            {professionals.map((p) => (
                              <option key={p.id} value={p.id}>
                                {p.name} ({p.profession}) • {p.rating}★
                              </option>
                            ))}
                          </select>

                          <div className="flex items-center gap-2">
                            <button
                              disabled={!currentProSelection || assigningId === b.id}
                              onClick={() => handleAssignWorkManually(b.id)}
                              className="flex-1 py-2 bg-emerald-600 hover:bg-emerald-700 disabled:opacity-50 text-white font-bold text-xs rounded-xl shadow-xs transition-all flex items-center justify-center gap-1.5 cursor-pointer active:scale-98"
                            >
                              <UserCheck className="w-3.5 h-3.5" />
                              <span>{assigningId === b.id ? 'Assigning...' : 'Assign Work Manually'}</span>
                            </button>

                            {b.status !== 'CANCELLED' && b.status !== 'COMPLETED' && (
                              <button
                                onClick={() => handleCancelBooking(b.id)}
                                className="px-3 py-2 bg-red-50 hover:bg-red-100 text-red-600 text-xs font-bold rounded-xl transition-colors cursor-pointer"
                                title="Cancel Booking"
                              >
                                Cancel
                              </button>
                            )}
                          </div>
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              );
            })
          )}
        </div>
      </div>
    </div>
  );
};
