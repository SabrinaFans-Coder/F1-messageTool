import { useState, useEffect, useCallback } from 'react';
import type { NewsPage } from '../types';
import { fetchJson } from '../api/client';

export const NEWS_CATEGORIES = [
  'ALL',
  'RACE',
  'DRIVER',
  'TEAM',
  'TECH',
  'TRANSFER',
  'GENERAL',
] as const;

const PAGE_SIZE = 20;

export function useNews() {
  const [category, setCategory] = useState<string>('ALL');
  const [page, setPage] = useState(0);
  const [newsPage, setNewsPage] = useState<NewsPage | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);

    const params = new URLSearchParams({ page: String(page), size: String(PAGE_SIZE) });
    if (category !== 'ALL') {
      params.set('category', category);
    }

    fetchJson<NewsPage>(`/news?${params.toString()}`)
      .then((data) => {
        if (!cancelled) {
          setNewsPage(data);
          setError(null);
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : '加载资讯失败');
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
  }, [category, page]);

  const selectCategory = useCallback((next: string) => {
    setCategory(next);
    setPage(0);
  }, []);

  return { category, selectCategory, page, setPage, newsPage, loading, error };
}
