import { useState, useEffect } from 'react';
import type { SeasonStandings } from '../types';
import { fetchJson } from '../api/client';

export function useSeasonStandings(year: number = 2026) {
  const [standings, setStandings] = useState<SeasonStandings | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);

    fetchJson<SeasonStandings>(`/f1/season-standings?year=${year}`)
      .then((data) => {
        if (!cancelled) {
          setStandings(data);
          setError(null);
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : '加载积分榜失败');
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

  return { standings, loading, error };
}
