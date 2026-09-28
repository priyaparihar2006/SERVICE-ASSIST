import { api, getAll } from '../services/api';
import { AdminManagement } from '../components/account/AdminManagement';
import { apiFetch } from '../services/api';
import React, { useState, useEffect } from 'react';
import { Booking, Service, Professional } from '../types';
import {
  Crown,
  Search,
  RefreshCw,
} from 'lucide-react';

interface AdminDashboardPageProps {
  services: Service[];
  professionals: Professional[];
}

export const AdminDashboardPage: React.FC<AdminDashboardPageProps> = () => {
  const [metrics, setMetrics] = useState<any>({});
  const [eligible, setEligible] = useState<Record<string, { id: string; name: string; businessName: string; scheduledJobs: number }[]>>({});
  const [candidateLoading, setCandidateLoading] = useState<Record<string, boolean>>({});
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [filterStatus, setFilterStatus] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState('');

  const fetchAllBookings = async () => {
    try {
      setLoading(true); setError('');
      const [rows, m] = await Promise.all([getAll('/bookings', 'bookings'), api('/admin/metrics')]);
      setBookings(rows); setMetrics(m);
    } catch (e) {
      setError(e.message);
    } finally { setLoading(false); }
  };

  useEffect(() => {
    fetchAllBookings();
    const timer = window.setInterval(() => { if (document.visibilityState === 'visible') fetchAllBookings(); }, 30000);
    return () => window.clearInterval(timer);
  }, []);

  const loadEligible = async (bookingId: string) => {
    setCandidateLoading((old) => ({ ...old, [bookingId]: true }));
    try {
      const data = await api(`/admin/bookings/${bookingId}/eligible-professionals`);
      setEligible((old) => ({ ...old, [bookingId]: data.professionals }));
    } catch (e) { setError(e.message); }
    finally { setCandidateLoading((old) => ({ ...old, [bookingId]: false })); }
  };

  const handleStatusChange = async (bookingId: string, status: Booking['status'], professionalId?: string) => {
    try {
      const res = await apiFetch(`/api/bookings/${bookingId}/status`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ status, professionalId }),
      });
      if (res.ok) {
        setEligible((old) => { const next = { ...old }; delete next[bookingId]; return next; });
        fetchAllBookings();
      }
    } catch (e) {
      alert(e.message);
    }
  };

  const filteredBookings = bookings.filter((b) => {
    const matchStatus = filterStatus === 'ALL' || b.status === filterStatus;
    const matchSearch =
      !searchQuery ||
      b.id.toLowerCase().includes(searchQuery.toLowerCase()) ||
      b.userName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      b.address.city.toLowerCase().includes(searchQuery.toLowerCase());
    return matchStatus && matchSearch;
  });

  const totalGMV = bookings.filter(b => b.status !== 'CANCELLED').reduce((sum, b) => sum + b.total, 0);

  return (
    <div className="min-h-screen bg-[var(--color-brand-soft)]/30 py-8 sm:py-12">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Admin Header */}
        <div className="bg-gradient-to-br from-[var(--color-ink)] via-[var(--color-ink)] to-[var(--color-ink)] text-white rounded-3xl p-6 sm:p-8 shadow-xl mb-8 flex flex-col sm:flex-row sm:items-center justify-between gap-4 border border-brand/20">
          <div className="flex items-center gap-4">
            <div className="w-14 h-14 rounded-2xl bg-brand/20 border border-brand/30 flex items-center justify-center text-[var(--color-brand-bright)]">
              <Crown className="w-7 h-7" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h1 className="text-xl sm:text-2xl font-black font-['Outfit']">Service Assist Admin Operations</h1>
                <span className="text-[10px] font-bold bg-[var(--color-brand)] text-white px-2.5 py-0.5 rounded-full uppercase shadow-xs">
                  Super Admin
                </span>
              </div>
              <p className="text-xs text-gray-300 mt-0.5">
                Full-platform overview • Live bookings dispatch • Quality assurance & metrics
              </p>
            </div>
          </div>

          <button
            onClick={fetchAllBookings}
            className="px-4 py-2 bg-white/10 hover:bg-white/20 border border-white/20 text-white rounded-xl text-xs font-bold flex items-center gap-2 cursor-pointer transition-colors"
          >
            <RefreshCw className="w-3.5 h-3.5" />
            <span>Sync Live DB</span>
          </button>
        </div>

        {loading && <p role="status">Loading dashboard...</p>}{error && <p role="alert">{error}</p>}
        <p className="my-3">Users: {metrics.totalUsers || 0} · Active services: {metrics.totalServices || 0}</p>
        <AdminManagement onChanged={fetchAllBookings} />
        {/* Top KPIs */}
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
          <div className="bg-white p-5 rounded-3xl border border-gray-100 shadow-xs">
            <span className="text-xs font-bold text-gray-400 uppercase tracking-wider block mb-1">
              Gross Merchandise Value
            </span>
            <div className="text-2xl font-black text-gray-900 font-['Outfit']">₹{(totalGMV / 100000).toFixed(2)} Lakh</div>
            <p className="text-[11px] text-brand font-semibold mt-1">Collected payments: ₹{metrics.revenue || 0}</p>
          </div>

          <div className="bg-white p-5 rounded-3xl border border-gray-100 shadow-xs">
            <span className="text-xs font-bold text-gray-400 uppercase tracking-wider block mb-1">
              Total Platform Bookings
            </span>
            <div className="text-2xl font-black text-gray-900 font-['Outfit']">{metrics.totalBookings || 0}</div>
            <p className="text-[11px] text-[var(--color-brand)] font-semibold mt-1">Saved bookings</p>
          </div>

          <div className="bg-white p-5 rounded-3xl border border-gray-100 shadow-xs">
            <span className="text-xs font-bold text-gray-400 uppercase tracking-wider block mb-1">
              Registered Professionals
            </span>
            <div className="text-2xl font-black text-gray-900 font-['Outfit']">{metrics.totalProfessionals || 0}</div>
            <p className="text-[11px] text-brand font-semibold mt-1">Registered professionals</p>
          </div>

          <div className="bg-white p-5 rounded-3xl border border-gray-100 shadow-xs">
            <span className="text-xs font-bold text-gray-400 uppercase tracking-wider block mb-1">
              CSAT Quality Score
            </span>
            <div className="text-2xl font-black text-[var(--color-brand-bright)] font-['Outfit']">{metrics.rating || 0} / 5.0</div>
            <p className="text-[11px] text-gray-500 font-semibold mt-1">Verified booking reviews</p>
          </div>
        </div>

        {/* Live Bookings Table */}
        <div className="bg-white rounded-3xl border border-gray-100 shadow-xs overflow-hidden mb-8">
          <div className="p-6 border-b border-gray-100 flex flex-col md:flex-row md:items-center justify-between gap-4">
            <div>
              <h3 className="font-extrabold text-base text-gray-900 font-['Outfit']">Live Orders & Service Dispatch</h3>
              <p className="text-xs text-gray-500">Real-time status management and operational audit</p>
            </div>

            <div className="flex flex-wrap items-center gap-3">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                <input
                  type="text"
                  placeholder="Filter by Booking ID, customer..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="pl-9 pr-3 py-1.5 bg-gray-50 border border-gray-200 rounded-xl text-xs focus:outline-none focus:ring-1 focus:ring-[var(--color-brand)]"
                />
              </div>

              <select
                value={filterStatus}
                onChange={(e) => setFilterStatus(e.target.value)}
                className="px-3 py-1.5 bg-gray-50 border border-gray-200 rounded-xl text-xs font-semibold cursor-pointer"
              >
                <option value="ALL">All Statuses</option>
                <option value="PENDING">Pending</option>
                <option value="ASSIGNED">Assigned</option>
                <option value="IN_PROGRESS">In Progress</option>
                <option value="COMPLETED">Completed</option>
                <option value="CANCELLED">Cancelled</option>
              </select>
            </div>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-xs">
              <thead>
                <tr className="border-b border-gray-100 bg-gray-50/70 text-gray-400 font-bold uppercase tracking-wider text-[10px]">
                  <th className="p-4 pl-6">Booking ID</th>
                  <th className="p-4">Customer</th>
                  <th className="p-4">City / Address</th>
                  <th className="p-4">Assigned Partner</th>
                  <th className="p-4">Amount</th>
                  <th className="p-4">Status</th>
                  <th className="p-4 pr-6">Admin Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {filteredBookings.map((b) => (
                  <tr key={b.id} className="hover:bg-gray-50/50 transition-colors">
                    <td className="p-4 pl-6 font-bold text-gray-900 font-['Outfit']">{b.id}</td>
                    <td className="p-4">
                      <span className="font-semibold text-gray-800 block">{b.userName}</span>
                      <span className="text-[11px] text-gray-400">{b.userPhone}</span>
                    </td>
                    <td className="p-4">
                      <span className="font-semibold text-gray-800 block">{b.address.city}</span>
                      <span className="text-[11px] text-gray-400 truncate max-w-[150px] block">
                        {b.address.area}
                      </span>
                    </td>
                    <td className="p-4 text-gray-700 font-medium">
                      {b.professionalName || 'Unassigned'}
                    </td>
                    <td className="p-4 font-bold text-gray-900">₹{b.total}</td>
                    <td className="p-4">
                      <span
                        className={`px-2 py-0.5 rounded-md text-[10px] font-bold ${
                          b.status === 'COMPLETED'
                            ? 'bg-brand-light text-brand-dark'
                            : b.status === 'IN_PROGRESS'
                            ? 'bg-[var(--color-brand-light)] text-[var(--color-brand-hover)] border border-[var(--color-brand-bright)]/40'
                            : b.status === 'CANCELLED'
                            ? 'bg-red-100 text-red-800'
                            : 'bg-brand-light text-brand-dark'
                        }`}
                      >
                        {b.status}
                      </span>
                    </td>
                    <td className="p-4 pr-6">
                      {b.status === 'PENDING' && (eligible[b.id]
                        ? <select aria-label={`Assign ${b.id}`} defaultValue="" onChange={e => handleStatusChange(b.id, 'ASSIGNED', e.target.value)} className="border rounded-xl p-2"><option value="" disabled>{eligible[b.id].length ? 'Assign eligible professional' : 'No eligible professional'}</option>{eligible[b.id].map(p => <option key={p.id} value={p.id}>{p.name}{p.businessName ? ` · ${p.businessName}` : ''} · {p.scheduledJobs} job(s) that day</option>)}</select>
                        : <button disabled={candidateLoading[b.id]} onClick={() => loadEligible(b.id)} className="rounded-xl border border-brand px-2 py-1.5 text-brand disabled:opacity-50">{candidateLoading[b.id] ? 'Checking...' : 'Find eligible professionals'}</button>)}
                      {['PENDING', 'ASSIGNED', 'CONFIRMED', 'ON_THE_WAY'].includes(b.status) && <button onClick={() => handleStatusChange(b.id, 'CANCELLED')} className="text-red-600">Cancel</button>}
                      {b.status === 'COMPLETED' && b.paymentStatus === 'PENDING' && <button className="underline" onClick={async () => { const reference = prompt('Cash collection receipt/reference (only after cash is collected):'); if (!reference) return; try { await api(`/admin/bookings/${b.id}/payment`, { method: 'PUT', body: JSON.stringify({ transactionId: reference }) }); fetchAllBookings(); } catch (e) { alert(e.message); } }}>Record collected cash</button>}

                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
};
