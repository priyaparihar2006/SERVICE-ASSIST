import { apiFetch } from '../../services/api';
import React, { useEffect, useState } from 'react';
import { NotificationItem } from '../../types';
import { Bell, Check, X, ArrowRight, Sparkles, AlertCircle } from 'lucide-react';

interface NotificationDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  onNavigate?: (path: string) => void;
}

export const NotificationDrawer: React.FC<NotificationDrawerProps> = ({ isOpen, onClose, onNavigate }) => {
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [loading, setLoading] = useState(false);

  const fetchNotifications = async () => {
    try {
      setLoading(true);
      const res = await apiFetch('/api/notifications');
      const data = await res.json();
      if (data.notifications) {
        setNotifications(data.notifications);
      }
    } catch (e) {
      alert(e.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (isOpen) {
      fetchNotifications();
    }
  }, [isOpen]);

  const markAllRead = async () => {
    try { await Promise.all(notifications.filter(n => !n.read).map(n => apiFetch(`/api/notifications/${n.id}/read`, { method: 'PATCH', body: '{}' }))); await fetchNotifications(); }
    catch (e) { alert(e.message); }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 overflow-hidden">
      {/* Backdrop */}
      <div className="absolute inset-0 bg-black/40 backdrop-blur-xs transition-opacity" onClick={onClose} />

      <div className="fixed inset-y-0 right-0 max-w-full flex pl-10">
        <div className="w-screen max-w-md bg-white shadow-2xl flex flex-col border-l border-gray-100 animate-in slide-in-from-right duration-200">
          {/* Header */}
          <div className="p-5 border-b border-gray-100 flex items-center justify-between">
            <div className="flex items-center gap-2.5">
              <div className="w-9 h-9 rounded-xl bg-[var(--color-brand-light)] flex items-center justify-center text-[var(--color-brand)]">
                <Bell className="w-5 h-5" />
              </div>
              <div>
                <h3 className="font-bold text-gray-900 text-lg font-['Outfit']">Notifications</h3>
                <p className="text-xs text-gray-500">Live booking updates & offers</p>
              </div>
            </div>
            <div className="flex items-center gap-2">
              <button
                onClick={markAllRead}
                className="text-xs text-[var(--color-brand-hover)] font-medium hover:underline px-2 py-1 rounded cursor-pointer"
              >
                Mark all read
              </button>
              <button
                onClick={onClose}
                className="p-1.5 text-gray-400 hover:text-gray-600 rounded-lg hover:bg-gray-100 cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
          </div>

          {/* List */}
          <div className="flex-1 overflow-y-auto p-4 space-y-3">
            {notifications.length === 0 ? (
              <div className="text-center py-12 text-gray-400">
                <Bell className="w-10 h-10 mx-auto mb-2 opacity-30" />
                <p className="text-sm">No new notifications</p>
              </div>
            ) : (
              notifications.map((notif) => (
                <div
                  key={notif.id}
                  onClick={() => {
                    if (notif.link && onNavigate) {
                      onNavigate(notif.link);
                      onClose();
                    }
                  }}
                  className={`p-4 rounded-xl border transition-all cursor-pointer ${
                    notif.read
                      ? 'bg-white border-gray-100 text-gray-700 hover:border-brand-light'
                      : 'bg-[var(--color-brand-soft)] border-brand-light text-gray-900 shadow-xs'
                  }`}
                >
                  <div className="flex items-start justify-between gap-2 mb-1">
                    <span className="font-semibold text-sm flex items-center gap-1.5">
                      {!notif.read && <span className="w-2 h-2 rounded-full bg-[var(--color-brand)] inline-block" />}
                      {notif.title}
                    </span>
                    <span className="text-[11px] text-gray-400 whitespace-nowrap">{notif.timestamp}</span>
                  </div>
                  <p className="text-xs text-gray-600 leading-relaxed">{notif.message}</p>
                  {notif.link && (
                    <div className="mt-2 flex items-center gap-1 text-[11px] font-semibold text-[var(--color-brand-hover)]">
                      <span>View details</span>
                      <ArrowRight className="w-3 h-3" />
                    </div>
                  )}
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
