import { useEffect, useState } from 'react';
import { api } from '../services/api';
import { Coupon } from '../types';
export function useOffers() {
  const [offers, setOffers] = useState<Coupon[]>([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  useEffect(() => {
    api('/offers')
      .then((d) => setOffers(d.coupons))
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);
  return { offers, error, loading };
}
