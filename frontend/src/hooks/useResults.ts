import { useState, useEffect } from 'react';
import type { Position, Session } from '../types';
import { fetchJson } from '../api/client';

export function useResults() {
  const [positions, setPositions] = useState<Position[]>([]);
  const [latestSession, setLatestSession] = useState<Session | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);

    fetchJson<Session[] | Session>('/f1/sessions/latest')
      .then((raw) => {
        if (cancelled) return;
        const sessions = Array.isArray(raw) ? raw : [raw];
        if (!sessions.length) return;
        // Find the latest past race session
        const now = new Date();
        const pastSessions = sessions
          .filter((s) => s.session_name === 'Race' && new Date(s.date_start) <= now)
          .sort((a, b) => new Date(b.date_start).getTime() - new Date(a.date_start).getTime());
        const session = pastSessions[0] ?? sessions[sessions.length - 1];
        if (!session?.session_key) return;
        setLatestSession(session);
        return fetchJson<Position[]>(`/f1/positions?sessionKey=${session.session_key}`);
      })
      .then((pos) => {
        if (!cancelled && pos) {
          const latestByDriver = new Map<number, Position>();
          for (const p of pos) {
            const existing = latestByDriver.get(p.driver_number);
            if (!existing || new Date(p.date) > new Date(existing.date)) {
              latestByDriver.set(p.driver_number, p);
            }
          }
          const latest = [...latestByDriver.values()].sort(
            (a, b) => a.position - b.position
          );
          setPositions(latest);
          setError(null);
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : '加载比赛结果失败');
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
  }, []);

  return { positions, latestSession, loading, error };
}
