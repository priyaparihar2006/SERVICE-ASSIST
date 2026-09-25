import { useEffect, useState } from 'react';
import { api } from '../services/api';
import { Coupon } from '../types';
import { DEFAULT_COUPONS } from '../data/defaultCatalog';

export function useOffers() {
  const [offers, setOffers] = useState<Coupon[]>(DEFAULT_COUPONS);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    api('/offers')
      .then((d) => {
        if (d && Array.isArray(d.coupons) && d.coupons.length > 0) {
          setOffers(d.coupons);
        }
      })
      .catch((e) => {
        // Retain default coupons if backend is offline or static deploy
        console.warn('Could not fetch live offers, showing verified catalog offers:', e?.message);
      })
      .finally(() => setLoading(false));
  }, []);

  return { offers, error, loading };
}
