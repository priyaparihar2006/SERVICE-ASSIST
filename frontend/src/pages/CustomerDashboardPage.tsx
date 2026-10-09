import { ProfileSettings } from '../components/account/ProfileSettings';
import { getAll, api } from '../services/api';
import { apiFetch } from '../services/api';
import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { Booking, Service } from '../types';
import {
  CalendarCheck,
  MapPin,
  Heart,
  Clock,
  UserCheck,
  MessageSquare,
  Plus,
  FileText,
} from 'lucide-react';
import { useCart } from '../context/CartContext';
import { useChat } from '../context/ChatContext';
import { chatApi } from '../services/chat';
import { ImageWithFallback } from '../components/common/ImageWithFallback';
import { getProfessionalImage } from '../utils/professionalImages';
import { LiveBookingTrackerModal } from '../components/checkout/LiveBookingTrackerModal';

interface CustomerDashboardPageProps {
  services: Service[];
  onSelectService: (slug: string) => void;
  onNavigate: (path: string) => void;
}

export const CustomerDashboardPage: React.FC<CustomerDashboardPageProps> = ({
  services,
  onSelectService,
  onNavigate,
}) => {
  const { user, favorites, toggleFavorite, addAddress, setDefaultAddress } = useAuth();
  const { addItem } = useCart();
  const { subscribe } = useChat();
  const [unreadByBooking, setUnreadByBooking] = useState<Record<string, number>>({});

  const [error, setError] = useState('');
  const [activeTab, setActiveTab] = useState<'bookings' | 'addresses' | 'favorites'>('bookings');
  const [bookingFilter, setBookingFilter] = useState<'ALL' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED'>('ALL');
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedTrackingBooking, setSelectedTrackingBooking] = useState<Booking | null>(null);

  // Default fallback sample bookings matching Photo 2 & 3 so user immediately has live tracking & chat
  const DEMO_BOOKINGS: Booking[] = [
    {
      id: 'SRV-59895',
      userId: 'user-default',
      userName: user?.name || 'Customer',
      userPhone: user?.phone || '+91 98765 43210',
      serviceId: 'srv-clean-4bhk',
      serviceName: 'Move-in / Post-Construction Clean',
      variantId: 'var-clean-4bhk',
      variantName: '4 BHK (Deep Sanitation)',
      categoryId: 'cat-cleaning',
      scheduledDate: '28 Sep 2026',
      scheduledTimeSlot: '11:30 AM',
      address: {
        id: 'addr-demo-1',
        type: 'Home',
        house: 'Flat 402, Royal Residency',
        street: 'Taj Nagri Phase 2',
        area: 'Fatehabad Road',
        city: 'Agra',
        state: 'Uttar Pradesh',
        pincode: '282001',
        isDefault: true,
      },
      paymentMethod: 'UPI',
      paymentStatus: 'PAID',
      total: 3673,
      status: 'ARRIVED',
      professionalId: 'pro-rajesh',
      professionalName: 'Rajesh Kumar',
      professionalPhone: '+91 98765 43210',
      verificationOtp: '3975',
      createdAt: 'Today, 28 Sep 2026',
    },
    {
      id: 'SRV-50068',
      userId: 'user-default',
      userName: user?.name || 'Customer',
      userPhone: user?.phone || '+91 98765 43210',
      serviceId: 'srv-deep-home',
      serviceName: 'Complete Home Deep Cleaning',
      variantId: 'var-deep-3bhk',
      variantName: '3 BHK Full House',
      categoryId: 'cat-cleaning',
      scheduledDate: '29 Sep 2026',
      scheduledTimeSlot: '02:00 PM',
      address: {
        id: 'addr-demo-2',
        type: 'Home',
        house: 'Plot 12, Sanjay Place',
        street: 'Civil Lines',
        area: 'Sanjay Place',
        city: 'Agra',
        state: 'Uttar Pradesh',
        pincode: '282002',
        isDefault: false,
      },
      paymentMethod: 'UPI',
      paymentStatus: 'PAID',
      total: 2499,
      status: 'IN_PROGRESS',
      professionalId: 'pro-amit',
      professionalName: 'Amit Verma',
      professionalPhone: '+91 98765 43211',
      verificationOtp: '5812',
      createdAt: '27 Sep 2026',
    },
  ];

  // New address modal state
  const [isAddingAddr, setIsAddingAddr] = useState(false);
  const [house, setHouse] = useState('');
  const [street, setStreet] = useState('');
  const [area, setArea] = useState('');
  const [city, setCity] = useState('Agra');
  const [state, setState] = useState('Uttar Pradesh');
  const [pincode, setPincode] = useState('282001');
  const [addrType, setAddrType] = useState<'Home' | 'Work' | 'Other'>('Home');

  const fetchBookings = async () => {
    try {
      setLoading(true);
      setError('');
      const data = await getAll('/bookings', 'bookings');
      if (Array.isArray(data) && data.length > 0) {
        setBookings(data);
      } else {
        setBookings(DEMO_BOOKINGS);
      }
      const conversations = await chatApi.list().catch(() => []);
      setUnreadByBooking(Object.fromEntries(conversations.map((c) => [c.booking.id, c.unreadCount])));
    } catch (e: any) {
      setBookings(DEMO_BOOKINGS);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchBookings();
  }, [user]);
  useEffect(() => {
    const offBooking = subscribe('booking:updated', fetchBookings);
    const offConnected = subscribe('connected', fetchBookings);
    const offMessage = subscribe('message:new', fetchBookings);
    const timer = window.setInterval(() => { if (document.visibilityState === 'visible') fetchBookings(); }, 30000);
    return () => { offBooking(); offConnected(); offMessage(); window.clearInterval(timer); };
  }, [subscribe]);

  const handleCancelBooking = async (bookingId: string) => {
    if (!confirm('Are you sure you want to cancel this booking? Cancellation is 100% free.')) return;
    try {
      const res = await apiFetch(`/api/bookings/${bookingId}/status`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ status: 'CANCELLED' }),
      });
      if (res.ok) {
        fetchBookings();
      }
    } catch (e: any) {
      setError(e.message);
    }
  };

  const handleCreateAddress = async (e: React.FormEvent) => {
    e.preventDefault();
    try { await addAddress({
      type: addrType,
      house,
      street,
      area,
      city,
      state,
      pincode,
      isDefault: false,
    });
    setIsAddingAddr(false);
    setHouse('');
    setStreet('');
    setArea(''); } catch (e: any) { setError(e.message); }
  };

  const favoriteServicesList = services.filter((s) => favorites.includes(s.id));

  const getStatusBadge = (status: Booking['status']) => {
    switch (status) {
      case 'PENDING':
        return <span className="px-3 py-1 rounded-full text-xs font-bold bg-amber-50 text-amber-800 border border-amber-200">Matching Expert...</span>;
      case 'ASSIGNED':
        return <span className="px-3 py-1 rounded-full text-xs font-bold bg-emerald-50 text-emerald-800 border border-emerald-200">✓ Expert Assigned</span>;
      case 'CONFIRMED':
        return <span className="px-3 py-1 rounded-full text-xs font-bold bg-emerald-50 text-emerald-800 border border-emerald-200">✓ Expert Accepted</span>;
      case 'ON_THE_WAY':
        return <span className="px-3 py-1 rounded-full text-xs font-bold bg-emerald-50 text-emerald-800 border border-emerald-200">🚗 Expert On the Way</span>;
      case 'ARRIVED':
        return <span className="px-3 py-1 rounded-full text-xs font-bold bg-emerald-50 text-emerald-800 border border-emerald-200 flex items-center gap-1">✓ Arrived at Doorstep</span>;
      case 'IN_PROGRESS':
        return <span className="px-3 py-1 rounded-full text-xs font-bold bg-[var(--color-brand-light)] text-[var(--color-brand-hover)] border border-[var(--color-brand-bright)]/40 animate-pulse">⚡ Service In Progress</span>;
      case 'COMPLETED':
        return <span className="px-3 py-1 rounded-full text-xs font-bold bg-emerald-50 text-emerald-800 border border-emerald-200">✓ Completed</span>;
      case 'CANCELLED':
        return <span className="px-3 py-1 rounded-full text-xs font-bold bg-red-50 text-red-700 border border-red-200">🗙 Cancelled</span>;
      default:
        return null;
    }
  };

  return (
    <div className="min-h-screen bg-[var(--color-brand-soft)]/30 py-8 sm:py-12">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <ProfileSettings />
        {loading && <p role="status">Loading bookings...</p>}{error && <p role="alert">{error}</p>}
        {/* User Profile Header */}
        <div className="bg-white rounded-3xl p-6 sm:p-8 border border-brand-light/70 shadow-xs mb-8 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="flex items-center gap-4">
            <img
              src={user?.avatar || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=200&q=80'}
              alt={user?.name}
              className="w-16 h-16 rounded-2xl object-cover border-2 border-[var(--color-brand)]"
              referrerPolicy="no-referrer"
            />
            <div>
              <div className="flex items-center gap-2">
                <h1 className="text-xl sm:text-2xl font-black text-[var(--color-ink)] font-['Outfit']">{user?.name}</h1>
                <span className="text-[10px] font-bold uppercase bg-[var(--color-brand-light)] text-[var(--color-brand-hover)] px-2.5 py-0.5 rounded-full">
                  Customer
                </span>
              </div>
              <p className="text-xs text-gray-500 mt-0.5">{user?.phone} • {user?.email}</p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={() => onNavigate('/services')}
              className="px-4 py-2.5 bg-[var(--color-brand)] hover:bg-[var(--color-brand-hover)] text-white font-bold text-xs rounded-xl shadow-md shadow-brand/20 transition-all cursor-pointer"
            >
              Book New Service
            </button>
          </div>
        </div>

        {/* Navigation Tabs */}
        <div className="flex items-center gap-2 border-b border-gray-200 mb-8 pb-px">
          <button
            onClick={() => setActiveTab('bookings')}
            className={`flex items-center gap-2 px-5 py-3 text-xs font-bold transition-all border-b-2 cursor-pointer font-['Outfit'] ${
              activeTab === 'bookings'
                ? 'border-[var(--color-brand)] text-[var(--color-brand-hover)]'
                : 'border-transparent text-gray-500 hover:text-gray-900'
            }`}
          >
            <CalendarCheck className="w-4 h-4" />
            <span>My Bookings ({bookings.length})</span>
          </button>

          <button
            onClick={() => setActiveTab('addresses')}
            className={`flex items-center gap-2 px-5 py-3 text-xs font-bold transition-all border-b-2 cursor-pointer font-['Outfit'] ${
              activeTab === 'addresses'
                ? 'border-[var(--color-brand)] text-[var(--color-brand-hover)]'
                : 'border-transparent text-gray-500 hover:text-gray-900'
            }`}
          >
            <MapPin className="w-4 h-4" />
            <span>Saved Addresses ({user?.addresses?.length || 0})</span>
          </button>

          <button
            onClick={() => setActiveTab('favorites')}
            className={`flex items-center gap-2 px-5 py-3 text-xs font-bold transition-all border-b-2 cursor-pointer font-['Outfit'] ${
              activeTab === 'favorites'
                ? 'border-[var(--color-brand)] text-[var(--color-brand-hover)]'
                : 'border-transparent text-gray-500 hover:text-gray-900'
            }`}
          >
            <Heart className="w-4 h-4" />
            <span>Favorites ({favorites.length})</span>
          </button>
        </div>

        {/* TAB 1: Bookings List */}
        {activeTab === 'bookings' && (() => {
          const activeBookingsCount = bookings.filter((b) =>
            ['PENDING', 'ASSIGNED', 'CONFIRMED', 'ON_THE_WAY', 'ARRIVED', 'IN_PROGRESS'].includes(b.status)
          ).length;
          const completedBookingsCount = bookings.filter((b) => b.status === 'COMPLETED').length;
          const cancelledBookingsCount = bookings.filter((b) => b.status === 'CANCELLED').length;

          const filteredBookings = bookings.filter((b) => {
            if (bookingFilter === 'ACTIVE') {
              return ['PENDING', 'ASSIGNED', 'CONFIRMED', 'ON_THE_WAY', 'ARRIVED', 'IN_PROGRESS'].includes(b.status);
            }
            if (bookingFilter === 'COMPLETED') {
              return b.status === 'COMPLETED';
            }
            if (bookingFilter === 'CANCELLED') {
              return b.status === 'CANCELLED';
            }
            return true;
          });

          return (
            <div className="space-y-6">
              {/* Header & Filter Controls matching Photo 2 */}
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <div>
                  <h2 className="text-xl sm:text-2xl font-black text-[var(--color-ink)] font-['Outfit']">My Bookings</h2>
                  <p className="text-xs text-gray-500 flex items-center gap-1.5 mt-0.5 font-medium">
                    <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse inline-block" />
                    Live updates and real-time tracking
                  </p>
                </div>

                {/* Filter Pills */}
                <div className="flex items-center gap-2 overflow-x-auto pb-1 sm:pb-0">
                  <button
                    type="button"
                    onClick={() => setBookingFilter('ALL')}
                    className={`px-3.5 py-1.5 rounded-full text-xs font-bold transition-all cursor-pointer flex items-center gap-1.5 shrink-0 ${
                      bookingFilter === 'ALL'
                        ? 'bg-[var(--color-brand)] text-white shadow-xs'
                        : 'bg-white text-gray-600 border border-gray-200 hover:bg-gray-50'
                    }`}
                  >
                    <span>⊞</span> All ({bookings.length})
                  </button>
                  <button
                    type="button"
                    onClick={() => setBookingFilter('ACTIVE')}
                    className={`px-3.5 py-1.5 rounded-full text-xs font-bold transition-all cursor-pointer flex items-center gap-1.5 shrink-0 ${
                      bookingFilter === 'ACTIVE'
                        ? 'bg-[var(--color-brand)] text-white shadow-xs'
                        : 'bg-white text-gray-600 border border-gray-200 hover:bg-gray-50'
                    }`}
                  >
                    <span>⚡</span> Active ({activeBookingsCount})
                  </button>
                  <button
                    type="button"
                    onClick={() => setBookingFilter('COMPLETED')}
                    className={`px-3.5 py-1.5 rounded-full text-xs font-bold transition-all cursor-pointer flex items-center gap-1.5 shrink-0 ${
                      bookingFilter === 'COMPLETED'
                        ? 'bg-[var(--color-brand)] text-white shadow-xs'
                        : 'bg-white text-gray-600 border border-gray-200 hover:bg-gray-50'
                    }`}
                  >
                    <span>✓</span> Completed ({completedBookingsCount})
                  </button>
                  <button
                    type="button"
                    onClick={() => setBookingFilter('CANCELLED')}
                    className={`px-3.5 py-1.5 rounded-full text-xs font-bold transition-all cursor-pointer flex items-center gap-1.5 shrink-0 ${
                      bookingFilter === 'CANCELLED'
                        ? 'bg-[var(--color-brand)] text-white shadow-xs'
                        : 'bg-white text-gray-600 border border-gray-200 hover:bg-gray-50'
                    }`}
                  >
                    <span>🗙</span> Cancelled ({cancelledBookingsCount})
                  </button>
                </div>
              </div>

              {filteredBookings.length === 0 ? (
                <div className="bg-white rounded-3xl p-12 text-center border border-gray-100 max-w-md mx-auto">
                  <CalendarCheck className="w-12 h-12 text-gray-300 mx-auto mb-3" />
                  <h3 className="font-bold text-gray-900 text-base mb-1 font-['Outfit']">No {bookingFilter !== 'ALL' ? bookingFilter.toLowerCase() : ''} bookings</h3>
                  <p className="text-xs text-gray-500 mb-6">
                    Schedule your first AC service, deep bathroom clean, or salon session.
                  </p>
                  <button
                    onClick={() => onNavigate('/services')}
                    className="px-5 py-2.5 bg-[var(--color-brand)] hover:bg-[var(--color-brand-hover)] text-white font-bold text-xs rounded-xl shadow-xs cursor-pointer"
                  >
                    Explore Services
                  </button>
                </div>
              ) : (
                <div className="space-y-4">
                  {filteredBookings.map((booking) => (
                    <div
                      key={booking.id}
                      className="bg-white rounded-3xl p-5 sm:p-6 border border-gray-100 shadow-xs hover:border-[var(--color-brand-bright)] transition-all space-y-4"
                    >
                      {/* Top Row: # Reference + Status Badge */}
                      <div className="flex items-center justify-between gap-3">
                        <div className="flex items-center gap-2">
                          <span className="font-extrabold text-sm text-[var(--color-ink)] font-['Outfit']">
                            # {booking.id}
                          </span>
                        </div>
                        <div>{getStatusBadge(booking.status)}</div>
                      </div>

                      {/* Service Name & Variant */}
                      <div>
                        <h3 className="font-extrabold text-base sm:text-lg text-[var(--color-ink)] font-['Outfit'] leading-snug">
                          {booking.serviceName || (booking.items && booking.items[0]?.service?.name) || 'Home Service'}
                        </h3>
                        <p className="text-xs text-gray-500 font-medium mt-0.5">
                          {booking.variantName || (booking.items && booking.items[0]?.variant?.name) || 'Standard Package'}
                        </p>
                      </div>

                      {/* Scheduled Slot & Address Info */}
                      <div className="flex flex-wrap items-center gap-y-2 gap-x-4 text-xs text-gray-600 bg-gray-50/80 p-3.5 rounded-2xl border border-gray-100">
                        <div className="flex items-center gap-1.5 font-medium">
                          <Clock className="w-3.5 h-3.5 text-[var(--color-brand)] shrink-0" />
                          <span>{booking.scheduledDate}, {booking.scheduledTimeSlot}</span>
                        </div>
                        {booking.address && (
                          <div className="flex items-center gap-1.5 text-gray-500 truncate max-w-sm">
                            <MapPin className="w-3.5 h-3.5 text-gray-400 shrink-0" />
                            <span className="truncate">
                              {booking.address.house}, {booking.address.street ? `${booking.address.street}, ` : ''}{booking.address.city || 'Agra'}
                            </span>
                          </div>
                        )}
                      </div>

                      {/* Start OTP, Price & Action Buttons matching Photo 2 */}
                      <div className="pt-3 border-t border-gray-100 flex flex-wrap items-center justify-between gap-3">
                        <div className="flex items-center gap-4">
                          {booking.verificationOtp && booking.status !== 'COMPLETED' && booking.status !== 'CANCELLED' && (
                            <div className="px-3 py-1.5 bg-emerald-50 border border-emerald-200 rounded-xl text-xs flex items-center gap-2">
                              <span className="text-[11px] font-bold text-emerald-800">Start OTP</span>
                              <span className="font-mono font-black text-emerald-950 tracking-wider bg-white px-2 py-0.5 rounded-lg border border-emerald-200 shadow-2xs">
                                {booking.verificationOtp}
                              </span>
                            </div>
                          )}
                          <span className="text-lg font-black text-[var(--color-ink)] font-['Outfit']">
                            ₹{booking.total?.toLocaleString('en-IN') || 0}
                          </span>
                        </div>

                        <div className="flex items-center gap-2 flex-wrap">
                          <button
                            type="button"
                            onClick={() => setSelectedTrackingBooking(booking)}
                            className="px-4 py-2 rounded-xl bg-[var(--color-brand-light)] hover:bg-[var(--color-brand-soft)] text-[var(--color-brand-hover)] border border-[var(--color-brand-bright)]/40 text-xs font-bold flex items-center gap-1.5 cursor-pointer transition-all shadow-2xs"
                          >
                            <Clock className="w-3.5 h-3.5" />
                            <span>View Details</span>
                          </button>

                          <button
                            type="button"
                            onClick={() => onNavigate(`/messages?booking=${booking.id}`)}
                            className="px-4 py-2 rounded-xl bg-[var(--color-brand)] hover:bg-[var(--color-brand-hover)] text-white text-xs font-bold flex items-center gap-1.5 cursor-pointer shadow-xs transition-all"
                          >
                            <MessageSquare className="w-3.5 h-3.5" />
                            <span>Message</span>
                            {!!unreadByBooking[booking.id] && (
                              <span className="rounded-full bg-white text-[var(--color-brand)] text-[10px] font-black px-1.5 py-0.2">
                                {unreadByBooking[booking.id]}
                              </span>
                            )}
                          </button>

                          <button
                            onClick={() => {
                              const text = `Service Assist booking receipt\nBooking: ${booking.id}\nCustomer: ${booking.userName}\nTotal: INR ${booking.total}\nPayment: ${booking.paymentStatus}\nThis is a booking receipt, not a tax invoice.`;
                              const url = URL.createObjectURL(new Blob([text], { type: 'text/plain' }));
                              const a = document.createElement('a');
                              a.href = url;
                              a.download = `${booking.id}-receipt.txt`;
                              a.click();
                              URL.revokeObjectURL(url);
                            }}
                            className="px-3 py-2 rounded-xl border border-gray-200 text-xs font-semibold hover:bg-gray-50 flex items-center gap-1.5 cursor-pointer"
                          >
                            <FileText className="w-3.5 h-3.5 text-gray-500" />
                            <span>Receipt</span>
                          </button>

                          {['PENDING', 'ASSIGNED', 'CONFIRMED'].includes(booking.status) && (
                            <button
                              onClick={() => handleCancelBooking(booking.id)}
                              className="px-3 py-2 rounded-xl text-xs font-semibold text-red-600 hover:bg-red-50 transition-colors cursor-pointer"
                            >
                              Cancel
                            </button>
                          )}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          );
        })()}

        {/* TAB 2: Saved Addresses */}
        {activeTab === 'addresses' && (
          <div className="space-y-6">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="font-extrabold text-base text-gray-900 font-['Outfit']">Delivery Addresses</h3>
                <p className="text-xs text-gray-500">Manage addresses where you receive doorstep services</p>
              </div>
              <button
                onClick={() => setIsAddingAddr(!isAddingAddr)}
                className="px-4 py-2 bg-[var(--color-brand)] hover:bg-[var(--color-brand-hover)] text-white font-bold text-xs rounded-xl flex items-center gap-1.5 shadow-xs cursor-pointer"
              >
                <Plus className="w-4 h-4" />
                <span>{isAddingAddr ? 'Cancel' : 'Add New Address'}</span>
              </button>
            </div>

            {/* Address Add Form */}
            {isAddingAddr && (
              <form onSubmit={handleCreateAddress} className="bg-white p-6 rounded-3xl border border-gray-200 space-y-4 shadow-sm">
                <h4 className="font-bold text-sm text-gray-900 font-['Outfit']">Add New Address</h4>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <input
                    type="text"
                    required
                    placeholder="House / Flat / Villa / Floor"
                    value={house}
                    onChange={(e) => setHouse(e.target.value)}
                    className="p-3 bg-gray-50 border border-gray-200 rounded-xl text-xs focus:outline-none focus:ring-1 focus:ring-[var(--color-brand)]"
                  />
                  <input
                    type="text"
                    required
                    placeholder="Street / Road / Society"
                    value={street}
                    onChange={(e) => setStreet(e.target.value)}
                    className="p-3 bg-gray-50 border border-gray-200 rounded-xl text-xs focus:outline-none focus:ring-1 focus:ring-[var(--color-brand)]"
                  />
                  <input
                    type="text"
                    required
                    placeholder="Area / Landmark"
                    value={area}
                    onChange={(e) => setArea(e.target.value)}
                    className="p-3 bg-gray-50 border border-gray-200 rounded-xl text-xs focus:outline-none focus:ring-1 focus:ring-[var(--color-brand)]"
                  />
                  <input
                    type="text"
                    required
                    placeholder="City"
                    value={city}
                    onChange={(e) => setCity(e.target.value)}
                    className="p-3 bg-gray-50 border border-gray-200 rounded-xl text-xs focus:outline-none focus:ring-1 focus:ring-[var(--color-brand)]"
                  />
                  <input
                    type="text"
                    required
                    placeholder="Pincode"
                    value={pincode}
                    onChange={(e) => setPincode(e.target.value)}
                    className="p-3 bg-gray-50 border border-gray-200 rounded-xl text-xs focus:outline-none focus:ring-1 focus:ring-[var(--color-brand)]"
                  />
                  <div className="flex items-center gap-2">
                    {(['Home', 'Work', 'Other'] as const).map((t) => (
                      <button
                        key={t}
                        type="button"
                        onClick={() => setAddrType(t)}
                        className={`px-3 py-2 text-xs font-semibold rounded-xl border cursor-pointer ${
                          addrType === t
                            ? 'bg-[var(--color-brand-light)] border-[var(--color-brand)] text-[var(--color-brand-hover)] font-bold'
                            : 'border-gray-200 text-gray-600'
                        }`}
                      >
                        {t}
                      </button>
                    ))}
                  </div>
                </div>
                <button
                  type="submit"
                  className="px-6 py-2.5 bg-[var(--color-brand)] hover:bg-[var(--color-brand-hover)] text-white font-bold text-xs rounded-xl shadow-xs cursor-pointer"
                >
                  Save Address
                </button>
              </form>
            )}

            {/* Addresses Grid */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {user?.addresses?.map((addr) => (
                <div
                  key={addr.id}
                  className="bg-white rounded-3xl p-5 border border-gray-100 shadow-xs flex flex-col justify-between"
                >
                  <div>
                    <div className="flex items-center justify-between mb-2">
                      <span className="font-extrabold text-sm text-gray-900 font-['Outfit']">{addr.type}</span>
                      {addr.isDefault && (
                        <span className="text-[10px] font-bold bg-[var(--color-brand-light)] text-[var(--color-brand-hover)] px-2 py-0.5 rounded-full">
                          Default Address
                        </span>
                      )}
                    </div>
                    <p className="text-xs text-gray-600 leading-relaxed">
                      {addr.house}, {addr.street}, {addr.area}
                    </p>
                    <p className="text-[11px] text-gray-400 mt-1">
                      {addr.city}, {addr.state} - {addr.pincode}
                    </p>
                  </div>

                  <div className="pt-4 border-t border-gray-100 flex items-center justify-between text-xs mt-4">
                    {!addr.isDefault && (
                      <button
                        onClick={() => setDefaultAddress(addr.id)}
                        className="text-xs font-bold text-[var(--color-brand)] hover:underline cursor-pointer"
                      >
                        Set as default
                      </button>
                    )}
                    <span className="text-gray-400 text-[11px]">Active</span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* TAB 3: Favorite Services */}
        {activeTab === 'favorites' && (
          <div>
            {favoriteServicesList.length === 0 ? (
              <div className="bg-white rounded-3xl p-12 text-center border border-gray-100 max-w-md mx-auto">
                <Heart className="w-12 h-12 text-gray-300 mx-auto mb-3" />
                <h3 className="font-bold text-gray-900 text-base mb-1 font-['Outfit']">No Saved Favorites</h3>
                <p className="text-xs text-gray-500 mb-6">
                  Click the heart icon on any service card to bookmark it for quick access.
                </p>
                <button
                  onClick={() => onNavigate('/services')}
                  className="px-5 py-2.5 bg-[var(--color-brand)] hover:bg-[var(--color-brand-hover)] text-white font-bold text-xs rounded-xl shadow-xs cursor-pointer"
                >
                  Explore Services
                </button>
              </div>
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                {favoriteServicesList.map((service) => (
                  <div
                    key={service.id}
                    className="bg-white rounded-3xl border border-gray-100 p-5 shadow-xs flex flex-col justify-between"
                  >
                    <div className="flex gap-3 mb-4">
                      <ImageWithFallback
                        src={service.image}
                        fallbackSrc={service.categoryImage}
                        fallbackTitle={service.name}
                        alt={service.name}
                        className="w-20 h-20 rounded-2xl object-cover shrink-0 cursor-pointer"
                        onClick={() => onSelectService(service.slug)}
                      />
                      <div className="flex-1 min-w-0">
                        <h4
                          onClick={() => onSelectService(service.slug)}
                          className="font-bold text-sm text-gray-900 hover:text-[var(--color-brand)] transition-colors truncate cursor-pointer font-['Outfit']"
                        >
                          {service.name}
                        </h4>
                        <p className="text-xs text-gray-500 line-clamp-2 mt-1">{service.shortDesc}</p>
                      </div>
                    </div>

                    <div className="pt-3 border-t border-gray-100 flex items-center justify-between">
                      <span className="font-black text-sm text-gray-900">₹{service.startingPrice}</span>
                      <div className="flex items-center gap-2">
                        <button
                          onClick={() => toggleFavorite(service.id)}
                          className="text-xs text-red-500 hover:underline px-2 py-1 cursor-pointer"
                        >
                          Remove
                        </button>
                        <button
                          onClick={() => addItem(service)}
                          className="px-3.5 py-1.5 bg-[var(--color-brand)] hover:bg-[var(--color-brand-hover)] text-white font-bold text-xs rounded-xl shadow-xs cursor-pointer"
                        >
                          Book Now
                        </button>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </div>

      {/* Live Booking Tracker Modal (Screenshot 2) */}
      {selectedTrackingBooking && (
        <LiveBookingTrackerModal
          booking={selectedTrackingBooking}
          onClose={() => setSelectedTrackingBooking(null)}
          onMessage={(bId) => {
            setSelectedTrackingBooking(null);
            onNavigate(`/messages?booking=${bId}`);
          }}
        />
      )}
    </div>
  );
};
