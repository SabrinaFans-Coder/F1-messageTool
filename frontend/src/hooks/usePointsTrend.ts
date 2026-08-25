import { useQuery } from '@tanstack/react-query';
import type { PointsTrend } from '../types';
import { fetchJson } from '../api/client';

export function usePointsTrend(year: number, top: number) {
  return useQuery<PointsTrend>({
    queryKey: ['f1', 'points-trend', year, top],
    queryFn: () =>
      fetchJson<PointsTrend>(`/f1/points-trend?year=${year}&top=${top}`, { timeout: 60000 }),
    // 后端冷缓存时全赛季聚合较慢，放宽单次请求超时
    staleTime: 60 * 60 * 1000,
  });
}
