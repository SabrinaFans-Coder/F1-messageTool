import { useState, useEffect } from 'react';
import type { CircuitHistory } from '../types';
import { fetchJson } from '../api/client';

export function useCircuitHistory(meetingKey: number | null) {
  const [history, setHistory] = useState<CircuitHistory | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!meetingKey) return;

    let cancelled = false;
    setLoading(true);

    fetchJson<CircuitHistory>(`/f1/circuit-history?meetingKey=${meetingKey}`)
      .then((data) => {
        if (!cancelled) {
          setHistory(data);
          setError(null);
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : '加载赛道历史失败');
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
  }, [meetingKey]);

  return { history, loading, error };
}
