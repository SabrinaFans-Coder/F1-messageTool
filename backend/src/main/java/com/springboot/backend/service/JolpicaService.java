package com.springboot.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Jolpica F1 API 服务（Ergast 继任者）
 * 提供现成的车手/车队积分榜数据，单次请求即可获取
 *
 * @author F1-messageTool
 * @since 2026-06-03
 */
@Slf4j
@Service
public class JolpicaService {

    private static final String BASE_URL = "https://api.jolpi.ca/ergast/f1";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private final WebClient webClient;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public JolpicaService() {
        this.webClient = WebClient.builder()
                .baseUrl(BASE_URL)
                .build();
    }

    /**
     * 获取车手积分榜
     *
     * @param year 赛季年份
     * @return Jolpica 原始 JSON
     */
    public String fetchDriverStandings(int year) {
        String cacheKey = "jolpica_driver_" + year;
        return getFromCacheOrFetch(cacheKey, "/" + year + "/driverstandings.json");
    }

    /**
     * 获取车队积分榜
     *
     * @param year 赛季年份
     * @return Jolpica 原始 JSON
     */
    public String fetchConstructorStandings(int year) {
        String cacheKey = "jolpica_constructor_" + year;
        return getFromCacheOrFetch(cacheKey, "/" + year + "/constructorstandings.json");
    }

    private String getFromCacheOrFetch(String cacheKey, String path) {
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.debug("Jolpica 缓存命中, cacheKey: {}", cacheKey);
            return cached.data;
        }

        log.info("调用 Jolpica API, path: {}", path);
        try {
            String response = webClient.get()
                    .uri(path)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            cache.put(cacheKey, new CacheEntry(response));
            log.info("Jolpica API 调用成功, path: {}", path);
            return response;
        } catch (Exception exception) {
            log.error("Jolpica API 调用失败, path: {}", path, exception);
            throw new com.springboot.backend.common.BusinessException(
                    "获取 Jolpica 数据失败，path: " + path, exception);
        }
    }

    private static class CacheEntry {
        final String data;
        final long expireAt;

        CacheEntry(String data) {
            this.data = data;
            this.expireAt = System.currentTimeMillis() + CACHE_TTL.toMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expireAt;
        }
    }
}
