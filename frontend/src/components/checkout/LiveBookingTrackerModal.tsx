import React from 'react';
import { Booking } from '../../types';
import {
  Check,
  ShieldCheck,
  MessageSquare,
  ArrowLeft,
  Clock,
  MapPin,
  CheckCircle2,
  Key,
  MoreVertical,
} from 'lucide-react';

interface LiveBookingTrackerModalProps {
  booking: Booking;
  onClose: () => void;
  onMessage?: (bookingId: string) => void;
}

export const LiveBookingTrackerModal: React.FC<LiveBookingTrackerModalProps> = ({
  booking,
  onClose,
  onMessage,
}) => {
  // 7 standard status steps matching Screenshots
  const steps = [
    { key: 'PLACED', label: 'Booking Placed', desc: 'Received & logged into network' },
    { key: 'ASSIGNED', label: 'Professional Assigned', desc: 'Current State' },
    { key: 'ON_THE_WAY', label: 'On the Way to Your Home', desc: 'Partner en route' },
    { key: 'ARRIVED', label: 'Arrived at Doorstep', desc: 'Partner reached location' },
    { key: 'IN_PROGRESS', label: 'Service in Progress', desc: 'Work under execution' },
    { key: 'PAYMENT', label: 'Payment Collection', desc: 'Invoice generated' },
    { key: 'COMPLETED', label: 'Job Finished & Verified', desc: 'Warranty active' },
  ];

  const getStepIndex = (status: Booking['status']) => {
    switch (status) {
      case 'PENDING':
        return 0;
      case 'ASSIGNED':
      case 'CONFIRMED':
        return 1;
      case 'ON_THE_WAY':
        return 2;
      case 'ARRIVED':
        return 3;
      case 'IN_PROGRESS':
        return 4;
      case 'PAYMENT_PENDING':
        return 5;
      case 'COMPLETED':
        return 6;
      default:
        return 1;
    }
  };

  const currentIdx = getStepIndex(booking.status);

  const formattedAddress = booking.address?.house
    ? `${booking.address.house}, ${booking.address.street || ''} ${booking.address.area || ''}, ${booking.address.city || ''} ${booking.address.pincode ? `- ${booking.address.pincode}` : ''}`
    : typeof booking.address === 'string'
      ? booking.address
      : 'Flat 402, Royal Residency, Taj Nagri Phase 2, Taj Nagri, Agra';

  const paymentDisplayLabel =
    booking.paymentMethod === 'UPI'
      ? 'Instant UPI'
      : booking.paymentMethod === 'CARD' || booking.paymentMethod === 'NETBANKING'
        ? 'Cards / Netbanking'
        : 'Cash after service';

  return (
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-black/60 backdrop-blur-xs overflow-y-auto animate-in fade-in duration-200"
    >
      <div className="relative w-full max-w-[440px] my-auto bg-white rounded-3xl shadow-2xl border border-gray-100 overflow-hidden flex flex-col max-h-[94vh]">
        {/* Top App Bar Header */}
        <div className="px-5 py-4 border-b border-gray-100 flex items-center justify-between bg-white shrink-0">
          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={onClose}
              className="p-1.5 text-gray-700 hover:text-gray-900 rounded-xl hover:bg-gray-100 transition-colors cursor-pointer"
              aria-label="Back"
            >
              <ArrowLeft className="w-5 h-5" />
            </button>
            <h3 className="font-extrabold text-base text-gray-900 font-['Outfit']">Booking Status</h3>
          </div>
          <button
            type="button"
            onClick={() => {}}
            className="p-1.5 text-gray-500 hover:text-gray-700 rounded-xl hover:bg-gray-100 transition-colors cursor-pointer"
            aria-label="More options"
          >
            <MoreVertical className="w-5 h-5" />
          </button>
        </div>

        {/* Modal Scrollable Content */}
        <div className="p-4 sm:p-5 overflow-y-auto space-y-4 text-xs">
          {/* Top Hero Booking Confirmed Card */}
          <div className="bg-[#edf8f1] rounded-3xl p-6 text-center border border-emerald-100 shadow-2xs">
            <div className="w-14 h-14 rounded-full bg-emerald-600 text-white flex items-center justify-center mx-auto mb-3 shadow-xs">
              <Check className="w-7 h-7 stroke-[3]" />
            </div>
            <h2 className="text-xl font-black text-gray-900 font-['Outfit'] tracking-tight">
              Booking Confirmed!
            </h2>
            <p className="text-xs font-bold text-gray-700 mt-1">
              Booking Reference: {booking.id || 'SRV-59895'}
            </p>
          </div>

          {/* START SERVICE OTP Card */}
          <div className="bg-white rounded-2xl p-4 border border-gray-100 shadow-2xs flex items-center justify-between gap-3">
            <div className="flex items-center gap-3 min-w-0">
              <div className="w-11 h-11 rounded-full bg-[#edf8f1] text-emerald-600 flex items-center justify-center shrink-0">
                <Key className="w-5 h-5" />
              </div>
              <div className="min-w-0">
                <span className="text-[11px] font-black uppercase tracking-wider text-emerald-800 block">
                  START SERVICE OTP
                </span>
                <span className="text-[11px] text-gray-500 block truncate">
                  Share with pro only when at door
                </span>
              </div>
            </div>

            <div className="px-3.5 py-1.5 rounded-xl border border-emerald-600 bg-white font-mono font-black text-base text-emerald-900 tracking-[0.3em] shrink-0">
              {booking.verificationOtp ? booking.verificationOtp.split('').join(' ') : '3 9 7 5'}
            </div>
          </div>

          {/* 7-Step Vertical Status Tracker */}
          <div>
            <div className="flex items-center justify-between px-1 mb-2">
              <span className="text-[11px] font-black uppercase tracking-wider text-gray-500">
                LIVE STATUS TRACKER
              </span>
              <span className="text-[11px] font-bold text-emerald-600 flex items-center gap-1.5">
                <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
                <span>Auto-updating</span>
              </span>
            </div>

            <div className="bg-white rounded-2xl p-4 border border-gray-100 shadow-2xs space-y-3">
              {steps.map((step, idx) => {
                const isDone = idx <= currentIdx;
                const isCurrent = idx === currentIdx;

                return (
                  <div key={step.key} className="flex items-start gap-3 relative">
                    {/* Vertical connector line */}
                    {idx < steps.length - 1 && (
                      <div
                        className={`absolute left-3.5 top-6 bottom-0 w-0.5 -mb-3 transition-colors ${
                          idx < currentIdx ? 'bg-emerald-600' : 'bg-gray-200'
                        }`}
                      />
                    )}

                    {/* Step Bullet Icon */}
                    <div
                      className={`w-7 h-7 rounded-full flex items-center justify-center text-white shrink-0 z-10 transition-all ${
                        isDone
                          ? 'bg-emerald-600 shadow-xs ring-4 ring-emerald-50'
                          : 'bg-gray-200 text-gray-400'
                      }`}
                    >
                      {isDone ? <Check className="w-4 h-4 stroke-[3]" /> : <span className="w-2 h-2 rounded-full bg-gray-300" />}
                    </div>

                    {/* Step text */}
                    <div className="pt-0.5">
                      <span
                        className={`font-bold block leading-tight ${
                          isDone ? 'text-gray-900 font-extrabold text-[13px]' : 'text-gray-400'
                        }`}
                      >
                        {step.label}
                      </span>
                      {isCurrent && (
                        <span className="text-[11px] font-bold text-emerald-700 block mt-0.5">
                          Current State
                        </span>
                      )}
                    </div>
                  </div>
                );
              })}

              <div className="pt-2.5 border-t border-gray-100 flex items-center gap-2 text-[11px] text-gray-500">
                <ShieldCheck className="w-4 h-4 text-emerald-600 shrink-0" />
                <span>Steps are authorized live by your service partner as work proceeds.</span>
              </div>
            </div>
          </div>

          {/* Assigned Professional Card */}
          <div>
            <span className="text-[11px] font-black uppercase tracking-wider text-gray-500 block mb-2 px-1">
              YOUR ASSIGNED PROFESSIONAL
            </span>
            <div className="bg-white rounded-2xl p-4 border border-gray-100 shadow-2xs flex items-center justify-between gap-3">
              <div className="flex items-center gap-3 min-w-0">
                <div className="relative shrink-0">
                  <div className="w-12 h-12 rounded-full bg-[#edf8f1] text-emerald-800 font-black text-sm flex items-center justify-center border border-emerald-200">
                    {booking.professionalName ? booking.professionalName.split(' ').map((n: string) => n[0]).join('').slice(0, 2) : 'RS'}
                  </div>
                  <CheckCircle2 className="w-4 h-4 text-emerald-600 bg-white rounded-full absolute -bottom-0.5 -right-0.5" />
                </div>

                <div className="min-w-0">
                  <div className="flex items-center gap-1.5">
                    <h4 className="font-extrabold text-sm text-gray-900 truncate">
                      {booking.professionalName || 'Rajesh Sharma'}
                    </h4>
                    <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
                  </div>
                  <p className="text-[11px] text-gray-500 truncate">
                    Master AC & Appliance Technician
                  </p>
                  <p className="text-[11px] font-semibold text-gray-600 mt-0.5">
                    ★ 4.92 • 1240+ jobs completed
                  </p>
                </div>
              </div>

              <button
                type="button"
                onClick={() => {
                  if (onMessage) {
                    onMessage(booking.id);
                  }
                  onClose();
                }}
                className="px-4 py-2 bg-emerald-700 hover:bg-emerald-800 text-white text-xs font-bold rounded-xl flex items-center gap-1.5 shrink-0 shadow-xs cursor-pointer transition-colors"
              >
                <MessageSquare className="w-3.5 h-3.5" />
                <span>Message</span>
              </button>
            </div>
          </div>

          {/* Schedule & Address Card */}
          <div>
            <span className="text-[11px] font-black uppercase tracking-wider text-gray-500 block mb-2 px-1">
              SCHEDULE & ADDRESS
            </span>
            <div className="bg-white rounded-2xl p-4 border border-gray-100 shadow-2xs space-y-3">
              <div className="flex items-center gap-2.5 text-gray-900 font-bold text-xs">
                <Clock className="w-4 h-4 text-emerald-600 shrink-0" />
                <span>
                  {booking.scheduledDate || 'Today'} at {booking.scheduledTimeSlot || '02:00 PM'}
                </span>
              </div>

              <div className="flex items-start gap-2.5 text-gray-600 text-xs">
                <MapPin className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
                <span className="leading-relaxed">
                  {formattedAddress}
                </span>
              </div>

              <div className="pt-3 border-t border-gray-100 flex items-center justify-between">
                <span className="text-gray-500 font-semibold text-xs">Total Amount</span>
                <span className="font-extrabold text-sm text-emerald-700">
                  ₹{booking.total || '3673'} ({paymentDisplayLabel})
                </span>
              </div>
            </div>
          </div>
        </div>

        {/* Bottom Action Button */}
        <div className="p-4 border-t border-gray-100 bg-white shrink-0">
          <button
            type="button"
            onClick={onClose}
            className="w-full py-3.5 bg-emerald-700 hover:bg-emerald-800 text-white font-bold text-sm rounded-2xl transition-all shadow-md shadow-emerald-700/20 flex items-center justify-center gap-2 cursor-pointer"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>Back to Home</span>
          </button>
        </div>
      </div>
    </div>
  );
};
