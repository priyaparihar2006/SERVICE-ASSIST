import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { Booking, Service } from '../types';
import {
  CalendarCheck,
  MapPin,
  Heart,
  Clock,
  UserCheck,
  Phone,
  Plus,
  FileText,
} from 'lucide-react';
import { useCart } from '../context/CartContext';

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

  const [activeTab, setActiveTab] = useState<'bookings' | 'addresses' | 'favorites'>('bookings');
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [, setLoading] = useState(true);

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
      const res = await fetch(`/api/bookings?userId=${user?.id || 'usr-customer-1'}`);
      const data = await res.json();
      if (data.bookings) {
        setBookings(data.bookings);
      }
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchBookings();
  }, [user]);

  const handleCancelBooking = async (bookingId: string) => {
    if (!confirm('Are you sure you want to cancel this booking? Cancellation is 100% free.')) return;
    try {
      const res = await fetch(`/api/bookings/${bookingId}/status`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ status: 'CANCELLED' }),
      });
      if (res.ok) {
        fetchBookings();
      }
    } catch (e) {
      console.error(e);
    }
  };

  const handleCreateAddress = async (e: React.FormEvent) => {
    e.preventDefault();
    await addAddress({
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
    setArea('');
  };

  const favoriteServicesList = services.filter((s) => favorites.includes(s.id));

  const getStatusBadge = (status: Booking['status']) => {
    switch (status) {
      case 'PENDING':
        return <span className="px-2.5 py-1 rounded-full text-[10px] font-bold bg-amber-50 text-amber-800 border border-amber-200">Matching Expert...</span>;
      case 'ASSIGNED':
        return <span className="px-2.5 py-1 rounded-full text-[10px] font-bold bg-blue-50 text-blue-700 border border-blue-200">Expert Assigned</span>;
      case 'IN_PROGRESS':
        return <span className="px-2.5 py-1 rounded-full text-[10px] font-bold bg-[#FFF1E5] text-[#E85D04] border border-[#FF9A3D]/40 animate-pulse">Service In Progress</span>;
      case 'COMPLETED':
        return <span className="px-2.5 py-1 rounded-full text-[10px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">Completed ✓</span>;
      case 'CANCELLED':
        return <span className="px-2.5 py-1 rounded-full text-[10px] font-bold bg-red-50 text-red-700 border border-red-200">Cancelled</span>;
      default:
        return null;
    }
  };

  return (
    <div className="min-h-screen bg-[#FFF8F2]/30 py-8 sm:py-12">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* User Profile Header */}
        <div className="bg-white rounded-3xl p-6 sm:p-8 border border-orange-100/70 shadow-xs mb-8 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="flex items-center gap-4">
            <img
              src={user?.avatar || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=200&q=80'}
              alt={user?.name}
              className="w-16 h-16 rounded-2xl object-cover border-2 border-[#FF7A00]"
              referrerPolicy="no-referrer"
            />
            <div>
              <div className="flex items-center gap-2">
                <h1 className="text-xl sm:text-2xl font-black text-[#15252B] font-['Outfit']">{user?.name}</h1>
                <span className="text-[10px] font-bold uppercase bg-[#FFF1E5] text-[#E85D04] px-2.5 py-0.5 rounded-full">
                  Customer
                </span>
              </div>
              <p className="text-xs text-gray-500 mt-0.5">{user?.phone} • {user?.email}</p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={() => onNavigate('/services')}
              className="px-4 py-2.5 bg-[#FF7A00] hover:bg-[#E85D04] text-white font-bold text-xs rounded-xl shadow-md shadow-orange-500/20 transition-all cursor-pointer"
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
                ? 'border-[#FF7A00] text-[#E85D04]'
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
                ? 'border-[#FF7A00] text-[#E85D04]'
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
                ? 'border-[#FF7A00] text-[#E85D04]'
                : 'border-transparent text-gray-500 hover:text-gray-900'
            }`}
          >
            <Heart className="w-4 h-4" />
            <span>Favorites ({favorites.length})</span>
          </button>
        </div>

        {/* TAB 1: Bookings List */}
        {activeTab === 'bookings' && (
          <div className="space-y-4">
            {bookings.length === 0 ? (
              <div className="bg-white rounded-3xl p-12 text-center border border-gray-100 max-w-md mx-auto">
                <CalendarCheck className="w-12 h-12 text-gray-300 mx-auto mb-3" />
                <h3 className="font-bold text-gray-900 text-base mb-1 font-['Outfit']">No Bookings Yet</h3>
                <p className="text-xs text-gray-500 mb-6">
                  Schedule your first AC service, deep bathroom clean, or salon session.
                </p>
                <button
                  onClick={() => onNavigate('/services')}
                  className="px-5 py-2.5 bg-[#FF7A00] hover:bg-[#E85D04] text-white font-bold text-xs rounded-xl shadow-xs cursor-pointer"
                >
                  Explore Services
                </button>
              </div>
            ) : (
              bookings.map((booking) => (
                <div
                  key={booking.id}
                  className="bg-white rounded-3xl p-6 border border-gray-100 shadow-xs hover:border-[#FF9A3D] transition-all space-y-4"
                >
                  {/* Top Bar */}
                  <div className="flex flex-wrap items-center justify-between gap-3 pb-4 border-b border-gray-100">
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="font-extrabold text-sm text-gray-900 font-['Outfit']">{booking.id}</span>
                        {getStatusBadge(booking.status)}
                      </div>
                      <p className="text-xs text-gray-500 mt-0.5">
                        Booked on {booking.createdAt} • Payment: {booking.paymentMethod} ({booking.paymentStatus})
                      </p>
                    </div>

                    <div className="flex items-center gap-3">
                      {booking.verificationOtp && booking.status !== 'COMPLETED' && booking.status !== 'CANCELLED' && (
                        <div className="px-3 py-1.5 bg-[#FFF1E5] border border-[#FF9A3D]/40 rounded-xl text-xs flex items-center gap-1.5">
                          <span className="text-[11px] font-semibold text-[#E85D04]">Start OTP:</span>
                          <span className="font-mono font-black text-[#15252B] tracking-wider">
                            {booking.verificationOtp}
                          </span>
                        </div>
                      )}

                      <span className="text-base font-black text-gray-900">₹{booking.total}</span>
                    </div>
                  </div>

                  {/* Scheduled Slot & Address */}
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs text-gray-600 bg-gray-50/70 p-4 rounded-2xl">
                    <div className="flex items-start gap-2.5">
                      <Clock className="w-4 h-4 text-[#FF7A00] shrink-0 mt-0.5" />
                      <div>
                        <span className="font-bold text-gray-900 block">Scheduled Arrival Slot</span>
                        <span>{booking.scheduledDate} at {booking.scheduledTimeSlot}</span>
                      </div>
                    </div>

                    <div className="flex items-start gap-2.5">
                      <MapPin className="w-4 h-4 text-[#FF7A00] shrink-0 mt-0.5" />
                      <div>
                        <span className="font-bold text-gray-900 block">Service Address</span>
                        <span className="truncate block">
                          {booking.address.house}, {booking.address.street}, {booking.address.area}, {booking.address.city} - {booking.address.pincode}
                        </span>
                      </div>
                    </div>
                  </div>

                  {/* Items List */}
                  <div className="space-y-2">
                    <span className="text-[11px] uppercase font-bold text-gray-400 tracking-wider">
                      Included Services
                    </span>
                    <div className="space-y-1.5">
                      {booking.items && booking.items.length > 0 ? (
                        booking.items.map((item, i) => (
                          <div key={i} className="flex items-center justify-between text-xs py-1">
                            <div className="flex items-center gap-2">
                              <span className="w-1.5 h-1.5 rounded-full bg-[#FF7A00]" />
                              <span className="font-bold text-gray-900">{item.service?.name}</span>
                              <span className="text-gray-500">({item.variant?.name})</span>
                              <span className="font-semibold text-gray-400">× {item.quantity}</span>
                            </div>
                            <span className="font-bold text-gray-800">
                              ₹{(item.variant?.price || 0) * item.quantity}
                            </span>
                          </div>
                        ))
                      ) : (
                        <div className="flex items-center justify-between text-xs py-1">
                          <div className="flex items-center gap-2">
                            <span className="w-1.5 h-1.5 rounded-full bg-[#FF7A00]" />
                            <span className="font-bold text-gray-900">{booking.serviceName}</span>
                            <span className="text-gray-500">({booking.variantName})</span>
                          </div>
                          <span className="font-bold text-gray-800">₹{booking.total}</span>
                        </div>
                      )}
                    </div>
                  </div>

                  {/* Assigned Professional Card & Actions */}
                  <div className="pt-4 border-t border-gray-100 flex flex-wrap items-center justify-between gap-3">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-xl bg-[#FFF1E5] flex items-center justify-center text-[#FF7A00]">
                        <UserCheck className="w-5 h-5" />
                      </div>
                      <div>
                        <span className="text-[10px] text-gray-400 font-bold uppercase block">
                          Assigned Professional
                        </span>
                        <span className="font-bold text-xs text-gray-900 font-['Outfit']">
                          {booking.professionalName || 'Matching nearest partner...'}
                        </span>
                      </div>
                    </div>

                    <div className="flex items-center gap-2">
                      <button
                        onClick={() => alert(`Connecting you to ${booking.professionalName} (+91 98765 43210)`)}
                        className="px-3 py-1.5 rounded-xl border border-gray-200 text-xs font-semibold hover:bg-[#FFF8F2] flex items-center gap-1.5 cursor-pointer"
                      >
                        <Phone className="w-3.5 h-3.5 text-[#FF7A00]" />
                        <span>Call Partner</span>
                      </button>

                      <button
                        onClick={() => alert(`Invoice #INV-${booking.id} generated! Standard GST 5% included.`)}
                        className="px-3 py-1.5 rounded-xl border border-gray-200 text-xs font-semibold hover:bg-gray-50 flex items-center gap-1.5 cursor-pointer"
                      >
                        <FileText className="w-3.5 h-3.5 text-gray-500" />
                        <span>Invoice</span>
                      </button>

                      {booking.status !== 'COMPLETED' && booking.status !== 'CANCELLED' && (
                        <button
                          onClick={() => handleCancelBooking(booking.id)}
                          className="px-3 py-1.5 rounded-xl text-xs font-semibold text-red-600 hover:bg-red-50 transition-colors cursor-pointer"
                        >
                          Cancel
                        </button>
                      )}
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>
        )}

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
                className="px-4 py-2 bg-[#FF7A00] hover:bg-[#E85D04] text-white font-bold text-xs rounded-xl flex items-center gap-1.5 shadow-xs cursor-pointer"
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
                    className="p-3 bg-gray-50 border border-gray-200 rounded-xl text-xs focus:outline-none focus:ring-1 focus:ring-[#FF7A00]"
                  />
                  <input
                    type="text"
                    required
                    placeholder="Street / Road / Society"
                    value={street}
                    onChange={(e) => setStreet(e.target.value)}
                    className="p-3 bg-gray-50 border border-gray-200 rounded-xl text-xs focus:outline-none focus:ring-1 focus:ring-[#FF7A00]"
                  />
                  <input
                    type="text"
                    required
                    placeholder="Area / Landmark"
                    value={area}
                    onChange={(e) => setArea(e.target.value)}
                    className="p-3 bg-gray-50 border border-gray-200 rounded-xl text-xs focus:outline-none focus:ring-1 focus:ring-[#FF7A00]"
                  />
                  <input
                    type="text"
                    required
                    placeholder="City"
                    value={city}
                    onChange={(e) => setCity(e.target.value)}
                    className="p-3 bg-gray-50 border border-gray-200 rounded-xl text-xs focus:outline-none focus:ring-1 focus:ring-[#FF7A00]"
                  />
                  <input
                    type="text"
                    required
                    placeholder="Pincode"
                    value={pincode}
                    onChange={(e) => setPincode(e.target.value)}
                    className="p-3 bg-gray-50 border border-gray-200 rounded-xl text-xs focus:outline-none focus:ring-1 focus:ring-[#FF7A00]"
                  />
                  <div className="flex items-center gap-2">
                    {(['Home', 'Work', 'Other'] as const).map((t) => (
                      <button
                        key={t}
                        type="button"
                        onClick={() => setAddrType(t)}
                        className={`px-3 py-2 text-xs font-semibold rounded-xl border cursor-pointer ${
                          addrType === t
                            ? 'bg-[#FFF1E5] border-[#FF7A00] text-[#E85D04] font-bold'
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
                  className="px-6 py-2.5 bg-[#FF7A00] hover:bg-[#E85D04] text-white font-bold text-xs rounded-xl shadow-xs cursor-pointer"
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
                        <span className="text-[10px] font-bold bg-[#FFF1E5] text-[#E85D04] px-2 py-0.5 rounded-full">
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
                        className="text-xs font-bold text-[#FF7A00] hover:underline cursor-pointer"
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
                  className="px-5 py-2.5 bg-[#FF7A00] hover:bg-[#E85D04] text-white font-bold text-xs rounded-xl shadow-xs cursor-pointer"
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
                      <img
                        src={service.image}
                        alt={service.name}
                        className="w-20 h-20 rounded-2xl object-cover shrink-0 cursor-pointer"
                        onClick={() => onSelectService(service.slug)}
                        referrerPolicy="no-referrer"
                      />
                      <div className="flex-1 min-w-0">
                        <h4
                          onClick={() => onSelectService(service.slug)}
                          className="font-bold text-sm text-gray-900 hover:text-[#FF7A00] transition-colors truncate cursor-pointer font-['Outfit']"
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
                          className="px-3.5 py-1.5 bg-[#FF7A00] hover:bg-[#E85D04] text-white font-bold text-xs rounded-xl shadow-xs cursor-pointer"
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
    </div>
  );
};
