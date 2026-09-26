import React from 'react';
import { Booking } from '../../types';
import { Check, Shield, MessageSquare, ArrowLeft, Clock, MapPin, CheckCircle2 } from 'lucide-react';

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
  // 7 standard status steps matching Screenshot 2
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

  return (
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-black/60 backdrop-blur-xs overflow-y-auto animate-in fade-in duration-200"
    >
      <div className="relative w-full max-w-[440px] my-auto bg-white rounded-3xl shadow-2xl border border-gray-100 overflow-hidden flex flex-col max-h-[94vh]">
        {/* Header Bar */}
        <div className="px-6 pt-5 pb-3 border-b border-gray-100 flex items-center justify-between bg-white shrink-0">
          <div>
            <span className="text-[11px] font-black uppercase tracking-wider text-gray-500 block">
              LIVE STATUS TRACKER
            </span>
            <span className="text-xs font-bold text-emerald-600 flex items-center gap-1.5 mt-0.5">
              <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
              <span>Auto-updating</span>
            </span>
          </div>

          <div className="px-3 py-1 rounded-full bg-[var(--color-brand-soft)] border border-[var(--color-brand-light)] text-[11px] font-mono font-bold text-[var(--color-brand-hover)]">
            #{booking.id?.slice(0, 10) || 'SRV-50068'}
          </div>
        </div>

        {/* Modal Scroll Content */}
        <div className="p-5 overflow-y-auto space-y-4 text-xs">
          {/* 7-Step Vertical Tracker Card */}
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
                        idx < currentIdx ? 'bg-emerald-500' : 'bg-gray-200'
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
                    {isCurrent ? (
                      <span className="text-[10px] font-bold text-emerald-700 block mt-0.5">
                        Current State
                      </span>
                    ) : (
                      <span className="text-[10px] text-gray-400 block mt-0.5">{step.desc}</span>
                    )}
                  </div>
                </div>
              );
            })}

            <div className="pt-2 border-t border-gray-100 flex items-center gap-1.5 text-[10px] text-gray-500">
              <Shield className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
              <span>Steps are authorized live by your service partner as work proceeds.</span>
            </div>
          </div>

          {/* Assigned Professional Card */}
          <div>
            <span className="text-[10px] font-black uppercase tracking-wider text-gray-400 block mb-1.5 px-1">
              YOUR ASSIGNED PROFESSIONAL
            </span>
            <div className="bg-white rounded-2xl p-4 border border-gray-100 shadow-2xs flex items-center justify-between gap-3">
              <div className="flex items-center gap-3 min-w-0">
                <div className="relative">
                  <div className="w-12 h-12 rounded-full bg-emerald-50 text-emerald-800 font-black text-sm flex items-center justify-center border border-emerald-200">
                    {booking.professionalName ? booking.professionalName.split(' ').map((n: string) => n[0]).join('').slice(0, 2) : 'RS'}
                  </div>
                  <CheckCircle2 className="w-4 h-4 text-emerald-600 bg-white rounded-full absolute -bottom-0.5 -right-0.5" />
                </div>

                <div className="min-w-0">
                  <h4 className="font-extrabold text-sm text-gray-900 truncate">
                    {booking.professionalName || 'Rajesh Sharma'}
                  </h4>
                  <p className="text-[11px] text-gray-500 truncate">
                    Master AC & Appliance Technician
                  </p>
                  <p className="text-[10px] font-semibold text-gray-500 mt-0.5">
                    ★ 4.92 • 1240+ jobs completed
                  </p>
                </div>
              </div>

              <button
                type="button"
                onClick={() => {
                  if (onMessage) onMessage(booking.id);
                  onClose();
                }}
                className="px-3.5 py-2 bg-[var(--color-brand)] hover:bg-[var(--color-brand-hover)] text-white text-xs font-bold rounded-xl flex items-center gap-1.5 shrink-0 shadow-xs cursor-pointer"
              >
                <MessageSquare className="w-3.5 h-3.5" />
                <span>Message</span>
              </button>
            </div>
          </div>

          {/* Schedule & Address Card */}
          <div>
            <span className="text-[10px] font-black uppercase tracking-wider text-gray-400 block mb-1.5 px-1">
              SCHEDULE & ADDRESS
            </span>
            <div className="bg-white rounded-2xl p-4 border border-gray-100 shadow-2xs space-y-2.5">
              <div className="flex items-center gap-2 text-gray-900 font-bold">
                <Clock className="w-4 h-4 text-emerald-600 shrink-0" />
                <span>
                  {booking.scheduledDate || 'Today'} at {booking.scheduledTimeSlot || '02:00 PM'}
                </span>
              </div>

              <div className="flex items-start gap-2 text-gray-600">
                <MapPin className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
                <span className="leading-relaxed">
                  {booking.address?.house ? `${booking.address.house}, ${booking.address.area}, ${booking.address.city}` : 'Flat 402, Royal Residency, Taj Nagri Phase 2, Taj Nagri, Agra'}
                </span>
              </div>

              {booking.verificationOtp && (
                <div className="p-2.5 bg-amber-50 rounded-xl border border-amber-200 flex items-center justify-between text-[11px]">
                  <span className="text-amber-800 font-semibold">Start OTP:</span>
                  <span className="font-mono font-black text-sm text-amber-900 tracking-wider bg-white px-2 py-0.5 rounded border border-amber-200">
                    {booking.verificationOtp}
                  </span>
                </div>
              )}

              <div className="pt-2 border-t border-gray-100 flex items-center justify-between">
                <span className="text-gray-500 font-semibold">Total Amount</span>
                <span className="font-extrabold text-sm text-emerald-700">
                  ₹{booking.total || '3673'} ({booking.paymentMethod === 'COD' ? 'Cash after service' : 'Paid Online'})
                </span>
              </div>
            </div>
          </div>
        </div>

        {/* Bottom Button */}
        <div className="p-4 border-t border-gray-100 bg-white shrink-0">
          <button
            type="button"
            onClick={onClose}
            className="w-full py-3.5 bg-[var(--color-brand)] hover:bg-[var(--color-brand-hover)] text-white font-bold text-sm rounded-2xl transition-all shadow-md shadow-brand/20 flex items-center justify-center gap-2 cursor-pointer"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>Back to Home</span>
          </button>
        </div>
      </div>
    </div>
  );
};
