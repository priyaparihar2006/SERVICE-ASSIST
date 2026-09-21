import React, { useState, useEffect } from 'react';
import { api, getAll } from '../../services/api';
export function ProfessionalSettings({ onSaved }: { onSaved: () => void }) {
  const [profile, setProfile] = useState<any>(null),
    [services, setServices] = useState<any[]>([]),
    [message, setMessage] = useState('');
  useEffect(() => {
    Promise.all([
      api('/professionals/profile'),
      getAll('/services', 'services'),
    ])
      .then(([p, s]) => {
        setProfile(p.professional);
        setServices(s);
      })
      .catch((e) => setMessage(e.message));
  }, []);
  const save = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    try {
      await api('/professionals/profile', {
        method: 'PUT',
        body: JSON.stringify({
          businessName: form.get('businessName'),
          description: form.get('description'),
          experience: Number(form.get('experience')),
          serviceArea: String(form.get('serviceArea'))
            .split(',')
            .map((s) => s.trim())
            .filter(Boolean),
          serviceIds: form.getAll('serviceIds'),
          isAvailableToday: form.get('available') === 'on',
        }),
      });
      const availability = form
        .getAll('days')
        .map((day) => ({
          dayOfWeek: Number(day),
          startMinute: Number(form.get('start')) * 60,
          endMinute: Number(form.get('end')) * 60,
        }));
      await api('/professionals/availability', {
        method: 'PUT',
        body: JSON.stringify({ availability }),
      });
      setMessage('Saved. Service changes require administrator verification.');
      onSaved();
    } catch (e) {
      setMessage(e.message);
    }
  };
  return (
    <details className="bg-white rounded-2xl border p-5 my-5">
      <summary className="font-bold cursor-pointer">
        Business profile, services & availability
      </summary>
      <p role="status">{message}</p>
      {profile ? (
        <form onSubmit={save} className="space-y-3 mt-4">
          <label className="block">
            Business name{' '}
            <input
              name="businessName"
              required
              defaultValue={profile.businessName}
              className="border rounded-xl p-2"
            />
          </label>
          <label className="block">
            About your work{' '}
            <textarea
              name="description"
              defaultValue={profile.description}
              className="border rounded-xl p-2"
            />
          </label>
          <label className="block">
            Years of experience{' '}
            <input
              name="experience"
              type="number"
              min="0"
              max="80"
              defaultValue={profile.experience}
              className="border rounded-xl p-2"
            />
          </label>
          <label className="block">
            Cities (comma separated){' '}
            <input
              name="serviceArea"
              required
              defaultValue={profile.serviceArea.join(', ')}
              className="border rounded-xl p-2 w-full"
            />
          </label>
          <fieldset>
            <legend>Services you provide</legend>
            <div className="grid sm:grid-cols-2 gap-2">
              {services.map((s) => (
                <label key={s.id} className="text-sm">
                  <input
                    type="checkbox"
                    name="serviceIds"
                    value={s.id}
                    defaultChecked={profile.serviceIds.includes(s.id)}
                  />{' '}
                  {s.name}
                </label>
              ))}
            </div>
          </fieldset>
          <fieldset>
            <legend>Working days</legend>
            {['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'].map((day, i) => (
              <label className="mr-3" key={day}>
                <input
                  type="checkbox"
                  name="days"
                  value={i}
                  defaultChecked={profile.availability.some(
                    (a) => a.dayOfWeek === i,
                  )}
                />{' '}
                {day}
              </label>
            ))}
          </fieldset>
          <label>
            From{' '}
            <input
              name="start"
              type="number"
              min="8"
              max="18"
              defaultValue="8"
              className="border p-2 w-20"
            />
          </label>{' '}
          <label>
            To{' '}
            <input
              name="end"
              type="number"
              min="9"
              max="19"
              defaultValue="19"
              className="border p-2 w-20"
            />
          </label>
          <label className="block">
            <input
              name="available"
              type="checkbox"
              defaultChecked={profile.isAvailableToday}
            />{' '}
            Accept assignments within these hours
          </label>
          <button className="bg-brand text-white rounded-xl p-3">
            Save business settings
          </button>
        </form>
      ) : (
        <p>Loading profile...</p>
      )}
    </details>
  );
}
