import { QueryClient } from '@tanstack/react-query';

/**
 * 全局 QueryClient：staleTime 与后端长缓存节奏对齐，
 * 避免 V2 新页面频繁重复请求
 */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000,
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});
