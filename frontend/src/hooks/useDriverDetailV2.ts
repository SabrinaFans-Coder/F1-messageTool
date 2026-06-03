import { useState, useEffect } from 'react';
import type { DriverDetail } from '../types';
import { fetchJson } from '../api/client';

export function useDriverDetailV2(driverNumber: number | null, year: number = 2026) {
  const [detail, setDetail] = useState<DriverDetail | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (driverNumber === null) return;

    let cancelled = false;
    setLoading(true);

    fetchJson<DriverDetail>(`/f1/driver-detail-v2?driverNumber=${driverNumber}&year=${year}`)
      .then((data) => {
        if (!cancelled) {
          setDetail(data);
          setError(null);
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : '加载车手详情失败');
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
  }, [driverNumber, year]);

  return { detail, loading, error };
}
