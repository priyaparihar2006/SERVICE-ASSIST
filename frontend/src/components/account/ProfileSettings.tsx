import React, { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { api } from '../../services/api';
export function ProfileSettings() {
  const { user, refreshUser } = useAuth();
  const [message, setMessage] = useState('');
  const [saving, setSaving] = useState(false);
  const save = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSaving(true);
    setMessage('');
    const form = new FormData(event.currentTarget);
    try {
      await api('/auth/profile', {
        method: 'PUT',
        body: JSON.stringify({
          name: form.get('name'),
          phone: form.get('phone'),
        }),
      });
      await refreshUser();
      setMessage('Profile saved.');
    } catch (e) {
      setMessage(e.message);
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
      window.location.reload();
    } catch (e) {
      setMessage(e.message);
    } finally {
      setSaving(false);
    }
  };
  return (
    <details className="bg-white rounded-2xl p-5 border border-gray-100 my-5">
      <summary className="font-bold cursor-pointer">Profile & password</summary>
      <p role="status" className="my-2 text-sm">
        {message}
      </p>
      <form onSubmit={save} className="flex flex-wrap gap-3 my-3">
        <input
          aria-label="Full name"
          name="name"
          required
          defaultValue={user?.name}
          className="border rounded-xl p-2"
        />
        <input
          aria-label="Phone"
          name="phone"
          defaultValue={user?.phone}
          className="border rounded-xl p-2"
        />
        <button
          disabled={saving}
          className="bg-orange-500 text-white rounded-xl p-2"
        >
          Save profile
        </button>
      </form>
      <form onSubmit={changePassword} className="flex flex-wrap gap-3">
        <input
          aria-label="Current password"
          name="currentPassword"
          type="password"
          autoComplete="current-password"
          required
          placeholder="Current password"
          className="border rounded-xl p-2"
        />
        <input
          aria-label="New password"
          name="password"
          type="password"
          autoComplete="new-password"
          required
          minLength={12}
          maxLength={72}
          placeholder="New password (12+ characters)"
          className="border rounded-xl p-2"
        />
        <button disabled={saving} className="border rounded-xl p-2">
          Change password & sign out
        </button>
      </form>
    </details>
  );
}
