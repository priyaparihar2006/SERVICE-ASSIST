import { apiFetch } from '../../services/api';
import React, { useState, useEffect } from 'react';
import { useCart } from '../../context/CartContext';
import { useAuth } from '../../context/AuthContext';
import { useLocation } from '../../context/LocationContext';
import { Booking } from '../../types';
import { LiveBookingTrackerModal } from './LiveBookingTrackerModal';
import {
  X,
  Calendar,
  Clock,
  MapPin,
  Plus,
  CreditCard,
  ShieldCheck,
  CheckCircle2,
  ArrowRight,
  ArrowLeft,
  Sparkles,
  Smartphone,
  Banknote,
  Check,
} from 'lucide-react';

interface CheckoutModalProps {
  onSuccess: (booking: Booking) => void;
}

export const CheckoutModal: React.FC<CheckoutModalProps> = ({ onSuccess }) => {
  const {
    items,
    subtotal,
    discount,
    taxes,
    total,
    totalSaved,
    appliedCoupon,
    bookingDate,
    setBookingDate,
    bookingTimeSlot,
    setBookingTimeSlot,
    selectedAddress,
    setSelectedAddress,
    specialInstructions,
    setSpecialInstructions,
    isCheckoutModalOpen,
    closeCheckoutModal,
    clearCart,
  } = useCart();

  const { user, addAddress, openAuthModal } = useAuth();
  const { selectedCity } = useLocation();

  const [currentStep, setCurrentStep] = useState<1 | 2 | 3>(1);
  const [paymentMethod, setPaymentMethod] = useState<'UPI' | 'CARD' | 'COD'>('COD');
  const [requestKey, setRequestKey] = useState(() => crypto.randomUUID());
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [confirmedBooking, setConfirmedBooking] = useState<Booking | null>(null);

  // Address creation state
  const [isAddingNewAddress, setIsAddingNewAddress] = useState(false);
  const [newHouse, setNewHouse] = useState('');
  const [newStreet, setNewStreet] = useState('');
  const [newArea, setNewArea] = useState('');
  const [newPincode, setNewPincode] = useState('282001');
  const [newType, setNewType] = useState<'Home' | 'Work' | 'Other'>('Home');

  useEffect(() => { if (isCheckoutModalOpen && !confirmedBooking) setCurrentStep(1); }, [isCheckoutModalOpen]);
  useEffect(() => { setRequestKey(crypto.randomUUID()); }, [items, bookingDate, bookingTimeSlot, selectedAddress?.id, specialInstructions, appliedCoupon?.code, user?.id]);

  if (!isCheckoutModalOpen) return null;

  const timeSlots = [
    '08:00 AM - 09:00 AM',
    '09:00 AM - 10:00 AM',
    '10:00 AM - 11:00 AM',
    '11:00 AM - 12:00 PM',
    '01:00 PM - 02:00 PM',
    '02:00 PM - 03:00 PM',
    '04:00 PM - 05:00 PM',
    '05:00 PM - 06:00 PM',
    '06:00 PM - 07:00 PM',
  ];

  const handleSaveAddress = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
    const created = await addAddress({
      type: newType,
      house: newHouse,
      street: newStreet,
      area: newArea,
      city: selectedCity.name,
      state: selectedCity.state,
      pincode: newPincode,
      isDefault: true,
    });
    setSelectedAddress(created);
    setIsAddingNewAddress(false);
    } catch (e) { alert(e.message); }
  };

  const handlePlaceOrder = async () => {
    if (!user) { openAuthModal(); return; }
    if (!selectedAddress) {
      alert('Please select or add a delivery address');
      setCurrentStep(2);
      return;
    }

    setIsSubmitting(true);
    try {
      const payload = {
        userId: user.id,
        userName: user.name,
        userPhone: user.phone,
        userEmail: user.email,
        items,
        address: selectedAddress,
        scheduledDate: bookingDate,
        scheduledTimeSlot: bookingTimeSlot,
        subtotal,
        discount,
        tax: taxes,
        total,
        paymentMethod,
        specialInstructions,
        couponCode: appliedCoupon?.code,
      };

      const res = await apiFetch('/api/bookings', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Idempotency-Key': requestKey },
        body: JSON.stringify(payload),
      });

      const data = await res.json();
      if (data.booking) {
        setConfirmedBooking(data.booking); setRequestKey(crypto.randomUUID());
        clearCart();
        onSuccess(data.booking);
      }
    } catch (err) {
      console.error(err);
      alert(err.message);
    } finally {
      setIsSubmitting(false);
    }
  };

  // If order confirmed, show Live Booking Tracker Screen (Screenshot 2)
  if (confirmedBooking) {
    return (
      <LiveBookingTrackerModal
        booking={confirmedBooking}
        onClose={() => {
          setConfirmedBooking(null);
          closeCheckoutModal();
        }}
      />
    );
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-in fade-in duration-200">
      <div className="relative w-full max-w-2xl bg-white rounded-3xl shadow-2xl border border-gray-100 overflow-hidden flex flex-col max-h-[90vh]">
        {/* Header */}
        <div className="px-6 py-4 border-b border-gray-100 flex items-center justify-between bg-white">
          <div className="flex items-center gap-3">
            <button
              onClick={closeCheckoutModal}
              className="p-1 text-gray-400 hover:text-gray-600 rounded-lg cursor-pointer"
            >
              <X className="w-5 h-5" />
            </button>
            <div>
              <h3 className="font-bold text-gray-900 text-base font-['Outfit']">Schedule & Checkout</h3>
              <p className="text-xs text-gray-500">Step {currentStep} of 3</p>
            </div>
          </div>

          {/* Stepper Indicators */}
          <div className="flex items-center gap-2">
            {[1, 2, 3].map((step) => (
              <div
                key={step}
                className={`w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold transition-all ${
                  currentStep === step
                    ? 'bg-[var(--color-brand)] text-white shadow-xs'
                    : currentStep > step
                    ? 'bg-[var(--color-brand-light)] text-[var(--color-brand-hover)]'
                    : 'bg-gray-100 text-gray-400'
                }`}
              >
                {currentStep > step ? '✓' : step}
              </div>
            ))}
          </div>
        </div>

        {/* Modal Body */}
        <div className="flex-1 overflow-y-auto p-6 space-y-6">
          {/* STEP 1: Date & Time Slot */}
          {currentStep === 1 && (
            <div className="space-y-6">
              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-gray-500 mb-2">
                  1. Select Service Date
                </label>
                <div className="grid grid-cols-3 sm:grid-cols-4 gap-2">
                  {[0, 1, 2, 3].map((offset) => {
                    const d = new Date();
                    d.setDate(d.getDate() + offset);
                    const dateStr = d.toISOString().split('T')[0];
                    const label =
                      offset === 0
                        ? 'Today'
                        : offset === 1
                        ? 'Tomorrow'
                        : d.toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric' });
                    const isSelected = bookingDate === dateStr;

                    return (
                      <button
                        key={dateStr}
                        type="button"
                        onClick={() => setBookingDate(dateStr)}
                        className={`p-3 rounded-2xl border text-center transition-all cursor-pointer ${
                          isSelected
                            ? 'border-[var(--color-brand)] bg-[var(--color-brand-light)] text-[var(--color-ink)] font-bold shadow-xs'
                            : 'border-gray-200 text-gray-700 hover:bg-gray-50'
                        }`}
                      >
                        <span className="text-xs block font-bold">{label}</span>
                        <span className="text-[10px] text-gray-500">{dateStr}</span>
                      </button>
                    );
                  })}
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-gray-500 mb-2">
                  2. Choose 1-Hour Arrival Slot
                </label>
                <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
                  {timeSlots.map((slot) => {
                    const isSelected = bookingTimeSlot === slot;
                    return (
                      <button
                        key={slot}
                        type="button"
                        onClick={() => setBookingTimeSlot(slot)}
                        className={`p-2.5 rounded-xl border text-xs font-semibold text-center transition-all cursor-pointer ${
                          isSelected
                            ? 'border-[var(--color-brand)] bg-[var(--color-brand-light)] text-[var(--color-brand-hover)] font-bold shadow-xs'
                            : 'border-gray-200 text-gray-700 hover:bg-gray-50'
                        }`}
                      >
                        {slot}
                      </button>
                    );
                  })}
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-gray-500 mb-2">
                  3. Any Special Instructions for the Professional? (Optional)
                </label>
                <textarea
                  rows={2}
                  placeholder="e.g. Ring the bell twice, parking available in basement, bring extra outdoor coil cleaner..."
                  value={specialInstructions}
                  onChange={(e) => setSpecialInstructions(e.target.value)}
                  className="w-full p-3 bg-gray-50 border border-gray-200 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-[var(--color-brand)]/30 focus:border-[var(--color-brand)]"
                />
              </div>
            </div>
          )}

          {/* STEP 2: Address Selection */}
          {currentStep === 2 && (
            <div className="space-y-4">
              <div className="flex items-center justify-between mb-2">
                <label className="text-xs font-bold uppercase tracking-wider text-gray-500">
                  Select Service Delivery Address
                </label>
                <button
                  type="button"
                  onClick={() => setIsAddingNewAddress(!isAddingNewAddress)}
                  className="text-xs font-bold text-[var(--color-brand)] hover:underline flex items-center gap-1 cursor-pointer"
                >
                  <Plus className="w-3.5 h-3.5" />
                  <span>{isAddingNewAddress ? 'Cancel' : 'Add New Address'}</span>
                </button>
              </div>

              {/* Add New Address Form */}
              {isAddingNewAddress ? (
                <form onSubmit={handleSaveAddress} className="p-4 bg-[var(--color-brand-soft)]/60 rounded-2xl border border-brand-light/80 space-y-3">
                  <h4 className="font-bold text-xs text-gray-800">Add New Address ({selectedCity.name})</h4>
                  <div className="grid grid-cols-2 gap-2">
                    <input
                      type="text"
                      required
                      placeholder="House / Flat / Block No."
                      value={newHouse}
                      onChange={(e) => setNewHouse(e.target.value)}
                      className="p-2.5 bg-white border border-gray-200 rounded-xl text-xs focus:outline-none focus:ring-1 focus:ring-[var(--color-brand)]"
                    />
                    <input
                      type="text"
                      required
                      placeholder="Street / Road / Colony"
                      value={newStreet}
                      onChange={(e) => setNewStreet(e.target.value)}
                      className="p-2.5 bg-white border border-gray-200 rounded-xl text-xs focus:outline-none focus:ring-1 focus:ring-[var(--color-brand)]"
                    />
                  </div>
                  <div className="grid grid-cols-2 gap-2">
                    <input
                      type="text"
                      required
                      placeholder="Area / Landmark"
                      value={newArea}
                      onChange={(e) => setNewArea(e.target.value)}
                      className="p-2.5 bg-white border border-gray-200 rounded-xl text-xs focus:outline-none focus:ring-1 focus:ring-[var(--color-brand)]"
                    />
                    <input
                      type="text"
                      required
                      placeholder="Pincode"
                      value={newPincode}
                      onChange={(e) => setNewPincode(e.target.value)}
                      className="p-2.5 bg-white border border-gray-200 rounded-xl text-xs focus:outline-none focus:ring-1 focus:ring-[var(--color-brand)]"
                    />
                  </div>
                  <div className="flex items-center gap-2">
                    {(['Home', 'Work', 'Other'] as const).map((t) => (
                      <button
                        key={t}
                        type="button"
                        onClick={() => setNewType(t)}
                        className={`px-3 py-1 rounded-lg text-xs font-semibold border cursor-pointer ${
                          newType === t
                            ? 'bg-[var(--color-brand-light)] border-[var(--color-brand)] text-[var(--color-brand-hover)] font-bold'
                            : 'border-gray-200 text-gray-600 bg-white'
                        }`}
                      >
                        {t}
                      </button>
                    ))}
                    <button
                      type="submit"
                      className="ml-auto px-4 py-1.5 bg-[var(--color-brand)] text-white font-bold text-xs rounded-xl shadow-xs cursor-pointer hover:bg-[var(--color-brand-hover)]"
                    >
                      Save & Select
                    </button>
                  </div>
                </form>
              ) : (
                /* Address Cards List */
                <div className="space-y-2.5">
                  {user?.addresses && user.addresses.length > 0 ? (
                    user.addresses.map((addr) => {
                      const isSelected = selectedAddress?.id === addr.id;
                      return (
                        <div
                          key={addr.id}
                          onClick={() => setSelectedAddress(addr)}
                          className={`p-4 rounded-2xl border cursor-pointer transition-all flex items-start justify-between ${
                            isSelected
                              ? 'border-[var(--color-brand)] bg-[var(--color-brand-light)]/60 shadow-xs ring-1 ring-[var(--color-brand)]/30'
                              : 'border-gray-200 hover:border-gray-300 bg-white'
                          }`}
                        >
                          <div className="flex items-start gap-3">
                            <div className="w-8 h-8 rounded-xl bg-gray-100 flex items-center justify-center text-gray-600 mt-0.5">
                              <MapPin className="w-4 h-4 text-[var(--color-brand)]" />
                            </div>
                            <div>
                              <div className="flex items-center gap-2">
                                <span className="font-bold text-xs text-gray-900">{addr.type}</span>
                                {addr.isDefault && (
                                  <span className="text-[10px] bg-gray-100 text-gray-600 px-1.5 py-0.2 rounded font-medium">
                                    Default
                                  </span>
                                )}
                              </div>
                              <p className="text-xs text-gray-600 mt-0.5">
                                {addr.house}, {addr.street}, {addr.area}
                              </p>
                              <p className="text-[11px] text-gray-400">
                                {addr.city}, {addr.state} - {addr.pincode}
                              </p>
                            </div>
                          </div>

                          <div className={`w-5 h-5 rounded-full border flex items-center justify-center ${isSelected ? 'border-[var(--color-brand)] bg-[var(--color-brand)] text-white' : 'border-gray-300'}`}>
                            {isSelected && <Check className="w-3 h-3 stroke-[3]" />}
                          </div>
                        </div>
                      );
                    })
                  ) : (
                    <div className="text-center py-6 border border-dashed border-gray-300 rounded-2xl p-4">
                      <p className="text-xs text-gray-500 mb-2">No addresses saved yet</p>
                      <button
                        onClick={() => setIsAddingNewAddress(true)}
                        className="px-4 py-2 bg-[var(--color-brand)] text-white text-xs font-bold rounded-xl cursor-pointer hover:bg-[var(--color-brand-hover)]"
                      >
                        Add Address Now
                      </button>
                    </div>
                  )}
                </div>
              )}
            </div>
          )}

          {/* STEP 3: Payment & Summary */}
          {currentStep === 3 && (
            <div className="space-y-5">
              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-gray-500 mb-2">
                  Select Payment Method
                </label>
                <p className="text-xs text-gray-500 mb-3">Pay cash after the service. Online payments are not available yet.</p>
                <div className="grid grid-cols-3 gap-3">
                  <button
                    type="button"
                    disabled title="Online payments are not configured"
                    className={`p-3.5 rounded-2xl border text-left transition-all cursor-pointer ${
                      paymentMethod === 'UPI'
                        ? 'border-[var(--color-brand)] bg-[var(--color-brand-light)] text-[var(--color-ink)] font-bold shadow-xs'
                        : 'border-gray-200 text-gray-700 hover:bg-gray-50'
                    }`}
                  >
                    <Smartphone className="w-5 h-5 text-[var(--color-brand)] mb-1" />
                    <span className="text-xs font-bold block">Instant UPI</span>
                    <span className="text-[10px] text-gray-500">GPay, PhonePe, Paytm</span>
                  </button>

                  <button
                    type="button"
                    disabled title="Online payments are not configured"
                    className={`p-3.5 rounded-2xl border text-left transition-all cursor-pointer ${
                      paymentMethod === 'CARD'
                        ? 'border-[var(--color-brand)] bg-[var(--color-brand-light)] text-[var(--color-ink)] font-bold shadow-xs'
                        : 'border-gray-200 text-gray-700 hover:bg-gray-50'
                    }`}
                  >
                    <CreditCard className="w-5 h-5 text-brand mb-1" />
                    <span className="text-xs font-bold block">Cards / Netbanking</span>
                    <span className="text-[10px] text-gray-500">All Indian Banks</span>
                  </button>

                  <button
                    type="button"
                    onClick={() => setPaymentMethod('COD')}
                    className={`p-3.5 rounded-2xl border text-left transition-all cursor-pointer ${
                      paymentMethod === 'COD'
                        ? 'border-[var(--color-brand)] bg-[var(--color-brand-light)] text-[var(--color-ink)] font-bold shadow-xs'
                        : 'border-gray-200 text-gray-700 hover:bg-gray-50'
                    }`}
                  >
                    <Banknote className="w-5 h-5 text-brand mb-1" />
                    <span className="text-xs font-bold block">Pay After Service</span>
                    <span className="text-[10px] text-gray-500">Cash or UPI at doorstep</span>
                  </button>
                </div>
              </div>

              {/* Order Summary Recap */}
              <div className="bg-[var(--color-brand-soft)]/60 rounded-2xl p-4 border border-brand-light space-y-2">
                <h4 className="font-bold text-xs text-gray-900 mb-2">Final Booking Recap</h4>
                <div className="flex justify-between text-xs text-gray-600">
                  <span>Selected Slot</span>
                  <span className="font-semibold text-gray-900">
                    {bookingDate} ({bookingTimeSlot})
                  </span>
                </div>
                <div className="flex justify-between text-xs text-gray-600">
                  <span>Delivery Address</span>
                  <span className="font-semibold text-gray-900 truncate max-w-xs text-right">
                    {selectedAddress?.house}, {selectedAddress?.area}, {selectedAddress?.city}
                  </span>
                </div>
                {appliedCoupon && (
                  <div className="flex justify-between text-xs text-brand font-semibold">
                    <span>Coupon Applied ({appliedCoupon.code})</span>
                    <span>-₹{discount}</span>
                  </div>
                )}
                <div className="flex justify-between text-xs text-gray-600">
                  <span>Additional taxes</span>
                  <span>₹{taxes}</span>
                </div>
                <div className="pt-2 border-t border-gray-200 flex justify-between font-bold text-sm text-gray-900">
                  <span>Final Total</span>
                  <span className="text-[var(--color-brand)] text-base font-black">₹{total}</span>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Footer Navigation */}
        <div className="px-6 py-4 bg-gray-50 border-t border-gray-100 flex items-center justify-between">
          {currentStep > 1 ? (
            <button
              onClick={() => setCurrentStep((prev) => (prev - 1) as any)}
              className="flex items-center gap-1.5 px-4 py-2 border border-gray-200 bg-white hover:bg-gray-100 text-gray-700 font-semibold text-xs rounded-xl transition-all cursor-pointer"
            >
              <ArrowLeft className="w-3.5 h-3.5" />
              <span>Back</span>
            </button>
          ) : (
            <div />
          )}

          {currentStep < 3 ? (
            <button
              onClick={() => setCurrentStep((prev) => (prev + 1) as any)}
              className="flex items-center gap-1.5 px-6 py-2.5 bg-[var(--color-brand)] hover:bg-[var(--color-brand-hover)] text-white font-bold text-xs rounded-xl transition-all shadow-sm cursor-pointer"
            >
              <span>Continue</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </button>
          ) : (
            <button
              onClick={handlePlaceOrder}
              disabled={isSubmitting}
              className="flex items-center gap-1.5 px-7 py-3 bg-[var(--color-brand)] hover:bg-[var(--color-brand-hover)] text-white font-bold text-sm rounded-xl transition-all shadow-md shadow-brand/20 active:scale-[0.98] disabled:opacity-50 cursor-pointer"
            >
              <ShieldCheck className="w-4 h-4" />
              <span>{isSubmitting ? 'Confirming Booking...' : `Confirm & Book for ₹${total}`}</span>
            </button>
          )}
        </div>
      </div>
    </div>
  );
};
