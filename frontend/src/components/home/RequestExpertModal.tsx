import React, { useState, useEffect } from 'react';
import { Professional, Service, Category, Booking } from '../../types';
import { useAuth } from '../../context/AuthContext';
import { useLocation } from '../../context/LocationContext';
import { api, apiFetch } from '../../services/api';
import { ImageWithFallback } from '../common/ImageWithFallback';
import { getProfessionalImage } from '../../utils/professionalImages';
import {
  X,
  Star,
  ShieldCheck,
  Award,
  Calendar,
  Clock,
  MapPin,
  CheckCircle2,
  ArrowRight,
  Briefcase,
  AlertCircle,
} from 'lucide-react';

interface RequestExpertModalProps {
  isOpen: boolean;
  onClose: () => void;
  selectedProfessional: Professional | null;
  professionals: Professional[];
  services: Service[];
  categories: Category[];
  onNavigate: (path: string) => void;
}

export const RequestExpertModal: React.FC<RequestExpertModalProps> = ({
  isOpen,
  onClose,
  selectedProfessional,
  professionals,
  services,
  categories,
  onNavigate,
}) => {
  const { user, isAuthenticated, openAuthModal } = useAuth();
  const { selectedCity } = useLocation();

  const [activePro, setActivePro] = useState<Professional | null>(selectedProfessional);
  const [house, setHouse] = useState('');
  const [street, setStreet] = useState('');
  const [area, setArea] = useState('');
  const [notes, setNotes] = useState('');
  const [selectedDate, setSelectedDate] = useState(() => {
    const today = new Date();
    today.setDate(today.getDate() + 1);
    return today.toISOString().split('T')[0];
  });
  const [selectedSlot, setSelectedSlot] = useState('11:00 AM - 01:00 PM');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [successBooking, setSuccessBooking] = useState<Booking | null>(null);

  // Sync active professional when prop changes
  useEffect(() => {
    if (selectedProfessional) {
      setActivePro(selectedProfessional);
    } else if (professionals.length > 0 && !activePro) {
      setActivePro(professionals[0]);
    }
  }, [selectedProfessional, professionals]);

  // Prepopulate address if default exists
  useEffect(() => {
    const addrs = user?.addresses || [];
    const defaultAddr = addrs.find((a) => a.isDefault) || addrs[0];
    if (defaultAddr) {
      setHouse(defaultAddr.house || '');
      setStreet(defaultAddr.street || '');
      setArea(defaultAddr.area || '');
    }
  }, [user]);

  if (!isOpen) return null;

  const timeSlots = [
    '09:00 AM - 11:00 AM',
    '11:00 AM - 01:00 PM',
    '02:00 PM - 04:00 PM',
    '04:00 PM - 06:00 PM',
    '06:00 PM - 08:00 PM',
  ];

  const handleProSelect = (proId: string) => {
    const found = professionals.find((p) => p.id === proId);
    if (found) setActivePro(found);
  };

  const handleSubmitRequest = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!isAuthenticated) {
      openAuthModal();
      return;
    }

    if (!activePro) {
      setError('Please select an expert to request.');
      return;
    }

    if (!house.trim() || !area.trim()) {
      setError('Please provide your complete address (House/Flat & Area).');
      return;
    }

    setSubmitting(true);
    setError('');

    // Find associated service or default to category inspection
    const matchedService = services.find((s) => s.categoryId === activePro.categoryId) || services[0];
    const estimatedCost = matchedService?.startingPrice || 299;

    const bookingPayload = {
      id: `REQ-${Math.floor(100000 + Math.random() * 900000)}`,
      userId: user?.id || 'cust-user',
      userName: user?.name || 'Customer',
      userPhone: user?.phone || '+91 98765 43210',
      serviceId: matchedService?.id || 'srv-custom-expert',
      serviceName: `${activePro.profession} Consultation & Service`,
      variantId: matchedService?.variants?.[0]?.id || 'var-expert-request',
      variantName: activePro.name ? `Requested Expert: ${activePro.name}` : 'Doorstep Inspection',
      categoryId: activePro.categoryId || 'cat-carpenter',
      scheduledDate: selectedDate,
      scheduledTimeSlot: selectedSlot,
      address: {
        type: 'Home',
        house: house.trim(),
        street: street.trim() || 'Main Road',
        area: area.trim(),
        city: selectedCity.name || 'Agra',
        state: 'Uttar Pradesh',
        pincode: '282001',
        isDefault: true,
      },
      paymentMethod: 'CASH',
      paymentStatus: 'PENDING',
      total: estimatedCost,
      status: 'PENDING',
      professionalId: activePro.id,
      professionalName: activePro.name,
      professionalPhone: activePro.phone,
      notes: notes.trim(),
      createdAt: new Date().toLocaleDateString('en-US', { day: 'numeric', month: 'short', year: 'numeric' }),
    };

    try {
      // Attempt backend API create
      await apiFetch('/api/bookings', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Idempotency-Key': crypto.randomUUID(),
        },
        body: JSON.stringify(bookingPayload),
      }).catch((err) => {
        console.warn('Backend API note:', err.message);
      });

      // Also persist to localStorage for instant client access
      const stored = localStorage.getItem('sa_offline_bookings');
      const list = stored ? JSON.parse(stored) : [];
      list.unshift(bookingPayload);
      localStorage.setItem('sa_offline_bookings', JSON.stringify(list));

      setSuccessBooking(bookingPayload as any);
    } catch (err: any) {
      setError(err?.message || 'Failed to submit request. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-in fade-in duration-200">
      <div className="bg-white rounded-3xl shadow-2xl max-w-xl w-full max-h-[92vh] overflow-y-auto border border-gray-100 flex flex-col">
        {/* Modal Header */}
        <div className="sticky top-0 bg-white/95 backdrop-blur-md px-6 py-4 border-b border-gray-100 flex items-center justify-between z-10">
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-xl bg-[var(--color-brand-light)] text-[var(--color-brand)] flex items-center justify-center">
              <Briefcase className="w-5 h-5" />
            </div>
            <div>
              <h3 className="font-extrabold text-base text-gray-900 font-['Outfit']">Request an Expert</h3>
              <p className="text-xs text-gray-500">Directly book top-rated verified home service specialists</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-full bg-gray-100 hover:bg-gray-200 text-gray-500 flex items-center justify-center transition-colors cursor-pointer"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Modal Body */}
        <div className="p-6">
          {successBooking ? (
            <div className="text-center py-6 space-y-4 animate-in zoom-in-95 duration-200">
              <div className="w-16 h-16 bg-emerald-100 text-emerald-600 rounded-full flex items-center justify-center mx-auto shadow-sm">
                <CheckCircle2 className="w-9 h-9" />
              </div>
              <div>
                <h4 className="text-xl font-black text-gray-900 font-['Outfit']">Expert Request Placed!</h4>
                <p className="text-xs text-gray-600 max-w-sm mx-auto mt-1">
                  Your request for <span className="font-bold text-gray-800">{activePro?.name} ({activePro?.profession})</span> has been submitted with status <span className="font-bold text-amber-600">Pending Assignment</span>.
                </p>
              </div>

              <div className="p-4 bg-gray-50 rounded-2xl border border-gray-200 text-left text-xs space-y-2 max-w-md mx-auto">
                <div className="flex justify-between font-semibold">
                  <span className="text-gray-500">Booking Reference:</span>
                  <span className="text-gray-900 font-mono font-bold">{successBooking.id}</span>
                </div>
                <div className="flex justify-between font-semibold">
                  <span className="text-gray-500">Scheduled Date & Slot:</span>
                  <span className="text-gray-900">{successBooking.scheduledDate} • {successBooking.scheduledTimeSlot}</span>
                </div>
                <div className="flex justify-between font-semibold">
                  <span className="text-gray-500">Address:</span>
                  <span className="text-gray-900">{successBooking.address.house}, {successBooking.address.area}, {successBooking.address.city}</span>
                </div>
              </div>

              <div className="flex items-center justify-center gap-3 pt-2">
                <button
                  onClick={() => {
                    onClose();
                    onNavigate('/dashboard');
                  }}
                  className="px-6 py-2.5 bg-[var(--color-brand)] hover:bg-[var(--color-brand-hover)] text-white font-bold text-xs rounded-xl shadow-md cursor-pointer transition-all"
                >
                  View in My Bookings
                </button>
                <button
                  onClick={onClose}
                  className="px-5 py-2.5 bg-gray-100 hover:bg-gray-200 text-gray-700 font-bold text-xs rounded-xl cursor-pointer transition-all"
                >
                  Done
                </button>
              </div>
            </div>
          ) : (
            <form onSubmit={handleSubmitRequest} className="space-y-5">
              {error && (
                <div className="p-3 bg-red-50 border border-red-200 rounded-xl text-xs text-red-600 flex items-center gap-2">
                  <AlertCircle className="w-4 h-4 shrink-0" />
                  <span>{error}</span>
                </div>
              )}

              {/* Step 1: Selected Expert Preview Card */}
              {activePro && (
                <div className="p-4 rounded-2xl bg-[var(--color-brand-soft)]/40 border border-[var(--color-brand-light)] flex items-center justify-between gap-4">
                  <div className="flex items-center gap-3.5">
                    <div className="relative shrink-0">
                      <ImageWithFallback
                        src={getProfessionalImage(activePro)}
                        fallbackProfession={activePro.profession}
                        alt={activePro.name}
                        className="w-14 h-14 rounded-2xl object-cover object-top border-2 border-[var(--color-brand)]"
                      />
                      <span className="absolute -bottom-1 -right-1 w-4 h-4 bg-emerald-500 border-2 border-white rounded-full flex items-center justify-center text-white text-[9px] font-bold">
                        ✓
                      </span>
                    </div>
                    <div>
                      <div className="flex items-center gap-1.5">
                        <span className="font-bold text-sm text-gray-900 font-['Outfit']">{activePro.name}</span>
                        <span className="px-2 py-0.5 rounded-md bg-white border border-gray-200 text-[10px] font-bold text-gray-700">
                          {activePro.experienceYears}y exp
                        </span>
                      </div>
                      <p className="text-xs font-semibold text-[var(--color-brand-hover)]">{activePro.profession}</p>
                      <div className="flex items-center gap-1 text-[11px] text-amber-500 font-bold mt-0.5">
                        <Star className="w-3.5 h-3.5 fill-amber-400 text-amber-400" />
                        <span>{activePro.rating} rating</span>
                        <span className="text-gray-400 font-normal">• {activePro.completedJobs} jobs done</span>
                      </div>
                    </div>
                  </div>

                  {professionals.length > 1 && (
                    <select
                      value={activePro.id}
                      onChange={(e) => handleProSelect(e.target.value)}
                      className="px-2.5 py-1.5 bg-white border border-gray-200 rounded-xl text-xs font-semibold text-gray-700 focus:outline-none focus:ring-1 focus:ring-[var(--color-brand)] cursor-pointer"
                    >
                      {professionals.map((p) => (
                        <option key={p.id} value={p.id}>
                          {p.name} ({p.profession})
                        </option>
                      ))}
                    </select>
                  )}
                </div>
              )}

              {/* Step 2: Date & Slot Pickers */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-gray-700 mb-1 flex items-center gap-1">
                    <Calendar className="w-3.5 h-3.5 text-[var(--color-brand)]" />
                    <span>Preferred Date</span>
                  </label>
                  <input
                    type="date"
                    min={new Date().toISOString().split('T')[0]}
                    value={selectedDate}
                    onChange={(e) => setSelectedDate(e.target.value)}
                    required
                    className="w-full px-3 py-2 bg-gray-50 border border-gray-200 rounded-xl text-xs font-semibold text-gray-800 focus:outline-none focus:ring-2 focus:ring-[var(--color-brand)]"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-gray-700 mb-1 flex items-center gap-1">
                    <Clock className="w-3.5 h-3.5 text-[var(--color-brand)]" />
                    <span>Preferred Time Slot</span>
                  </label>
                  <select
                    value={selectedSlot}
                    onChange={(e) => setSelectedSlot(e.target.value)}
                    className="w-full px-3 py-2 bg-gray-50 border border-gray-200 rounded-xl text-xs font-semibold text-gray-800 focus:outline-none focus:ring-2 focus:ring-[var(--color-brand)] cursor-pointer"
                  >
                    {timeSlots.map((slot) => (
                      <option key={slot} value={slot}>
                        {slot}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              {/* Step 3: Doorstep Address */}
              <div className="space-y-2">
                <label className="block text-xs font-bold text-gray-700 flex items-center justify-between">
                  <span className="flex items-center gap-1">
                    <MapPin className="w-3.5 h-3.5 text-[var(--color-brand)]" />
                    <span>Service Address (City: {selectedCity.name})</span>
                  </span>
                </label>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                  <input
                    type="text"
                    placeholder="House / Flat / Floor No.*"
                    value={house}
                    onChange={(e) => setHouse(e.target.value)}
                    required
                    className="px-3 py-2 bg-gray-50 border border-gray-200 rounded-xl text-xs text-gray-800 focus:outline-none focus:ring-2 focus:ring-[var(--color-brand)]"
                  />
                  <input
                    type="text"
                    placeholder="Street / Society / Landmark"
                    value={street}
                    onChange={(e) => setStreet(e.target.value)}
                    className="px-3 py-2 bg-gray-50 border border-gray-200 rounded-xl text-xs text-gray-800 focus:outline-none focus:ring-2 focus:ring-[var(--color-brand)]"
                  />
                </div>
                <input
                  type="text"
                  placeholder="Area / Locality (e.g. Sanjay Place, Taj Nagri)*"
                  value={area}
                  onChange={(e) => setArea(e.target.value)}
                  required
                  className="w-full px-3 py-2 bg-gray-50 border border-gray-200 rounded-xl text-xs text-gray-800 focus:outline-none focus:ring-2 focus:ring-[var(--color-brand)]"
                />
              </div>

              {/* Step 4: Work Requirement / Problem description */}
              <div>
                <label className="block text-xs font-bold text-gray-700 mb-1">
                  Describe what you need help with (optional)
                </label>
                <textarea
                  rows={2}
                  placeholder="e.g. Carpenter needed for wardrobe hinge repair and hydraulic bed fitting..."
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  className="w-full px-3 py-2 bg-gray-50 border border-gray-200 rounded-xl text-xs text-gray-800 focus:outline-none focus:ring-2 focus:ring-[var(--color-brand)]"
                />
              </div>

              {/* Transparent Pricing Notice */}
              <div className="p-3 bg-amber-50/70 border border-amber-200/80 rounded-xl text-[11px] text-amber-900 flex items-center justify-between">
                <span>Doorstep visit & inspection starts at standard nominal rate. Pay securely after work completion.</span>
                <span className="font-bold text-amber-800 shrink-0 ml-2">Verified Guarantee</span>
              </div>

              {/* Submit CTA */}
              <button
                type="submit"
                disabled={submitting}
                className="w-full py-3.5 bg-[var(--color-brand)] hover:bg-[var(--color-brand-hover)] disabled:opacity-60 text-white font-bold text-sm rounded-2xl shadow-lg shadow-[var(--color-brand)]/20 transition-all flex items-center justify-center gap-2 cursor-pointer active:scale-98"
              >
                <span>{submitting ? 'Placing Request...' : `Request ${activePro?.name || 'Expert'} Now`}</span>
                <ArrowRight className="w-4 h-4" />
              </button>
            </form>
          )}
        </div>
      </div>
    </div>
  );
};
