package com.springboot.backend.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.backend.entity.DataSnapshot;
import com.springboot.backend.repository.DataSnapshotRepository;
import com.springboot.backend.service.JolpicaService;
import com.springboot.backend.service.OpenF1Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * F1 赛事数据定时同步服务（混合触发）：
 * - 启动时与每日凌晨检查"是否有新完赛"
 * - 发现新完赛或对应年份快照缺失时才执行同步
 * 读路径统一走本地库，不再实时爬取上游 API
 *
 * @author F1-messageTool
 * @since 2026-08-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class F1DataSyncService {

    public static final int CURRENT_YEAR = 2026;

    private final DataSnapshotRepository snapshotRepository;
    private final OpenF1Service openF1Service;
    private final JolpicaService jolpicaService;
    private final ObjectMapper objectMapper;

    /** 每日凌晨 3 点检查；启动后 8 秒做一次追赶检查 */
    @Scheduled(cron = "0 0 3 * * *")
    @Scheduled(initialDelay = 8000, fixedDelay = Long.MAX_VALUE)
    public void checkAndSync() {
        try {
            ensureYearSynced(CURRENT_YEAR);
        } catch (Exception exception) {
            log.error("F1 数据同步失败", exception);
        }
    }

    /**
     * 确保指定年份的赛事数据已同步（供读路径调用，缺失时内联同步一次）
     */
    public synchronized void ensureYearSynced(int year) {
        if (snapshotRepository.existsByCacheKey(keyMeetings(year))) {
            syncNewCompletedSessions(year);
            return;
        }
        log.info("开始同步 {} 赛季 F1 数据", year);
        long start = System.currentTimeMillis();
        syncYear(year);
        log.info("{} 赛季 F1 数据同步完成, 耗时: {}ms", year, System.currentTimeMillis() - start);
    }

    private void syncYear(int year) {
        try {
            put(keyMeetings(year), openF1Service.fetchMeetings(year));
            String sessionsJson = openF1Service.fetchRaceSessions(year);
            put(keySessions(year), sessionsJson);

            List<Map<String, Object>> sessions = objectMapper.readValue(
                    sessionsJson, new com.fasterxml.jackson.core.type.TypeReference<>() {});
            int syncedCount = 0;
            String latestCompletedKey = null;
            for (Map<String, Object> session : sessions) {
                if (syncSessionPositions(session)) {
                    syncedCount++;
                }
                latestCompletedKey = resolveLatestCompletedSessionKey(session, latestCompletedKey);
            }
            // 车手名单快照（姓名/车队/涂装元数据），取最近一场已同步场次
            if (latestCompletedKey != null) {
                put("f1:drivers:" + year, openF1Service.fetchDrivers(
                        Integer.parseInt(latestCompletedKey.substring("f1:positions:".length()))));
            }

            put(keyStandings(year), jolpicaService.fetchDriverStandings(year));
            log.info("{} 赛季同步明细: 场次={}, 逐场结果={}", year, sessions.size(), syncedCount);
        } catch (Exception exception) {
            log.error("同步 {} 赛季数据失败", year, exception);
            throw new RuntimeException("同步 " + year + " 赛季数据失败", exception);
        }
    }

    private String resolveLatestCompletedSessionKey(Map<String, Object> session, String current) {
        int sessionKey = ((Number) session.get("session_key")).intValue();
        String key = keyPositions(sessionKey);
        return snapshotRepository.existsByCacheKey(key) ? key : current;
    }

    /**
     * 拉取单个场次的名次数据并入库；未完赛或已存在时跳过。
     *
     * @return 是否实际拉取
     */
    public boolean syncSessionPositions(Map<String, Object> session) {
        try {
            int sessionKey = ((Number) session.get("session_key")).intValue();
            String key = keyPositions(sessionKey);
            if (snapshotRepository.existsByCacheKey(key)) {
                return false;
            }
            LocalDateTime dateStart = LocalDateTime.parse(
                    ((String) session.get("date_start")).substring(0, 19));
            if (dateStart.isAfter(LocalDateTime.now())) {
                return false; // 未完赛
            }
            put(key, openF1Service.fetchPositions(sessionKey));
            return true;
        } catch (Exception exception) {
            log.warn("场次 {} 名次同步失败, 跳过: {}", session.get("session_key"), exception.getMessage());
            return false;
        }
    }

    /**
     * 增量补拉：只同步库中尚无名次数据的已完赛场次；有新完赛时刷新积分榜快照
     */
    private void syncNewCompletedSessions(int year) {
        try {
            Optional<DataSnapshot> sessionsSnap = snapshotRepository.findByCacheKey(keySessions(year));
            if (sessionsSnap.isEmpty()) {
                syncYear(year);
                return;
            }
            List<Map<String, Object>> sessions = objectMapper.readValue(
                    sessionsSnap.get().getJsonContent(),
                    new com.fasterxml.jackson.core.type.TypeReference<>() {});
            int pulled = 0;
            for (Map<String, Object> session : sessions) {
                if (syncSessionPositions(session)) {
                    pulled++;
                }
            }
            if (pulled > 0 || !snapshotRepository.existsByCacheKey(keyStandings(year))
                    || !snapshotRepository.existsByCacheKey("f1:drivers:" + year)) {
                // 有新完赛，或上次同步中断导致快照缺失：补齐派生数据
                String latestKey = snapshotRepository.findByCacheKeyStartingWith("f1:positions:").stream()
                        .map(s -> s.getCacheKey().substring("f1:positions:".length()))
                        .max(java.util.Comparator.comparingInt(Integer::parseInt))
                        .orElse(null);
                if (latestKey != null && !snapshotRepository.existsByCacheKey("f1:drivers:" + year)) {
                    put("f1:drivers:" + year, openF1Service.fetchDrivers(Integer.parseInt(latestKey)));
                }
                if (!snapshotRepository.existsByCacheKey(keyStandings(year))) {
                    put(keyStandings(year), jolpicaService.fetchDriverStandings(year));
                }
                log.info("增量同步完成, 新拉取场次: {}", pulled);
            }
        } catch (Exception exception) {
            log.error("增量同步失败, year: {}", year, exception);
        }
    }

    private void put(String cacheKey, String json) {
        DataSnapshot snapshot = snapshotRepository.findByCacheKey(cacheKey).orElseGet(DataSnapshot::new);
        snapshot.setCacheKey(cacheKey);
        snapshot.setJsonContent(json);
        snapshotRepository.save(snapshot);
    }

    public static String keyMeetings(int year) { return "f1:meetings:" + year; }
    public static String keySessions(int year) { return "f1:sessions:" + year; }
    public static String keyPositions(int sessionKey) { return "f1:positions:" + sessionKey; }
    public static String keyStandings(int year) { return "f1:standings:" + year; }
}
