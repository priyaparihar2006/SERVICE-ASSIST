import React, { useState } from 'react';
import { useCart } from '../../context/CartContext';
import { useAuth } from '../../context/AuthContext';
import { useLocation } from '../../context/LocationContext';
import { Booking } from '../../types';
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

  const { user, addAddress } = useAuth();
  const { selectedCity } = useLocation();

  const [currentStep, setCurrentStep] = useState<1 | 2 | 3>(1);
  const [paymentMethod, setPaymentMethod] = useState<'UPI' | 'CARD' | 'COD'>('UPI');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [confirmedBooking, setConfirmedBooking] = useState<Booking | null>(null);

  // Address creation state
  const [isAddingNewAddress, setIsAddingNewAddress] = useState(false);
  const [newHouse, setNewHouse] = useState('');
  const [newStreet, setNewStreet] = useState('');
  const [newArea, setNewArea] = useState('');
  const [newPincode, setNewPincode] = useState('282001');
  const [newType, setNewType] = useState<'Home' | 'Work' | 'Other'>('Home');

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
  };

  const handlePlaceOrder = async () => {
    if (!selectedAddress) {
      alert('Please select or add a delivery address');
      setCurrentStep(2);
      return;
    }

    setIsSubmitting(true);
    try {
      const payload = {
        userId: user?.id || 'usr-customer-1',
        userName: user?.name || 'Priya Sharma',
        userPhone: user?.phone || '+91 98765 12345',
        userEmail: user?.email || 'priya.sharma@example.com',
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

      const res = await fetch('/api/bookings', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });

      const data = await res.json();
      if (data.booking) {
        setConfirmedBooking(data.booking);
        clearCart();
        onSuccess(data.booking);
      }
    } catch (err) {
      console.error(err);
      alert('Failed to place booking. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  // If order confirmed, show Success Screen
  if (confirmedBooking) {
    return (
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-in fade-in duration-200">
        <div className="relative w-full max-w-lg bg-white rounded-3xl shadow-2xl border border-gray-100 p-6 sm:p-8 text-center overflow-hidden">
          <div className="w-16 h-16 rounded-full bg-emerald-100 text-emerald-600 flex items-center justify-center mx-auto mb-4 animate-bounce">
            <Check className="w-8 h-8 stroke-[3]" />
          </div>

          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-[#FFF1E5] text-[#E85D04] text-xs font-bold mb-2">
            <Sparkles className="w-3.5 h-3.5 text-[#FF7A00]" />
            <span>Booking Confirmed!</span>
          </div>

          <h3 className="text-2xl font-black text-gray-900 tracking-tight mb-1 font-['Outfit']">
            We've Received Your Booking
          </h3>
          <p className="text-xs text-gray-500 mb-6">
            A verified professional has been assigned to your Service Assist request.
          </p>

          {/* Details Card */}
          <div className="bg-[#FFF8F2]/60 rounded-2xl p-4 text-left space-y-2.5 text-xs mb-6 border border-orange-100">
            <div className="flex justify-between pb-2 border-b border-gray-200/60">
              <span className="text-gray-500 font-medium">Booking Reference</span>
              <span className="font-bold text-gray-900">{confirmedBooking.id}</span>
            </div>
            <div className="flex justify-between pb-2 border-b border-gray-200/60">
              <span className="text-gray-500 font-medium">Scheduled Time</span>
              <span className="font-bold text-[#E85D04]">
                {confirmedBooking.scheduledDate} ({confirmedBooking.scheduledTimeSlot})
              </span>
            </div>
            <div className="flex justify-between pb-2 border-b border-gray-200/60">
              <span className="text-gray-500 font-medium">Assigned Professional</span>
              <span className="font-bold text-gray-900">{confirmedBooking.professionalName}</span>
            </div>
            <div className="flex justify-between pb-2 border-b border-gray-200/60">
              <span className="text-gray-500 font-medium">Service Verification OTP</span>
              <span className="font-mono font-extrabold text-sm text-[#FF7A00] bg-[#FFF1E5] px-2 py-0.5 rounded">
                {confirmedBooking.verificationOtp}
              </span>
            </div>
            <div className="flex justify-between pt-1 font-bold text-sm text-gray-900">
              <span>Total Payable</span>
              <span className="text-[#FF7A00] font-black">₹{confirmedBooking.total} ({confirmedBooking.paymentMethod})</span>
            </div>
          </div>

          <button
            onClick={() => {
              setConfirmedBooking(null);
              closeCheckoutModal();
            }}
            className="w-full py-3 bg-[#FF7A00] hover:bg-[#E85D04] text-white font-bold text-sm rounded-xl transition-all shadow-md shadow-orange-500/20 cursor-pointer"
          >
            Done & View My Bookings
          </button>
        </div>
      </div>
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
                    ? 'bg-[#FF7A00] text-white shadow-xs'
                    : currentStep > step
                    ? 'bg-[#FFF1E5] text-[#E85D04]'
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
                            ? 'border-[#FF7A00] bg-[#FFF1E5] text-[#15252B] font-bold shadow-xs'
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
                            ? 'border-[#FF7A00] bg-[#FFF1E5] text-[#E85D04] font-bold shadow-xs'
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
                  className="w-full p-3 bg-gray-50 border border-gray-200 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-[#FF7A00]/30 focus:border-[#FF7A00]"
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
                  className="text-xs font-bold text-[#FF7A00] hover:underline flex items-center gap-1 cursor-pointer"
                >
                  <Plus className="w-3.5 h-3.5" />
                  <span>{isAddingNewAddress ? 'Cancel' : 'Add New Address'}</span>
                </button>
              </div>

              {/* Add New Address Form */}
              {isAddingNewAddress ? (
                <form onSubmit={handleSaveAddress} className="p-4 bg-[#FFF8F2]/60 rounded-2xl border border-orange-200/80 space-y-3">
                  <h4 className="font-bold text-xs text-gray-800">Add New Address ({selectedCity.name})</h4>
                  <div className="grid grid-cols-2 gap-2">
                    <input
                      type="text"
                      required
                      placeholder="House / Flat / Block No."
                      value={newHouse}
                      onChange={(e) => setNewHouse(e.target.value)}
                      className="p-2.5 bg-white border border-gray-200 rounded-xl text-xs focus:outline-none focus:ring-1 focus:ring-[#FF7A00]"
                    />
                    <input
                      type="text"
                      required
                      placeholder="Street / Road / Colony"
                      value={newStreet}
                      onChange={(e) => setNewStreet(e.target.value)}
                      className="p-2.5 bg-white border border-gray-200 rounded-xl text-xs focus:outline-none focus:ring-1 focus:ring-[#FF7A00]"
                    />
                  </div>
                  <div className="grid grid-cols-2 gap-2">
                    <input
                      type="text"
                      required
                      placeholder="Area / Landmark"
                      value={newArea}
                      onChange={(e) => setNewArea(e.target.value)}
                      className="p-2.5 bg-white border border-gray-200 rounded-xl text-xs focus:outline-none focus:ring-1 focus:ring-[#FF7A00]"
                    />
                    <input
                      type="text"
                      required
                      placeholder="Pincode"
                      value={newPincode}
                      onChange={(e) => setNewPincode(e.target.value)}
                      className="p-2.5 bg-white border border-gray-200 rounded-xl text-xs focus:outline-none focus:ring-1 focus:ring-[#FF7A00]"
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
                            ? 'bg-[#FFF1E5] border-[#FF7A00] text-[#E85D04] font-bold'
                            : 'border-gray-200 text-gray-600 bg-white'
                        }`}
                      >
                        {t}
                      </button>
                    ))}
                    <button
                      type="submit"
                      className="ml-auto px-4 py-1.5 bg-[#FF7A00] text-white font-bold text-xs rounded-xl shadow-xs cursor-pointer hover:bg-[#E85D04]"
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
                              ? 'border-[#FF7A00] bg-[#FFF1E5]/60 shadow-xs ring-1 ring-[#FF7A00]/30'
                              : 'border-gray-200 hover:border-gray-300 bg-white'
                          }`}
                        >
                          <div className="flex items-start gap-3">
                            <div className="w-8 h-8 rounded-xl bg-gray-100 flex items-center justify-center text-gray-600 mt-0.5">
                              <MapPin className="w-4 h-4 text-[#FF7A00]" />
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

                          <div className={`w-5 h-5 rounded-full border flex items-center justify-center ${isSelected ? 'border-[#FF7A00] bg-[#FF7A00] text-white' : 'border-gray-300'}`}>
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
                        className="px-4 py-2 bg-[#FF7A00] text-white text-xs font-bold rounded-xl cursor-pointer hover:bg-[#E85D04]"
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
                <div className="grid grid-cols-3 gap-3">
                  <button
                    type="button"
                    onClick={() => setPaymentMethod('UPI')}
                    className={`p-3.5 rounded-2xl border text-left transition-all cursor-pointer ${
                      paymentMethod === 'UPI'
                        ? 'border-[#FF7A00] bg-[#FFF1E5] text-[#15252B] font-bold shadow-xs'
                        : 'border-gray-200 text-gray-700 hover:bg-gray-50'
                    }`}
                  >
                    <Smartphone className="w-5 h-5 text-[#FF7A00] mb-1" />
                    <span className="text-xs font-bold block">Instant UPI</span>
                    <span className="text-[10px] text-gray-500">GPay, PhonePe, Paytm</span>
                  </button>

                  <button
                    type="button"
                    onClick={() => setPaymentMethod('CARD')}
                    className={`p-3.5 rounded-2xl border text-left transition-all cursor-pointer ${
                      paymentMethod === 'CARD'
                        ? 'border-[#FF7A00] bg-[#FFF1E5] text-[#15252B] font-bold shadow-xs'
                        : 'border-gray-200 text-gray-700 hover:bg-gray-50'
                    }`}
                  >
                    <CreditCard className="w-5 h-5 text-indigo-600 mb-1" />
                    <span className="text-xs font-bold block">Cards / Netbanking</span>
                    <span className="text-[10px] text-gray-500">All Indian Banks</span>
                  </button>

                  <button
                    type="button"
                    onClick={() => setPaymentMethod('COD')}
                    className={`p-3.5 rounded-2xl border text-left transition-all cursor-pointer ${
                      paymentMethod === 'COD'
                        ? 'border-[#FF7A00] bg-[#FFF1E5] text-[#15252B] font-bold shadow-xs'
                        : 'border-gray-200 text-gray-700 hover:bg-gray-50'
                    }`}
                  >
                    <Banknote className="w-5 h-5 text-emerald-600 mb-1" />
                    <span className="text-xs font-bold block">Pay After Service</span>
                    <span className="text-[10px] text-gray-500">Cash or UPI at doorstep</span>
                  </button>
                </div>
              </div>

              {/* Order Summary Recap */}
              <div className="bg-[#FFF8F2]/60 rounded-2xl p-4 border border-orange-100 space-y-2">
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
                  <div className="flex justify-between text-xs text-emerald-600 font-semibold">
                    <span>Coupon Applied ({appliedCoupon.code})</span>
                    <span>-₹{discount}</span>
                  </div>
                )}
                <div className="flex justify-between text-xs text-gray-600">
                  <span>Taxes (5% GST)</span>
                  <span>₹{taxes}</span>
                </div>
                <div className="pt-2 border-t border-gray-200 flex justify-between font-bold text-sm text-gray-900">
                  <span>Final Total</span>
                  <span className="text-[#FF7A00] text-base font-black">₹{total}</span>
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
              className="flex items-center gap-1.5 px-6 py-2.5 bg-[#FF7A00] hover:bg-[#E85D04] text-white font-bold text-xs rounded-xl transition-all shadow-sm cursor-pointer"
            >
              <span>Continue</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </button>
          ) : (
            <button
              onClick={handlePlaceOrder}
              disabled={isSubmitting}
              className="flex items-center gap-1.5 px-7 py-3 bg-[#FF7A00] hover:bg-[#E85D04] text-white font-bold text-sm rounded-xl transition-all shadow-md shadow-orange-500/20 active:scale-[0.98] disabled:opacity-50 cursor-pointer"
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
