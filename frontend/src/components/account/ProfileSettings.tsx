import React, { useState, useRef } from 'react';
import { useAuth } from '../../context/AuthContext';
import { api } from '../../services/api';
import {
  Camera,
  Upload,
  User as UserIcon,
  Check,
  Sparkles,
  Lock,
  RefreshCw,
  Trash2,
  ChevronDown,
  ChevronUp,
} from 'lucide-react';

const PRESET_AVATARS = [
  {
    name: 'Priya (Customer)',
    url: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=200&q=80',
  },
  {
    name: 'Rajesh (Technician)',
    url: 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=200&q=80',
  },
  {
    name: 'Ananya (Corporate)',
    url: 'https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?auto=format&fit=crop&w=200&q=80',
  },
  {
    name: 'Vikram (Engineer)',
    url: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=200&q=80',
  },
  {
    name: 'Neha (Designer)',
    url: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=200&q=80',
  },
];

export function ProfileSettings() {
  const { user, updateProfile, refreshUser } = useAuth();
  const [name, setName] = useState(user?.name || '');
  const [phone, setPhone] = useState(user?.phone || '');
  const [avatarUrl, setAvatarUrl] = useState(user?.avatar || '');
  const [message, setMessage] = useState('');
  const [saving, setSaving] = useState(false);
  const [showPasswordSection, setShowPasswordSection] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (file.size > 2 * 1024 * 1024) {
      setMessage('Please choose an image under 2MB.');
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      if (typeof reader.result === 'string') {
        setAvatarUrl(reader.result);
        setMessage('Photo selected. Click "Save Profile & Photo" to apply.');
      }
    };
    reader.readAsDataURL(file);
  };

  const handleSelectPreset = (url: string) => {
    setAvatarUrl(url);
    setMessage('Avatar selected. Click "Save Profile & Photo" to apply.');
  };

  const handleRemovePhoto = () => {
    setAvatarUrl('');
    setMessage('Photo cleared. Click "Save Profile & Photo" to apply.');
  };

  const handleSaveProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setMessage('');
    try {
      await updateProfile({
        name,
        phone,
        avatar: avatarUrl || undefined,
      });
      await refreshUser().catch(() => {});
      setMessage('✓ Profile & photo updated successfully!');
      setTimeout(() => setMessage(''), 4000);
    } catch (err: any) {
      setMessage(err.message || 'Could not update profile.');
    } finally {
      setSaving(false);
    }
  };

  const changePassword = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    setSaving(true);
    try {
      await api('/auth/change-password', {
        method: 'POST',
        body: JSON.stringify({
          currentPassword: form.get('currentPassword'),
          password: form.get('password'),
        }),
      });
      setMessage('Password changed successfully.');
      window.location.reload();
    } catch (e: any) {
      setMessage(e.message || 'Could not change password.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="bg-white rounded-3xl p-6 sm:p-7 border border-brand-light shadow-xs mb-8 space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-4 border-b border-gray-100">
        <div>
          <h2 className="text-lg sm:text-xl font-black text-gray-900 font-['Outfit']">
            Profile & Account Settings
          </h2>
          <p className="text-xs text-gray-500 mt-0.5">
            Manage your personal profile photo, name, and login credentials.
          </p>
        </div>

        {message && (
          <div className={`px-3 py-1.5 rounded-xl text-xs font-bold animate-in fade-in ${
            message.includes('✓') || message.includes('success')
              ? 'bg-emerald-50 text-emerald-800 border border-emerald-200'
              : 'bg-amber-50 text-amber-800 border border-amber-200'
          }`}>
            {message}
          </div>
        )}
      </div>

      <form onSubmit={handleSaveProfile} className="space-y-6">
        {/* Section 1: Profile Photo Option */}
        <div>
          <label className="block text-xs font-bold uppercase tracking-wider text-gray-500 mb-3">
            Profile Photo
          </label>

          <div className="flex flex-col sm:flex-row sm:items-center gap-5">
            {/* Avatar Circle with Badge */}
            <div className="relative group shrink-0 self-center sm:self-auto">
              <div className="w-20 h-20 rounded-2xl overflow-hidden border-2 border-[var(--color-brand)] bg-gray-100 shadow-md">
                {avatarUrl ? (
                  <img
                    src={avatarUrl}
                    alt="User Profile"
                    className="w-full h-full object-cover"
                  />
                ) : (
                  <div className="w-full h-full flex items-center justify-center bg-[var(--color-brand-light)] text-[var(--color-brand)] font-black text-xl">
                    {name ? name.slice(0, 2).toUpperCase() : <UserIcon className="w-8 h-8 opacity-60" />}
                  </div>
                )}
              </div>

              <button
                type="button"
                onClick={() => fileInputRef.current?.click()}
                className="absolute -bottom-1 -right-1 w-7 h-7 rounded-xl bg-[var(--color-brand)] text-white flex items-center justify-center shadow-md hover:bg-[var(--color-brand-hover)] transition-colors cursor-pointer"
                title="Upload Photo"
              >
                <Camera className="w-3.5 h-3.5" />
              </button>
            </div>

            {/* Upload & Preset Options */}
            <div className="flex-1 space-y-3">
              <div className="flex flex-wrap items-center gap-2">
                <input
                  type="file"
                  ref={fileInputRef}
                  onChange={handleFileUpload}
                  accept="image/png, image/jpeg, image/webp"
                  className="hidden"
                />

                <button
                  type="button"
                  onClick={() => fileInputRef.current?.click()}
                  className="px-3.5 py-2 bg-gray-50 hover:bg-gray-100 text-gray-700 border border-gray-200 text-xs font-bold rounded-xl flex items-center gap-1.5 transition-colors cursor-pointer"
                >
                  <Upload className="w-3.5 h-3.5 text-[var(--color-brand)]" />
                  <span>Upload From Device</span>
                </button>

                {avatarUrl && (
                  <button
                    type="button"
                    onClick={handleRemovePhoto}
                    className="px-3 py-2 text-red-500 hover:text-red-700 hover:bg-red-50 text-xs font-semibold rounded-xl transition-colors cursor-pointer flex items-center gap-1"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                    <span>Remove Photo</span>
                  </button>
                )}
              </div>

              {/* Preset Quick Selectors */}
              <div>
                <span className="text-[11px] text-gray-500 block mb-1.5 font-medium">
                  Or pick a preset avatar:
                </span>
                <div className="flex items-center gap-2 overflow-x-auto pb-1">
                  {PRESET_AVATARS.map((preset) => (
                    <button
                      key={preset.url}
                      type="button"
                      onClick={() => handleSelectPreset(preset.url)}
                      className={`w-9 h-9 rounded-xl overflow-hidden border-2 transition-all cursor-pointer shrink-0 ${
                        avatarUrl === preset.url
                          ? 'border-[var(--color-brand)] ring-2 ring-[var(--color-brand-light)] scale-105'
                          : 'border-transparent hover:border-gray-300 opacity-80 hover:opacity-100'
                      }`}
                      title={preset.name}
                    >
                      <img src={preset.url} alt={preset.name} className="w-full h-full object-cover" />
                    </button>
                  ))}
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Section 2: Name & Phone */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-2 border-t border-gray-100">
          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-gray-500 mb-1.5">
              Full Name
            </label>
            <input
              type="text"
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Your full name"
              className="w-full px-3.5 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-xs sm:text-sm font-medium focus:outline-none focus:ring-2 focus:ring-[var(--color-brand)]/30 focus:border-[var(--color-brand)]"
            />
          </div>

          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-gray-500 mb-1.5">
              Phone Number
            </label>
            <input
              type="text"
              required
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              placeholder="e.g. +91 98765 43210"
              className="w-full px-3.5 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-xs sm:text-sm font-medium focus:outline-none focus:ring-2 focus:ring-[var(--color-brand)]/30 focus:border-[var(--color-brand)]"
            />
          </div>
        </div>

        {/* Save Button */}
        <div className="flex justify-end pt-2">
          <button
            type="submit"
            disabled={saving}
            className="px-5 py-2.5 bg-[var(--color-brand)] hover:bg-[var(--color-brand-hover)] text-white text-xs font-bold rounded-xl transition-all shadow-md shadow-brand/20 flex items-center gap-1.5 cursor-pointer disabled:opacity-50"
          >
            {saving ? <RefreshCw className="w-3.5 h-3.5 animate-spin" /> : <Check className="w-3.5 h-3.5" />}
            <span>Save Profile & Photo</span>
          </button>
        </div>
      </form>

      {/* Collapsible Password Change Section */}
      <div className="pt-4 border-t border-gray-100">
        <button
          type="button"
          onClick={() => setShowPasswordSection(!showPasswordSection)}
          className="flex items-center justify-between w-full text-xs font-bold text-gray-700 hover:text-gray-900 transition-colors cursor-pointer"
        >
          <div className="flex items-center gap-2">
            <Lock className="w-4 h-4 text-gray-400" />
            <span>Security & Change Password</span>
          </div>
          {showPasswordSection ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
        </button>

        {showPasswordSection && (
          <form onSubmit={changePassword} className="mt-4 p-4 bg-gray-50 rounded-2xl border border-gray-200/80 space-y-3">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <input
                name="currentPassword"
                type="password"
                required
                placeholder="Current password"
                className="w-full px-3 py-2 bg-white border border-gray-200 rounded-xl text-xs"
              />
              <input
                name="password"
                type="password"
                required
                minLength={8}
                placeholder="New password (8+ characters)"
                className="w-full px-3 py-2 bg-white border border-gray-200 rounded-xl text-xs"
              />
            </div>
            <button
              disabled={saving}
              className="px-4 py-2 bg-gray-900 hover:bg-black text-white text-xs font-bold rounded-xl transition-colors cursor-pointer disabled:opacity-50"
            >
              Update Password
            </button>
          </form>
        )}
      </div>
    </div>
  );
}
