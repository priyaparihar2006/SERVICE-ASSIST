import React, { useState, useEffect } from 'react';
import { Booking } from '../types';
import {
  Star,
  CheckCircle2,
  Clock,
  Phone,
  ShieldCheck,
  Award,
  TrendingUp,
} from 'lucide-react';

export const ProfessionalDashboardPage: React.FC = () => {
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [otpInputs, setOtpInputs] = useState<Record<string, string>>({});
  const [otpError, setOtpError] = useState<Record<string, string>>({});

  const fetchJobs = async () => {
    try {
      const res = await fetch('/api/bookings');
      const data = await res.json();
      if (data.bookings) {
        setBookings(data.bookings);
      }
    } catch (e) {
      console.error(e);
    }
  };

  useEffect(() => {
    fetchJobs();
  }, []);

  const handleUpdateStatus = async (bookingId: string, newStatus: Booking['status']) => {
    try {
      const res = await fetch(`/api/bookings/${bookingId}/status`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ status: newStatus }),
      });
      if (res.ok) {
        fetchJobs();
      }
    } catch (e) {
      console.error(e);
    }
  };

  const handleVerifyOtpAndStart = (booking: Booking) => {
    const entered = otpInputs[booking.id];
    if (entered === booking.verificationOtp) {
      handleUpdateStatus(booking.id, 'IN_PROGRESS');
      setOtpError((prev) => ({ ...prev, [booking.id]: '' }));
    } else {
      setOtpError((prev) => ({
        ...prev,
        [booking.id]: 'Incorrect customer OTP. Ask customer for the 4-digit code.',
      }));
    }
  };

  return (
    <div className="min-h-screen bg-[#FFF8F2]/30 py-8 sm:py-12">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Partner Header Banner */}
        <div className="bg-gradient-to-br from-[#15252B] to-[#0A1215] text-white rounded-3xl p-6 sm:p-8 shadow-xl mb-8 flex flex-col md:flex-row md:items-center justify-between gap-6 border border-orange-500/20">
          <div className="flex items-center gap-4">
            <img
              src="https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=200&q=80"
              alt="Partner Profile"
              className="w-16 h-16 rounded-2xl object-cover border-2 border-[#FF7A00]"
              referrerPolicy="no-referrer"
            />
            <div>
              <div className="flex items-center gap-2">
                <h1 className="text-xl sm:text-2xl font-black font-['Outfit']">Rahul Sharma</h1>
                <span className="text-[10px] font-bold bg-[#FF7A00] text-white px-2.5 py-0.5 rounded-full flex items-center gap-1 shadow-sm">
                  <ShieldCheck className="w-3 h-3" />
                  <span>Verified Partner</span>
                </span>
              </div>
              <p className="text-xs text-gray-300 mt-1">
                HVAC Specialist & Master Electrician • Service Assist Partner ID: #PRO-8821
              </p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <div className="px-3.5 py-2 bg-white/10 rounded-2xl border border-white/10 text-center">
              <span className="text-[10px] uppercase font-bold text-gray-400 block">Status</span>
              <span className="text-xs font-bold text-emerald-400 flex items-center justify-center gap-1">
                <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
                Online & Accepting
              </span>
            </div>
          </div>
        </div>

        {/* Metrics Grid */}
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
          <div className="bg-white p-5 rounded-3xl border border-gray-100 shadow-xs">
            <div className="flex items-center justify-between text-gray-400 mb-2">
              <span className="text-xs font-bold uppercase tracking-wider">Total Earnings</span>
              <div className="w-8 h-8 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center">
                <TrendingUp className="w-4 h-4" />
              </div>
            </div>
            <div className="text-2xl font-black text-gray-900 font-['Outfit']">₹48,250</div>
            <p className="text-[11px] text-emerald-600 font-semibold mt-1">+18% vs last month</p>
          </div>

          <div className="bg-white p-5 rounded-3xl border border-gray-100 shadow-xs">
            <div className="flex items-center justify-between text-gray-400 mb-2">
              <span className="text-xs font-bold uppercase tracking-wider">Partner Rating</span>
              <div className="w-8 h-8 rounded-xl bg-amber-50 text-amber-500 flex items-center justify-center">
                <Star className="w-4 h-4 fill-amber-400" />
              </div>
            </div>
            <div className="text-2xl font-black text-gray-900 font-['Outfit']">4.94 / 5</div>
            <p className="text-[11px] text-gray-500 font-medium mt-1">Based on 1,240 ratings</p>
          </div>

          <div className="bg-white p-5 rounded-3xl border border-gray-100 shadow-xs">
            <div className="flex items-center justify-between text-gray-400 mb-2">
              <span className="text-xs font-bold uppercase tracking-wider">Jobs Completed</span>
              <div className="w-8 h-8 rounded-xl bg-[#FFF1E5] text-[#FF7A00] flex items-center justify-center">
                <CheckCircle2 className="w-4 h-4" />
              </div>
            </div>
            <div className="text-2xl font-black text-gray-900 font-['Outfit']">1,240</div>
            <p className="text-[11px] text-gray-500 font-medium mt-1">Zero safety violations</p>
          </div>

          <div className="bg-white p-5 rounded-3xl border border-gray-100 shadow-xs">
            <div className="flex items-center justify-between text-gray-400 mb-2">
              <span className="text-xs font-bold uppercase tracking-wider">Acceptance Rate</span>
              <div className="w-8 h-8 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center">
                <Award className="w-4 h-4" />
              </div>
            </div>
            <div className="text-2xl font-black text-gray-900 font-['Outfit']">98.4%</div>
            <p className="text-[11px] text-indigo-600 font-semibold mt-1">Top tier partner club</p>
          </div>
        </div>

        {/* Live Service Dispatch Queue */}
        <div className="space-y-6">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-lg sm:text-xl font-black text-[#15252B] font-['Outfit']">
                Assigned Jobs & Real-Time Requests
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
            {bookings.map((b) => (
              <div
                key={b.id}
                className="bg-white rounded-3xl p-6 border border-gray-100 shadow-xs hover:border-[#FF9A3D] transition-all space-y-4"
              >
                <div className="flex flex-wrap items-center justify-between gap-3 pb-3 border-b border-gray-100">
                  <div className="flex items-center gap-2.5">
                    <span className="font-extrabold text-sm text-gray-900 font-['Outfit']">{b.id}</span>
                    <span
                      className={`px-2.5 py-0.5 rounded-full text-[10px] font-bold ${
                        b.status === 'COMPLETED'
                          ? 'bg-emerald-100 text-emerald-800'
                          : b.status === 'IN_PROGRESS'
                          ? 'bg-[#FFF1E5] text-[#E85D04] border border-[#FF9A3D]/40 animate-pulse'
                          : 'bg-blue-100 text-blue-800'
                      }`}
                    >
                      {b.status}
                    </span>
                  </div>

                  <div className="text-right">
                    <span className="text-[10px] text-gray-400 font-bold block uppercase">Payout</span>
                    <span className="text-base font-black text-emerald-600">
                      ₹{Math.round(b.total * 0.85)} <span className="text-xs font-normal text-gray-400">(85%)</span>
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
                    <span className="text-gray-500">{b.userPhone}</span>
                  </div>

                  <div className="p-3 bg-gray-50 rounded-2xl">
                    <span className="text-gray-400 font-bold uppercase text-[10px] block mb-1">
                      Scheduled Time
                    </span>
                    <span className="font-bold text-gray-900 block">{b.scheduledDate}</span>
                    <span className="text-[#E85D04] font-medium">{b.scheduledTimeSlot}</span>
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
                          className="px-2.5 py-1 rounded-lg bg-[#FFF1E5] text-[#E85D04] font-semibold text-[11px]"
                        >
                          {item.service?.name} ({item.variant?.name}) × {item.quantity}
                        </span>
                      ))
                    ) : (
                      <span className="px-2.5 py-1 rounded-lg bg-[#FFF1E5] text-[#E85D04] font-semibold text-[11px]">
                        {b.serviceName} ({b.variantName})
                      </span>
                    )}
                  </div>
                </div>

                {/* Partner Actions & OTP Verification */}
                <div className="pt-3 border-t border-gray-100 flex flex-wrap items-center justify-between gap-4">
                  {b.status === 'ASSIGNED' && (
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
                          className="w-36 px-3 py-2 bg-gray-50 border border-gray-200 rounded-xl text-xs font-mono font-bold text-center focus:outline-none focus:ring-1 focus:ring-[#FF7A00]"
                        />
                        <button
                          onClick={() => handleVerifyOtpAndStart(b)}
                          className="px-4 py-2 bg-[#FF7A00] hover:bg-[#E85D04] text-white font-bold text-xs rounded-xl shadow-xs cursor-pointer shadow-orange-500/20"
                        >
                          Verify & Start Job
                        </button>
                      </div>

                      {otpError[b.id] && (
                        <span className="text-[11px] text-red-600 font-semibold">{otpError[b.id]}</span>
                      )}
                      <span className="text-[10px] text-gray-400 italic">
                        (Demo Hint: Customer's OTP is {b.verificationOtp})
                      </span>
                    </div>
                  )}

                  {b.status === 'IN_PROGRESS' && (
                    <div className="flex items-center gap-3">
                      <span className="text-xs font-bold text-[#E85D04] flex items-center gap-1.5">
                        <Clock className="w-4 h-4 text-[#FF7A00] animate-spin" />
                        <span>Job in progress...</span>
                      </span>
                      <button
                        onClick={() => handleUpdateStatus(b.id, 'COMPLETED')}
                        className="px-5 py-2 bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs rounded-xl shadow-xs cursor-pointer"
                      >
                        Mark Job as Completed ✓
                      </button>
                    </div>
                  )}

                  {b.status === 'COMPLETED' && (
                    <div className="flex items-center gap-1 text-xs font-bold text-emerald-700 bg-emerald-50 px-3 py-1 rounded-xl">
                      <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                      <span>Service successfully completed & paid</span>
                    </div>
                  )}

                  <a
                    href={`tel:${b.userPhone}`}
                    className="ml-auto px-3.5 py-2 rounded-xl border border-gray-200 hover:bg-gray-50 text-xs font-semibold flex items-center gap-1.5"
                  >
                    <Phone className="w-3.5 h-3.5 text-[#FF7A00]" />
                    <span>Call Customer</span>
                  </a>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};
