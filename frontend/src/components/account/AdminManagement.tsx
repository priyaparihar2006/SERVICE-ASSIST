import React, { useState, useEffect } from 'react';
import { api, getAll } from '../../services/api';
export function AdminManagement({ onChanged }: { onChanged: () => void }) {
  const [pros, setPros] = useState<any[]>([]),
    [categories, setCategories] = useState<any[]>([]),
    [services, setServices] = useState<any[]>([]),
    [message, setMessage] = useState('');
  const load = async () => {
    try {
      const [p, c, s] = await Promise.all([
        api('/admin/professionals?limit=100'),
        api('/admin/categories'),
        getAll('/admin/services', 'services'),
      ]);
      setPros(p.professionals);
      setCategories(c.categories);
      setServices(s);
    } catch (e) {
      setMessage(e.message);
    }
  };
  useEffect(() => {
    load();
  }, []);
  const mutate = async (path: string, method: string, body: any) => {
    try {
      await api(path, { method, body: JSON.stringify(body) });
      setMessage('Saved.');
      await load();
      onChanged();
    } catch (e) {
      setMessage(e.message);
    }
  };
  const saveCategory = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const f = new FormData(e.currentTarget);
    const id = f.get('id');
    mutate(
      id ? `/admin/categories/${id}` : '/admin/categories',
      id ? 'PUT' : 'POST',
      {
        name: f.get('name'),
        slug: f.get('slug'),
        description: f.get('description'),
        image: f.get('image'),
        isActive: f.get('active') === 'on',
      },
    );
  };
  const saveService = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const f = new FormData(e.currentTarget);
    const id = f.get('id');
    mutate(id ? `/services/${id}` : '/services', id ? 'PUT' : 'POST', {
      name: f.get('name'),
      slug: f.get('slug'),
      categoryId: f.get('categoryId'),
      description: f.get('description'),
      shortDesc: f.get('shortDesc'),
      steps: String(f.get('steps') || '').split('\n').map(s => s.trim()).filter(Boolean),
      image: f.get('image'),
      ...(!id || services.find(s => s.id === id)?.variants.length === 1 ? {
        startingPrice: Number(f.get('price')),
        duration: Number(f.get('duration')),
      } : {}),
      locations: String(f.get('locations'))
        .split(',')
        .map((s) => s.trim())
        .filter(Boolean),
      isActive: true,
    });
  };
  return (
    <details className="bg-white rounded-2xl p-5 border my-5">
      <summary className="font-bold cursor-pointer">
        Manage professionals, categories & services
      </summary>
      <p role="status" className="my-3">
        {message}
      </p>
      <h3 className="font-bold">Professional verification</h3>
      <div className="space-y-2 my-3">
        {pros.map((p) => (
          <div key={p.id} className="flex flex-wrap gap-3 items-center">
            <span>
              {p.name} · {p.verificationStatus}
            </span>
            <select
              aria-label={`Verify ${p.name}`}
              value={p.verificationStatus}
              onChange={(e) =>
                mutate(`/admin/professionals/${p.id}/verification`, 'PUT', {
                  verificationStatus: e.target.value,
                })
              }
              className="border p-2 rounded-xl"
            >
              {['PENDING', 'VERIFIED', 'REJECTED'].map((v) => (
                <option key={v}>{v}</option>
              ))}
            </select>
          </div>
        ))}
      </div>
      <h3 className="font-bold">Categories</h3>
      {categories.map((c) => (
        <div key={c.id} className="flex gap-3 my-2">
          <span>
            {c.name} ({c.isActive ? 'active' : 'inactive'})
          </span>
          <button
            onClick={() =>
              mutate(`/admin/categories/${c.id}`, 'PUT', {
                isActive: !c.isActive,
              })
            }
            className="underline"
          >
            {c.isActive ? 'Deactivate' : 'Activate'}
          </button>
        </div>
      ))}
      <form onSubmit={saveCategory} className="grid sm:grid-cols-2 gap-2 my-3">
        <select name="id" aria-label="Category to edit" className="border p-2">
          <option value="">Create category</option>
          {categories.map((c) => (
            <option key={c.id} value={c.id}>
              Update {c.name}
            </option>
          ))}
        </select>
        {['name', 'slug', 'description', 'image'].map((n) => (
          <input
            key={n}
            name={n}
            required
            aria-label={`Category ${n}`}
            placeholder={n}
            className="border rounded-xl p-2"
          />
        ))}
        <label>
          <input name="active" type="checkbox" defaultChecked /> Active
        </label>
        <button className="bg-orange-500 text-white rounded-xl p-2">
          Save category
        </button>
      </form>
      <h3 className="font-bold">Services</h3>
      {services.map((s) => (
        <details key={s.id} className="my-2 border rounded-xl p-3">
          <summary className="cursor-pointer">{s.name} ({s.isActive ? 'active' : 'inactive'})</summary>
          <button
            onClick={() => mutate(`/services/${s.id}`, 'PUT', { isActive: !s.isActive })}
            className="underline"
          >
            {s.isActive ? 'Deactivate' : 'Activate'}
          </button>
          {s.variants.map(v => <form key={v.id} className="flex flex-wrap gap-2 mt-3" onSubmit={e => { e.preventDefault(); const form = new FormData(e.currentTarget); mutate(`/services/${s.id}/variants/${v.id}`, 'PUT', { name: form.get('name'), price: Number(form.get('price')), durationMin: Number(form.get('duration')) }); }}>
            <input name="name" aria-label="Package name" defaultValue={v.name} required className="border rounded p-2" />
            <input name="price" aria-label="Package price" type="number" min="1" step="0.01" defaultValue={v.price} required className="border rounded p-2 w-24" />
            <input name="duration" aria-label="Package duration" type="number" min="15" max="480" defaultValue={v.durationMin} required className="border rounded p-2 w-24" />
            <button className="border rounded p-2">Save package</button>
          </form>)}
        </details>
      ))}
      <form onSubmit={saveService} className="grid sm:grid-cols-2 gap-2 my-3">
        <textarea name="steps" aria-label="Service procedure" placeholder="Service procedure: one step per line" className="border rounded-xl p-2" />
        <select name="id" aria-label="Service to edit" className="border p-2">
          <option value="">Create service</option>
          {services.map((s) => (
            <option key={s.id} value={s.id}>
              Update {s.name}
            </option>
          ))}
        </select>
        <select
          name="categoryId"
          aria-label="Service category"
          className="border p-2"
        >
          {categories
            .filter((c) => c.isActive)
            .map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
        </select>
        {['name', 'slug', 'description', 'shortDesc', 'image', 'locations'].map(
          (n) => (
            <input
              key={n}
              name={n}
              required
              aria-label={`Service ${n}`}
              placeholder={n === 'locations' ? 'Cities, comma separated' : n}
              className="border rounded-xl p-2"
            />
          ),
        )}
        <input
          name="price"
          type="number"
          min="1"
          required
          placeholder="Price (INR)"
          aria-label="Service price"
          className="border rounded-xl p-2"
        />
        <input
          name="duration"
          type="number"
          min="15"
          max="480"
          required
          placeholder="Minutes"
          aria-label="Duration"
          className="border rounded-xl p-2"
        />
        <button className="bg-orange-500 text-white rounded-xl p-2">
          Save service
        </button>
      </form>
    </details>
  );
}
