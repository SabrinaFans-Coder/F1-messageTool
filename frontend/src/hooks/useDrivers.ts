import { useState, useEffect } from 'react';
import type { Driver } from '../types';
import { fetchJson } from '../api/client';

export function useDrivers(sessionKey: number | null) {
  const [drivers, setDrivers] = useState<Driver[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!sessionKey) return;

    let cancelled = false;
    setLoading(true);

    fetchJson<Driver[] | Driver>(`/f1/drivers?sessionKey=${sessionKey}`)
      .then((raw) => {
        if (!cancelled) {
          const data = Array.isArray(raw) ? raw : [raw];
          const unique = data.filter(
            (driver, index, arr) =>
              arr.findIndex((d) => d.driver_number === driver.driver_number) === index
          );
          setDrivers(unique);
          setError(null);
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : '加载车手数据失败');
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
  }, [sessionKey]);

  return { drivers, loading, error };
}
