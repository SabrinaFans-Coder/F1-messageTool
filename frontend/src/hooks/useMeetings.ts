import { useState, useEffect } from 'react';
import type { Meeting } from '../types';
import { fetchJson } from '../api/client';

export function useMeetings(year: number = 2026) {
  const [meetings, setMeetings] = useState<Meeting[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);

    fetchJson<Meeting[] | Meeting>(`/f1/meetings?year=${year}`)
      .then((raw) => {
        if (!cancelled) {
          const data = Array.isArray(raw) ? raw : [raw];
          const races = data.filter((m) => !m.is_cancelled);
          setMeetings(races);
          setError(null);
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : '加载赛事日历失败');
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

  return { meetings, loading, error };
}
