import { useState, useEffect } from 'react';
import type { Driver } from '../types';
import { fetchJson } from '../api/client';

export function useDriversList(year: number = 2026) {
  const [drivers, setDrivers] = useState<Driver[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);

    fetchJson<Driver[]>(`/f1/drivers-list?year=${year}`)
      .then((data) => {
        if (!cancelled) {
          setDrivers(data);
          setError(null);
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : '加载车手列表失败');
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [year]);

  return { drivers, loading, error };
}
