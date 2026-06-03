import { useState, useEffect } from 'react';
import type { SeasonRankings } from '../types';
import { fetchJson } from '../api/client';

export function useSeasonRankings(year: number = 2026, limit: number = 5) {
  const [rankings, setRankings] = useState<SeasonRankings | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);

    fetchJson<SeasonRankings>(`/f1/season-rankings?year=${year}&limit=${limit}`)
      .then((data) => {
        if (!cancelled) {
          setRankings(data);
          setError(null);
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : '加载赛季排名失败');
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
  }, [year, limit]);

  return { rankings, loading, error };
}
