import React, { useState, useEffect } from 'react';
import { 
  X, 
  Clock, 
  MapPin, 
  ShieldCheck, 
  CreditCard, 
  Lock, 
  CheckCircle2, 
  Star, 
  PhoneCall, 
  MessageSquare, 
  ArrowRight,
  AlertCircle,
  Tag,
  Compass
} from 'lucide-react';
import { CartItem, ServiceItem, UserProfile } from '../../types/landing';
import { OFFERS, CITIES } from '../../data/landing/servicesData';
import { Tilt3DCard } from './Tilt3DCard';

interface BookingModalProps {
  isOpen: boolean;
  onClose: () => void;
  cartItems: CartItem[];
  currentCity: string;
  onClearCart: () => void;
  initialService?: ServiceItem | null;
  currentUser?: UserProfile | null;
  onRequireAuth?: () => void;
}

export const BookingModal: React.FC<BookingModalProps> = ({
  isOpen,
  onClose,
  cartItems,
  currentCity,
  onClearCart,
  initialService,
  currentUser,
  onRequireAuth,
}) => {
  const [items, setItems] = useState<CartItem[]>([]);
  const [step, setStep] = useState<'details' | 'tracking'>('details');

  // Form states
  const [slot, setSlot] = useState('instant');
  const [name, setName] = useState(currentUser?.name || 'Aditi Sharma');
  const [phone, setPhone] = useState(currentUser?.phone || '9876543210');
  const [flat, setFlat] = useState('Flat 402, Tower B');
  const [street, setStreet] = useState('Palm Grove Heights, Sector 45');
  const [pincode, setPincode] = useState('110001');
  const [paymentMethod, setPaymentMethod] = useState<'cash' | 'upi' | 'card'>('cash');
  const [couponCode, setCouponCode] = useState('FIRST50');
  const [couponDiscount, setCouponDiscount] = useState(150);

  // Dispatch Tracking states
  const [bookingId, setBookingId] = useState('SA-829143');
  const [otp, setOtp] = useState('4829');
  const [etaMinutes, setEtaMinutes] = useState(14);
  const [chatOpen, setChatOpen] = useState(false);
  const [chatMessages, setChatMessages] = useState<Array<{ sender: 'pro' | 'user'; text: string }>>([
    { sender: 'pro', text: 'Namaste! I am Ramesh, your assigned Service Assist technician. I have picked up the tools and will reach your doorstep in ~14 mins.' }
  ]);
  const [inputChat, setInputChat] = useState('');

  useEffect(() => {
    if (cartItems.length > 0) {
      setItems(cartItems);
    } else if (initialService) {
      setItems([{ service: initialService, quantity: 1 }]);
    }
  }, [cartItems, initialService]);

  useEffect(() => {
    if (currentUser) {
      if (currentUser.name) setName(currentUser.name);
      if (currentUser.phone) setPhone(currentUser.phone);
    }
  }, [currentUser]);

  // Live ETA countdown timer simulation
  useEffect(() => {
    if (step === 'tracking' && etaMinutes > 1) {
      const interval = setInterval(() => {
        setEtaMinutes((prev) => Math.max(1, prev - 1));
      }, 15000);
      return () => clearInterval(interval);
    }
  }, [step, etaMinutes]);

  if (!isOpen) return null;

  const subtotal = items.reduce((acc, item) => acc + item.service.price * item.quantity, 0);
  const total = Math.max(0, subtotal - couponDiscount);

  const handleApplyCoupon = (code: string) => {
    const found = OFFERS.find((o) => o.code.toUpperCase() === code.trim().toUpperCase());
    if (found) {
      if (found.discount) {
        setCouponDiscount(found.discount);
      } else if (found.discountPercent) {
        setCouponDiscount(Math.min(found.maxDiscount || 400, Math.round(subtotal * (found.discountPercent / 100))));
      }
      setCouponCode(found.code);
    } else {
      setCouponDiscount(0);
    }
  };

  const handleConfirmBooking = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!currentUser) {
      if (onRequireAuth) {
        onRequireAuth();
        return;
      }
    }
    try {
      const res = await fetch('/api/bookings', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          service: items,
          customer: { name, phone },
          address: { flat, street, city: currentCity, pincode },
          slot,
          paymentMethod,
          total,
        }),
      });
      const data = await res.json();
      if (data.bookingId) {
        setBookingId(data.bookingId);
        setOtp(data.otp);
      }
    } catch {
      setBookingId('SA-' + Math.floor(100000 + Math.random() * 900000));
      setOtp('4829');
    }
    setStep('tracking');
  };

  const handleSendChat = (e: React.FormEvent) => {
    e.preventDefault();
    if (!inputChat.trim()) return;
    const msg = inputChat;
    setChatMessages((prev) => [...prev, { sender: 'user', text: msg }]);
    setInputChat('');
    setTimeout(() => {
      setChatMessages((prev) => [
        ...prev,
        { sender: 'pro', text: 'Got it! I am carrying all certified tools and genuine replacement spares.' }
      ]);
    }, 1500);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-slate-950/75 backdrop-blur-sm animate-in fade-in duration-150">
      <div className="bg-white rounded-3xl max-w-2xl w-full max-h-[92vh] overflow-y-auto shadow-2xl relative border border-slate-100 flex flex-col">
        
        {/* Modal Header */}
        <div className="p-5 sm:p-6 border-b border-slate-100 flex items-center justify-between sticky top-0 bg-white/95 backdrop-blur-sm z-20">
          <div>
            <div className="inline-flex items-center gap-1.5 text-[11px] font-bold uppercase tracking-wider text-emerald-700">
              <ShieldCheck className="w-3.5 h-3.5" />
              {step === 'details' ? 'Easy Doorstep Booking' : '3D Live Technician Dispatch'}
            </div>
            <h3 className="text-xl sm:text-2xl font-extrabold text-slate-900 font-display">
              {step === 'details' ? 'Complete Your Booking' : 'Technician is On the Way!'}
            </h3>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-full hover:bg-slate-100 text-slate-400 hover:text-slate-700 transition-colors cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Modal Body */}
        <div className="p-5 sm:p-6 space-y-6 flex-1">
          {step === 'details' ? (
            <form onSubmit={handleConfirmBooking} className="space-y-6">
              
              {/* Simple Explainer */}
              <div className="p-3 bg-emerald-50 rounded-xl border border-emerald-200/80 text-xs text-emerald-900 flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
                <span>Zero advance payment required. Pay only after your service is completed and inspected.</span>
              </div>

              {/* 1. Selected Services Review */}
              <div className="bg-slate-50/80 rounded-2xl p-4 border border-slate-200">
                <span className="text-xs font-bold text-slate-500 uppercase tracking-wider block mb-3">
                  Selected Packages ({items.length})
                </span>
                <div className="space-y-2">
                  {items.map((it, idx) => (
                    <div key={idx} className="flex items-center justify-between text-xs bg-white p-3 rounded-xl border border-slate-200/80">
                      <div>
                        <h4 className="font-bold text-slate-900">{it.service.title}</h4>
                        <p className="text-[11px] text-slate-500">
                          ~{it.service.durationMinutes} mins · {it.service.warrantyDays}-day warranty
                        </p>
                      </div>
                      <div className="text-right">
                        <span className="font-mono font-bold text-slate-900 text-sm">
                          ₹{it.service.price * it.quantity}
                        </span>
                        {it.quantity > 1 && (
                          <span className="block text-[10px] text-slate-400">Qty: {it.quantity}</span>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              {/* 2. Timing Slot Selection */}
              <div>
                <label className="text-xs font-bold text-slate-700 block mb-2">
                  When Should the Pro Arrive?
                </label>
                <div className="grid grid-cols-2 sm:grid-cols-3 gap-2.5">
                  <button
                    type="button"
                    onClick={() => setSlot('instant')}
                    className={`p-3 rounded-xl border text-left transition-all cursor-pointer ${
                      slot === 'instant'
                        ? 'bg-emerald-50 border-emerald-500 ring-2 ring-emerald-500/20'
                        : 'bg-white hover:bg-slate-50 border-slate-200'
                    }`}
                  >
                    <span className="flex items-center gap-1 text-[11px] font-bold text-emerald-700">
                      <Clock className="w-3.5 h-3.5" /> Fast Dispatch
                    </span>
                    <p className="text-xs font-bold text-slate-900 mt-1">Instant (15 mins)</p>
                  </button>

                  <button
                    type="button"
                    onClick={() => setSlot('today-afternoon')}
                    className={`p-3 rounded-xl border text-left transition-all cursor-pointer ${
                      slot === 'today-afternoon'
                        ? 'bg-emerald-50 border-emerald-500 ring-2 ring-emerald-500/20'
                        : 'bg-white hover:bg-slate-50 border-slate-200'
                    }`}
                  >
                    <span className="text-[11px] font-medium text-slate-500">Today</span>
                    <p className="text-xs font-bold text-slate-900 mt-1">2:00 PM - 4:00 PM</p>
                  </button>

                  <button
                    type="button"
                    onClick={() => setSlot('tomorrow-morning')}
                    className={`p-3 rounded-xl border text-left transition-all cursor-pointer ${
                      slot === 'tomorrow-morning'
                        ? 'bg-emerald-50 border-emerald-500 ring-2 ring-emerald-500/20'
                        : 'bg-white hover:bg-slate-50 border-slate-200'
                    }`}
                  >
                    <span className="text-[11px] font-medium text-slate-500">Tomorrow</span>
                    <p className="text-xs font-bold text-slate-900 mt-1">10:00 AM - 12:00 PM</p>
                  </button>
                </div>
              </div>

              {/* 3. Address & Contact Details */}
              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <label className="text-xs font-bold text-slate-700 block">
                    Service Address &amp; Customer Details
                  </label>
                  {currentUser ? (
                    <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-full border border-emerald-200">
                      <ShieldCheck className="w-3.5 h-3.5 text-emerald-600" />
                      Verified Account
                    </span>
                  ) : (
                    <button
                      type="button"
                      onClick={onRequireAuth}
                      className="text-[11px] font-bold text-emerald-600 hover:text-emerald-700 underline cursor-pointer"
                    >
                      Sign In Required
                    </button>
                  )}
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <div>
                    <input
                      type="text"
                      required
                      value={name}
                      onChange={(e) => setName(e.target.value)}
                      placeholder="Your Full Name"
                      className="w-full bg-slate-50 border border-slate-200 rounded-xl py-2 px-3 text-xs text-slate-800 focus:outline-emerald-500"
                    />
                  </div>
                  <div>
                    <input
                      type="tel"
                      required
                      value={phone}
                      onChange={(e) => setPhone(e.target.value)}
                      placeholder="Mobile Number (+91)"
                      className="w-full bg-slate-50 border border-slate-200 rounded-xl py-2 px-3 text-xs text-slate-800 focus:outline-emerald-500"
                    />
                  </div>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                  <div className="sm:col-span-2">
                    <input
                      type="text"
                      required
                      value={flat}
                      onChange={(e) => setFlat(e.target.value)}
                      placeholder="Flat / House / Floor No."
                      className="w-full bg-slate-50 border border-slate-200 rounded-xl py-2 px-3 text-xs text-slate-800 focus:outline-emerald-500"
                    />
                  </div>
                  <div>
                    <input
                      type="text"
                      required
                      value={pincode}
                      onChange={(e) => setPincode(e.target.value)}
                      placeholder="Pincode"
                      maxLength={6}
                      className="w-full bg-slate-50 border border-slate-200 rounded-xl py-2 px-3 text-xs text-slate-800 focus:outline-emerald-500"
                    />
                  </div>
                </div>

                <div>
                  <input
                    type="text"
                    required
                    value={street}
                    onChange={(e) => setStreet(e.target.value)}
                    placeholder="Apartment name, Street, Landmark"
                    className="w-full bg-slate-50 border border-slate-200 rounded-xl py-2 px-3 text-xs text-slate-800 focus:outline-emerald-500"
                  />
                </div>
              </div>

              {/* 4. Voucher Discount Code */}
              <div>
                <label className="text-xs font-bold text-slate-700 block mb-1">
                  Apply Discount Voucher
                </label>
                <div className="flex gap-2">
                  <div className="relative flex-1">
                    <Tag className="w-3.5 h-3.5 text-slate-400 absolute left-3 top-3" />
                    <input
                      type="text"
                      value={couponCode}
                      onChange={(e) => setCouponCode(e.target.value.toUpperCase())}
                      placeholder="Enter promo code (e.g. FIRST50)"
                      className="w-full bg-slate-50 border border-slate-200 rounded-xl py-2 pl-9 pr-3 text-xs uppercase font-mono font-bold text-slate-800 focus:outline-emerald-500"
                    />
                  </div>
                  <button
                    type="button"
                    onClick={() => handleApplyCoupon(couponCode)}
                    className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-white rounded-xl text-xs font-bold cursor-pointer"
                  >
                    Apply
                  </button>
                </div>
                {couponDiscount > 0 && (
                  <p className="text-[11px] text-emerald-600 font-semibold mt-1">
                    ✓ Code applied! You saved ₹{couponDiscount} on this booking.
                  </p>
                )}
              </div>

              {/* 5. Payment Mode */}
              <div>
                <label className="text-xs font-bold text-slate-700 block mb-2">
                  Payment Method
                </label>
                <div className="grid grid-cols-3 gap-2 text-xs">
                  <button
                    type="button"
                    onClick={() => setPaymentMethod('cash')}
                    className={`p-3 rounded-xl border text-center transition-all cursor-pointer ${
                      paymentMethod === 'cash'
                        ? 'bg-emerald-50 border-emerald-500 font-bold text-emerald-800'
                        : 'bg-white border-slate-200 text-slate-600'
                    }`}
                  >
                    Pay Post Work (Cash/UPI)
                  </button>

                  <button
                    type="button"
                    onClick={() => setPaymentMethod('upi')}
                    className={`p-3 rounded-xl border text-center transition-all cursor-pointer ${
                      paymentMethod === 'upi'
                        ? 'bg-emerald-50 border-emerald-500 font-bold text-emerald-800'
                        : 'bg-white border-slate-200 text-slate-600'
                    }`}
                  >
                    Instant UPI (GPay/PhonePe)
                  </button>

                  <button
                    type="button"
                    onClick={() => setPaymentMethod('card')}
                    className={`p-3 rounded-xl border text-center transition-all cursor-pointer ${
                      paymentMethod === 'card'
                        ? 'bg-emerald-50 border-emerald-500 font-bold text-emerald-800'
                        : 'bg-white border-slate-200 text-slate-600'
                    }`}
                  >
                    Credit / Debit Card
                  </button>
                </div>
              </div>

              {/* Price Summary Breakdown */}
              <div className="pt-3 border-t border-slate-200/80 space-y-1.5 text-xs">
                <div className="flex justify-between text-slate-500">
                  <span>Subtotal</span>
                  <span className="font-mono">₹{subtotal}</span>
                </div>
                {couponDiscount > 0 && (
                  <div className="flex justify-between text-emerald-600 font-semibold">
                    <span>Voucher Discount</span>
                    <span className="font-mono">-₹{couponDiscount}</span>
                  </div>
                )}
                <div className="flex justify-between text-slate-500">
                  <span>Safety Inspection &amp; Insurance</span>
                  <span className="font-mono text-emerald-600">FREE</span>
                </div>
                <div className="flex justify-between text-base font-extrabold text-slate-900 pt-2 border-t border-slate-100">
                  <span>Total Amount</span>
                  <span className="font-mono text-emerald-700">₹{total}</span>
                </div>
              </div>

              {/* Submit CTA */}
              <button
                type="submit"
                className="w-full py-3.5 bg-emerald-600 hover:bg-emerald-700 active:bg-emerald-800 text-white rounded-xl text-sm font-bold shadow-lg shadow-emerald-600/30 transition-all flex items-center justify-center gap-2 cursor-pointer"
              >
                <span>Confirm &amp; Dispatch Pro</span>
                <ArrowRight className="w-4 h-4" />
              </button>

            </form>
          ) : (
            /* LIVE 3D TRACKING & DISPATCH SCREEN */
            <div className="space-y-6 animate-in fade-in duration-200">
              
              {/* Top ETA & Safety OTP Card with 3D Tilt */}
              <Tilt3DCard maxTilt={10} perspective={1000} glare={true}>
                <div className="bg-slate-900 text-white rounded-2xl p-5 shadow-xl border border-slate-800">
                  <div className="flex items-center justify-between pb-3 border-b border-slate-800 text-xs">
                    <div className="flex items-center gap-2">
                      <span className="w-2.5 h-2.5 rounded-full bg-emerald-400 animate-ping" />
                      <span className="font-bold text-emerald-400">Pro En Route · Live GPS</span>
                    </div>
                    <span className="font-mono text-slate-400">ID: {bookingId}</span>
                  </div>

                  <div className="py-4 text-center">
                    <span className="text-xs text-slate-400 uppercase tracking-wider block">
                      Estimated Doorstep Arrival
                    </span>
                    <div className="text-4xl font-extrabold text-white mt-1 font-mono tracking-tight flex items-center justify-center gap-2">
                      <Clock className="w-7 h-7 text-emerald-400" />
                      <span>{etaMinutes} Mins</span>
                    </div>
                    <p className="text-xs text-slate-400 mt-1">
                      Technician is on bike with sanitized tool equipment
                    </p>
                  </div>

                  {/* 4-Digit Security OTP Badge */}
                  <div
                    style={{ transform: 'translateZ(25px)' }}
                    className="bg-slate-800/90 rounded-xl p-3 border border-slate-700 flex items-center justify-between shadow-md"
                  >
                    <div className="flex items-center gap-2">
                      <Lock className="w-4 h-4 text-emerald-400" />
                      <span className="text-xs text-slate-300">
                        Share this 4-Digit OTP only when Pro arrives:
                      </span>
                    </div>
                    <div className="font-mono text-xl font-extrabold tracking-widest text-emerald-400 bg-slate-950 px-3 py-1 rounded-lg border border-emerald-500/40">
                      {otp}
                    </div>
                  </div>
                </div>
              </Tilt3DCard>

              {/* Assigned Technician Profile */}
              <div className="bg-slate-50 rounded-2xl p-4 sm:p-5 border border-slate-200">
                <div className="flex items-start justify-between">
                  <div className="flex items-center gap-3">
                    <div className="w-14 h-14 rounded-2xl bg-gradient-to-tr from-emerald-600 to-teal-700 text-white font-extrabold text-lg flex items-center justify-center shadow-md">
                      RK
                    </div>
                    <div>
                      <div className="flex items-center gap-1.5">
                        <h4 className="text-base font-bold text-slate-900">Ramesh Kumar</h4>
                        <span className="text-[10px] bg-emerald-100 text-emerald-800 px-2 py-0.5 rounded-full font-bold">
                          Platinum Pro
                        </span>
                      </div>
                      <p className="text-xs text-slate-500">Certified HVAC &amp; Appliance Specialist</p>
                      <div className="flex items-center gap-2 text-xs mt-1">
                        <span className="flex items-center gap-1 font-bold text-slate-900">
                          <Star className="w-3.5 h-3.5 fill-amber-400 text-amber-500" /> 4.95
                        </span>
                        <span className="text-slate-300">·</span>
                        <span className="text-slate-600">2,410+ jobs completed</span>
                      </div>
                    </div>
                  </div>
                </div>

                {/* Action buttons: Call / Chat */}
                <div className="grid grid-cols-2 gap-3 mt-4 pt-3 border-t border-slate-200">
                  <a
                    href="tel:9876543210"
                    className="py-2.5 px-3 bg-white hover:bg-slate-100 text-slate-800 rounded-xl text-xs font-bold border border-slate-300 text-center flex items-center justify-center gap-1.5 transition-colors"
                  >
                    <PhoneCall className="w-3.5 h-3.5 text-emerald-600" />
                    <span>Call Technician</span>
                  </a>

                  <button
                    type="button"
                    onClick={() => setChatOpen(!chatOpen)}
                    className="py-2.5 px-3 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs font-bold text-center flex items-center justify-center gap-1.5 transition-colors cursor-pointer"
                  >
                    <MessageSquare className="w-3.5 h-3.5" />
                    <span>In-App Chat</span>
                  </button>
                </div>
              </div>

              {/* Chat Drawer if toggled */}
              {chatOpen && (
                <div className="bg-slate-100 rounded-2xl p-3 border border-slate-300 space-y-3">
                  <div className="text-xs font-bold text-slate-700 flex items-center justify-between pb-1 border-b border-slate-200">
                    <span>Chat with Ramesh (Pro)</span>
                    <button onClick={() => setChatOpen(false)} className="text-slate-400 hover:text-slate-700">
                      Close
                    </button>
                  </div>
                  <div className="space-y-2 max-h-40 overflow-y-auto p-1">
                    {chatMessages.map((msg, i) => (
                      <div
                        key={i}
                        className={`text-xs p-2 rounded-xl max-w-[85%] ${
                          msg.sender === 'user'
                            ? 'ml-auto bg-emerald-600 text-white rounded-br-none'
                            : 'bg-white text-slate-800 rounded-bl-none shadow-xs'
                        }`}
                      >
                        {msg.text}
                      </div>
                    ))}
                  </div>
                  <form onSubmit={handleSendChat} className="flex gap-2">
                    <input
                      type="text"
                      value={inputChat}
                      onChange={(e) => setInputChat(e.target.value)}
                      placeholder="Type a message to your pro..."
                      className="flex-1 bg-white border border-slate-200 rounded-xl px-3 py-1.5 text-xs text-slate-800 focus:outline-emerald-500"
                    />
                    <button
                      type="submit"
                      className="px-3 py-1.5 bg-emerald-600 text-white text-xs font-bold rounded-xl"
                    >
                      Send
                    </button>
                  </form>
                </div>
              )}

              {/* Booking Details Summary */}
              <div className="bg-white rounded-2xl p-4 border border-slate-200 text-xs space-y-2">
                <div className="flex justify-between text-slate-600">
                  <span>Destination:</span>
                  <span className="font-semibold text-slate-900">{flat}, {street}</span>
                </div>
                <div className="flex justify-between text-slate-600">
                  <span>Payment Mode:</span>
                  <span className="font-semibold uppercase text-slate-900">{paymentMethod}</span>
                </div>
                <div className="flex justify-between text-slate-600">
                  <span>Payable Post-Service:</span>
                  <span className="font-mono font-bold text-emerald-700 text-sm">₹{total}</span>
                </div>
              </div>

              {/* Action buttons */}
              <div className="flex gap-3">
                <button
                  type="button"
                  onClick={() => {
                    onClearCart();
                    onClose();
                  }}
                  className="w-full py-3 bg-slate-900 hover:bg-slate-800 text-white rounded-xl text-xs font-bold transition-colors cursor-pointer text-center"
                >
                  Return to Home
                </button>
              </div>

            </div>
          )}
        </div>

      </div>
    </div>
  );
};

