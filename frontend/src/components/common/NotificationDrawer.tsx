import { apiFetch } from '../../services/api';
import React, { useEffect, useState } from 'react';
import { NotificationItem } from '../../types';
import {
  Bell,
  Check,
  X,
  ArrowRight,
  Sparkles,
  AlertCircle,
  Trash2,
  ShoppingBag,
  PackageCheck,
  Tag,
  Clock,
  CheckCircle2,
} from 'lucide-react';
import { useChat } from '../../context/ChatContext';
import { useCart } from '../../context/CartContext';

interface NotificationDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  onNavigate?: (path: string) => void;
}

export const NotificationDrawer: React.FC<NotificationDrawerProps> = ({
  isOpen,
  onClose,
  onNavigate,
}) => {
  const { subscribe } = useChat();
  const { items, openCartDrawer } = useCart();
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [filterTab, setFilterTab] = useState<'ALL' | 'ORDERS' | 'OFFERS' | 'ALERTS'>('ALL');

  const defaultNotifications: NotificationItem[] = [
    {
      id: 'notif-1',
      title: 'Booking Confirmed • SRV-59895',
      message: 'Rajesh Sharma is assigned for your Move-in Deep Clean. Arrival OTP: 3975.',
      type: 'BOOKING',
      timestamp: '10m ago',
      read: false,
      link: '/dashboard',
    },
    {
      id: 'notif-2',
      title: 'Flat ₹150 OFF Voucher Active',
      message: 'Use WELCOME150 on your booking. Offer available up to 30 days.',
      type: 'OFFER',
      timestamp: '2h ago',
      read: false,
      link: '/offers',
    },
    {
      id: 'notif-3',
      title: 'Service Completed & Inspected',
      message: 'Bathroom scrubbing completed with 30-day warranty active.',
      type: 'BOOKING',
      timestamp: '1d ago',
      read: true,
      link: '/dashboard',
    },
  ];

  const fetchNotifications = async () => {
    try {
      setLoading(true);
      const res = await apiFetch('/api/notifications');
      const data = await res.json();
      if (data.notifications && Array.isArray(data.notifications) && data.notifications.length > 0) {
        setNotifications(data.notifications);
      } else {
        setNotifications((prev) => (prev.length > 0 ? prev : defaultNotifications));
      }
    } catch {
      setNotifications((prev) => (prev.length > 0 ? prev : defaultNotifications));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (isOpen) {
      fetchNotifications();
    }
  }, [isOpen]);

  useEffect(() => {
    if (!isOpen) return;
    const off = subscribe('notification:new', fetchNotifications);
    const timer = window.setInterval(fetchNotifications, 30000);
    return () => {
      off();
      window.clearInterval(timer);
    };
  }, [isOpen, subscribe]);

  const markAllRead = async () => {
    try {
      await Promise.all(
        notifications
          .filter((n) => !n.read)
          .map((n) => apiFetch(`/api/notifications/${n.id}/read`, { method: 'PATCH', body: '{}' }).catch(() => {}))
      );
    } catch {
      // offline fallback
    }
    setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
  };

  const deleteAll = async () => {
    try {
      await apiFetch('/api/notifications', { method: 'DELETE' }).catch(() => {});
    } catch {
      // offline fallback
    }
    setNotifications([]);
  };

  const deleteSingle = async (e: React.MouseEvent, id: string) => {
    e.stopPropagation();
    try {
      await apiFetch(`/api/notifications/${id}`, { method: 'DELETE' }).catch(() => {});
    } catch {
      // offline fallback
    }
    setNotifications((prev) => prev.filter((n) => n.id !== id));
  };

  const handleOpenCart = () => {
    onClose();
    setTimeout(() => {
      openCartDrawer();
    }, 150);
  };

  if (!isOpen) return null;

  const totalCartCount = items.reduce((sum, item) => sum + item.quantity, 0);
  const unreadCount = notifications.filter((n) => !n.read).length;

  const filteredNotifications = notifications.filter((n) => {
    const text = (n.title + ' ' + n.message).toLowerCase();
    if (filterTab === 'ORDERS') return text.includes('booking') || text.includes('assigned') || text.includes('completed') || text.includes('order');
    if (filterTab === 'OFFERS') return text.includes('off') || text.includes('voucher') || text.includes('discount') || text.includes('offer');
    if (filterTab === 'ALERTS') return text.includes('alert') || text.includes('warranty') || text.includes('remind');
    return true;
  });

  return (
    <div className="fixed inset-0 z-50 overflow-hidden">
      {/* Backdrop */}
      <div className="absolute inset-0 bg-black/50 backdrop-blur-xs transition-opacity" onClick={onClose} />

      <div className="fixed inset-y-0 right-0 max-w-full flex pl-10">
        <div className="w-screen max-w-md bg-white shadow-2xl flex flex-col border-l border-gray-100 animate-in slide-in-from-right duration-200">
          {/* Header */}
          <div className="p-4 sm:p-5 border-b border-gray-100 flex items-center justify-between bg-white shrink-0">
            <div className="flex items-center gap-2.5">
              <div className="w-9 h-9 rounded-xl bg-[var(--color-brand-light)] flex items-center justify-center text-[var(--color-brand)]">
                <Bell className="w-5 h-5" />
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <h3 className="font-bold text-gray-900 text-base sm:text-lg font-['Outfit']">Notifications</h3>
                  {unreadCount > 0 && (
                    <span className="px-2 py-0.5 rounded-full text-[10px] font-extrabold bg-[var(--color-brand)] text-white">
                      {unreadCount} new
                    </span>
                  )}
                </div>
                <p className="text-xs text-gray-500">Live booking tracker & updates</p>
              </div>
            </div>

            <div className="flex items-center gap-2">
              {/* Add Cart Quick Access Button inside Notification Drawer */}
              <button
                type="button"
                onClick={handleOpenCart}
                className="relative px-2.5 py-1.5 bg-gray-50 hover:bg-[var(--color-brand-light)] text-[var(--color-brand-hover)] border border-gray-200 hover:border-[var(--color-brand)] rounded-xl text-xs font-bold flex items-center gap-1.5 transition-colors cursor-pointer shadow-2xs"
                title="Open Cart"
              >
                <ShoppingBag className="w-3.5 h-3.5 text-[var(--color-brand)]" />
                <span>Cart</span>
                {totalCartCount > 0 && (
                  <span className="w-4 h-4 rounded-full bg-[var(--color-brand)] text-white text-[10px] font-black flex items-center justify-center">
                    {totalCartCount}
                  </span>
                )}
              </button>

              <button
                onClick={onClose}
                className="p-1.5 text-gray-400 hover:text-gray-600 rounded-lg hover:bg-gray-100 cursor-pointer"
                aria-label="Close notifications"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
          </div>

          {/* Subheader: Category Filter Pills & Actions */}
          <div className="px-4 py-2.5 bg-gray-50/80 border-b border-gray-100 flex items-center justify-between gap-2 overflow-x-auto">
            <div className="flex items-center gap-1">
              {(['ALL', 'ORDERS', 'OFFERS', 'ALERTS'] as const).map((tab) => (
                <button
                  key={tab}
                  type="button"
                  onClick={() => setFilterTab(tab)}
                  className={`px-2.5 py-1 rounded-lg text-[11px] font-bold transition-all cursor-pointer ${
                    filterTab === tab
                      ? 'bg-white text-[var(--color-brand-hover)] shadow-xs border border-brand-light'
                      : 'text-gray-500 hover:text-gray-800'
                  }`}
                >
                  {tab === 'ALL' ? 'All' : tab === 'ORDERS' ? 'Orders' : tab === 'OFFERS' ? 'Offers' : 'Alerts'}
                </button>
              ))}
            </div>

            {notifications.length > 0 && (
              <div className="flex items-center gap-2 shrink-0">
                {unreadCount > 0 && (
                  <button
                    onClick={markAllRead}
                    className="text-[11px] text-[var(--color-brand-hover)] font-semibold hover:underline cursor-pointer"
                  >
                    Read all
                  </button>
                )}
                <button
                  onClick={deleteAll}
                  title="Clear all notifications"
                  className="p-1 text-gray-400 hover:text-red-500 transition-colors cursor-pointer"
                >
                  <Trash2 className="w-3.5 h-3.5" />
                </button>
              </div>
            )}
          </div>

          {/* List */}
          <div className="flex-1 overflow-y-auto p-4 space-y-2.5">
            {filteredNotifications.length === 0 ? (
              <div className="text-center py-16 text-gray-400">
                <div className="w-12 h-12 rounded-2xl bg-gray-100 text-gray-400 flex items-center justify-center mx-auto mb-3">
                  <Bell className="w-6 h-6 opacity-40" />
                </div>
                <p className="text-sm font-bold text-gray-700">No {filterTab.toLowerCase()} notifications</p>
                <p className="text-xs text-gray-400 mt-1">Updates on your bookings and offers will appear here</p>
              </div>
            ) : (
              filteredNotifications.map((notif) => {
                const isOrder = notif.title.toLowerCase().includes('booking') || notif.title.toLowerCase().includes('srv');
                const isOffer = notif.title.toLowerCase().includes('off') || notif.title.toLowerCase().includes('voucher');

                return (
                  <div
                    key={notif.id}
                    onClick={() => {
                      if (notif.link && onNavigate) {
                        onNavigate(notif.link);
                        onClose();
                      }
                    }}
                    className={`relative group p-3.5 rounded-2xl border transition-all cursor-pointer flex gap-3 items-start ${
                      notif.read
                        ? 'bg-white border-gray-100 text-gray-700 hover:border-brand-light hover:shadow-2xs'
                        : 'bg-emerald-50/50 border-emerald-200 text-gray-900 shadow-xs'
                    }`}
                  >
                    {/* Visual Icon Badge */}
                    <div
                      className={`w-9 h-9 rounded-xl flex items-center justify-center shrink-0 mt-0.5 ${
                        isOrder
                          ? 'bg-emerald-100 text-emerald-800'
                          : isOffer
                          ? 'bg-amber-100 text-amber-800'
                          : 'bg-[var(--color-brand-light)] text-[var(--color-brand)]'
                      }`}
                    >
                      {isOrder ? (
                        <PackageCheck className="w-4 h-4" />
                      ) : isOffer ? (
                        <Tag className="w-4 h-4" />
                      ) : (
                        <Sparkles className="w-4 h-4" />
                      )}
                    </div>

                    <div className="flex-1 min-w-0">
                      <div className="flex items-start justify-between gap-1 mb-1">
                        <span className="font-bold text-xs text-gray-900 leading-tight">
                          {notif.title}
                        </span>
                        <div className="flex items-center gap-1.5 shrink-0">
                          <span className="text-[10px] text-gray-400">{notif.timestamp}</span>
                          <button
                            type="button"
                            onClick={(e) => deleteSingle(e, notif.id)}
                            className="opacity-0 group-hover:opacity-100 p-0.5 text-gray-400 hover:text-red-500 rounded transition-all cursor-pointer"
                            title="Delete notification"
                          >
                            <Trash2 className="w-3 h-3" />
                          </button>
                        </div>
                      </div>

                      <p className="text-xs text-gray-600 leading-relaxed pr-2">{notif.message}</p>

                      {notif.link && (
                        <div className="mt-2 flex items-center gap-1 text-[11px] font-bold text-[var(--color-brand-hover)]">
                          <span>{isOrder ? 'Track Booking Live' : isOffer ? 'View Offers' : 'Open Details'}</span>
                          <ArrowRight className="w-3 h-3 group-hover:translate-x-0.5 transition-transform" />
                        </div>
                      )}
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
