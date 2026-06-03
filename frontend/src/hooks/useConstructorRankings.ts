import { useState, useEffect } from 'react';
import type { ConstructorRankings } from '../types';
import { fetchJson } from '../api/client';

export function useConstructorRankings(year: number = 2026) {
  const [rankings, setRankings] = useState<ConstructorRankings | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);

    fetchJson<ConstructorRankings>(`/f1/constructor-rankings?year=${year}`)
      .then((data) => {
        if (!cancelled) {
          setRankings(data);
          setError(null);
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : '加载车队排名失败');
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

  return { rankings, loading, error };
}
