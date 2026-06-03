package com.springboot.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

/**
 * OpenF1 API 调用服务
 * 负责代理 OpenF1 API 请求并提供内存缓存（TTL 5分钟）
 *
 * @author F1-messageTool
 * @since 2026-06-03
 */
@Slf4j
@Service
public class OpenF1Service {

    private static final String BASE_URL = "https://api.openf1.org/v1";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);
    private static final long API_DELAY_MS = 500;

    private final WebClient webClient;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    /** 全局信号量：限制同时只有一个线程在调用 OpenF1 API */
    private final Semaphore apiSemaphore = new Semaphore(1);
    /** 上次 API 调用时间戳，用于全局限流 */
    private volatile long lastApiCallTime = 0;

    public OpenF1Service() {
        this.webClient = WebClient.builder()
                .baseUrl(BASE_URL)
                .build();
    }

    /**
     * 获取指定年份的赛事日历
     *
     * @param year 赛季年份
     * @return 赛事列表 JSON 字符串
     */
    public String fetchMeetings(int year) {
        String cacheKey = "meetings_" + year;
        return getFromCacheOrFetch(cacheKey, "/meetings?year=" + year);
    }

    /**
     * 获取指定年份的所有会话
     *
     * @param year 赛季年份
     * @return 会话列表 JSON 字符串
     */
    public String fetchSessions(int year) {
        String cacheKey = "sessions_" + year;
        return getFromCacheOrFetch(cacheKey, "/sessions?year=" + year);
    }

    /**
     * 获取最新比赛会话
     *
     * @return 最新会话 JSON 字符串
     */
    public String fetchLatestRaceSession() {
        String cacheKey = "sessions_latest";
        return getFromCacheOrFetch(cacheKey, "/sessions?session_type=Race&session_key=latest");
    }

    /**
     * 获取指定会话的车手列表
     *
     * @param sessionKey 会话标识
     * @return 车手列表 JSON 字符串
     */
    public String fetchDrivers(int sessionKey) {
        String cacheKey = "drivers_" + sessionKey;
        return getFromCacheOrFetch(cacheKey, "/drivers?session_key=" + sessionKey);
    }

    /**
     * 获取指定会话的比赛位置
     *
     * @param sessionKey 会话标识
     * @return 位置列表 JSON 字符串
     */
    public String fetchPositions(int sessionKey) {
        String cacheKey = "positions_" + sessionKey;
        return getFromCacheOrFetch(cacheKey, "/position?session_key=" + sessionKey);
    }

    /**
     * 获取赛道历史数据（按 meeting_key 精确查询，正赛和冲刺赛分开）
     *
     * @param meetingKey 大奖赛标识
     * @return 赛道历史 JSON 字符串
     */
    public String fetchCircuitHistory(int meetingKey) {
        String cacheKey = "circuit_history_" + meetingKey;
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.debug("缓存命中, cacheKey: {}", cacheKey);
            return cached.data;
        }

        log.info("获取赛道历史数据, meetingKey: {}", meetingKey);
        try {
            // 先获取当前 meeting 信息
            String meetingJson = fetchWithRetry("/meetings?meeting_key=" + meetingKey);
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.List<java.util.Map<String, Object>> meetings = mapper.readValue(
                    meetingJson, new com.fasterxml.jackson.core.type.TypeReference<>() {});

            if (meetings.isEmpty()) {
                throw new com.springboot.backend.common.BusinessException("未找到该大奖赛, meetingKey: " + meetingKey);
            }

            java.util.Map<String, Object> currentMeeting = meetings.get(0);
            String circuitShortName = (String) currentMeeting.getOrDefault("circuit_short_name", "");

            // 用 circuit_short_name 获取同赛道的所有年份 Race 会话
            // OpenF1 中 session_type=Race 包含正赛和冲刺赛，用 session_name 区分
            String sessionsJson = fetchWithRetry("/sessions?circuit_short_name=" + circuitShortName + "&session_type=Race");

            java.util.List<java.util.Map<String, Object>> sessions = mapper.readValue(
                    sessionsJson, new com.fasterxml.jackson.core.type.TypeReference<>() {});

            // 按 meeting_key 去重，每个 meeting 只保留一个正赛（session_name="Race"）
            java.util.Map<Integer, java.util.Map<String, Object>> sessionsByMeeting = new java.util.LinkedHashMap<>();
            for (java.util.Map<String, Object> s : sessions) {
                String sName = (String) s.getOrDefault("session_name", "");
                if ("Race".equals(sName)) {
                    int mk = ((Number) s.get("meeting_key")).intValue();
                    sessionsByMeeting.putIfAbsent(mk, s);
                }
            }

            // 获取当前 meeting 的所有会话，筛选出冲刺赛（session_name="Sprint"）
            java.util.List<java.util.Map<String, Object>> sprintSessions = new java.util.ArrayList<>();
            String allSessionsJson = fetchWithRetry("/sessions?meeting_key=" + meetingKey);
            java.util.List<java.util.Map<String, Object>> allSessions = mapper.readValue(
                    allSessionsJson, new com.fasterxml.jackson.core.type.TypeReference<>() {});
            for (java.util.Map<String, Object> s : allSessions) {
                String sName = (String) s.getOrDefault("session_name", "");
                if ("Sprint".equals(sName)) {
                    sprintSessions.add(s);
                    break;
                }
            }

            java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
            result.put("circuit_short_name", circuitShortName);
            result.put("country_name", currentMeeting.getOrDefault("country_name", ""));
            result.put("circuit_image", currentMeeting.getOrDefault("circuit_image", ""));

            java.util.List<java.util.Map<String, Object>> years = new java.util.ArrayList<>();
            java.util.Map<Integer, java.util.Map<String, Object>> driverStats = new java.util.LinkedHashMap<>();
            int[] pointsTable = {25, 18, 15, 12, 10, 8, 6, 4, 2, 1};

            for (java.util.Map<String, Object> session : sessionsByMeeting.values()) {
                int year = ((Number) session.get("year")).intValue();
                int sessionKey = ((Number) session.get("session_key")).intValue();
                int mk = ((Number) session.get("meeting_key")).intValue();

                    String positionsJson = fetchWithRetry("/position?session_key=" + sessionKey);

                java.util.List<java.util.Map<String, Object>> positions = mapper.readValue(
                        positionsJson, new com.fasterxml.jackson.core.type.TypeReference<>() {});

                java.util.Map<Integer, java.util.Map<String, Object>> latestByDriver = new java.util.LinkedHashMap<>();
                for (java.util.Map<String, Object> pos : positions) {
                    int driverNum = ((Number) pos.get("driver_number")).intValue();
                    latestByDriver.put(driverNum, pos);
                }

                    String driversJson = fetchWithRetry("/drivers?session_key=" + sessionKey);

                java.util.List<java.util.Map<String, Object>> drivers = mapper.readValue(
                        driversJson, new com.fasterxml.jackson.core.type.TypeReference<>() {});

                java.util.Map<Integer, java.util.Map<String, Object>> driverInfo = new java.util.LinkedHashMap<>();
                for (java.util.Map<String, Object> d : drivers) {
                    int num = ((Number) d.get("driver_number")).intValue();
                    driverInfo.put(num, d);
                }

                java.util.List<java.util.Map<String, Object>> yearResults = new java.util.ArrayList<>();
                java.util.List<java.util.Map<String, Object>> sortedPositions = new java.util.ArrayList<>(latestByDriver.values());
                sortedPositions.sort((a, b) -> Integer.compare(
                        ((Number) a.get("position")).intValue(), ((Number) b.get("position")).intValue()));

                for (java.util.Map<String, Object> pos : sortedPositions) {
                    int driverNum = ((Number) pos.get("driver_number")).intValue();
                    int position = ((Number) pos.get("position")).intValue();
                    java.util.Map<String, Object> info = driverInfo.getOrDefault(driverNum, java.util.Map.of());

                    java.util.Map<String, Object> entry = new java.util.LinkedHashMap<>();
                    entry.put("position", position);
                    entry.put("driver_number", driverNum);
                    entry.put("driver_name", info.getOrDefault("full_name", "Unknown"));
                    entry.put("name_acronym", info.getOrDefault("name_acronym", "???"));
                    entry.put("team_name", info.getOrDefault("team_name", "Unknown"));
                    entry.put("team_colour", info.getOrDefault("team_colour", "888888"));
                    yearResults.add(entry);

                    int points = (position >= 1 && position <= 10) ? pointsTable[position - 1] : 0;
                    java.util.Map<String, Object> stats = driverStats.computeIfAbsent(driverNum, k -> {
                        java.util.Map<String, Object> s = new java.util.LinkedHashMap<>();
                        s.put("driver_number", k);
                        s.put("driver_name", info.getOrDefault("full_name", "Unknown"));
                        s.put("name_acronym", info.getOrDefault("name_acronym", "???"));
                        s.put("team_name", info.getOrDefault("team_name", "Unknown"));
                        s.put("team_colour", info.getOrDefault("team_colour", "888888"));
                        s.put("totalPoints", 0);
                        s.put("wins", 0);
                        s.put("podiums", 0);
                        s.put("races", 0);
                        return s;
                    });
                    stats.put("totalPoints", ((Number) stats.get("totalPoints")).intValue() + points);
                    if (position == 1) stats.put("wins", ((Number) stats.get("wins")).intValue() + 1);
                    if (position <= 3) stats.put("podiums", ((Number) stats.get("podiums")).intValue() + 1);
                    stats.put("races", ((Number) stats.get("races")).intValue() + 1);
                }

                java.util.Map<String, Object> yearEntry = new java.util.LinkedHashMap<>();
                yearEntry.put("year", year);
                yearEntry.put("session_key", sessionKey);
                yearEntry.put("meeting_key", mk);
                yearEntry.put("date_start", session.get("date_start"));
                yearEntry.put("results", yearResults);
                years.add(yearEntry);
            }

            // 获取当前年份冲刺赛结果
            java.util.List<java.util.Map<String, Object>> sprintResults = new java.util.ArrayList<>();
            for (java.util.Map<String, Object> sprintSession : sprintSessions) {
                int sprintKey = ((Number) sprintSession.get("session_key")).intValue();

                    String sprintPosJson = fetchWithRetry("/position?session_key=" + sprintKey);

                java.util.List<java.util.Map<String, Object>> sprintPositions = mapper.readValue(
                        sprintPosJson, new com.fasterxml.jackson.core.type.TypeReference<>() {});

                java.util.Map<Integer, java.util.Map<String, Object>> sprintLatest = new java.util.LinkedHashMap<>();
                for (java.util.Map<String, Object> pos : sprintPositions) {
                    int driverNum = ((Number) pos.get("driver_number")).intValue();
                    sprintLatest.put(driverNum, pos);
                }

                    String sprintDriversJson = fetchWithRetry("/drivers?session_key=" + sprintKey);

                java.util.List<java.util.Map<String, Object>> sprintDrivers = mapper.readValue(
                        sprintDriversJson, new com.fasterxml.jackson.core.type.TypeReference<>() {});

                java.util.Map<Integer, java.util.Map<String, Object>> sprintDriverInfo = new java.util.LinkedHashMap<>();
                for (java.util.Map<String, Object> d : sprintDrivers) {
                    int num = ((Number) d.get("driver_number")).intValue();
                    sprintDriverInfo.put(num, d);
                }

                java.util.List<java.util.Map<String, Object>> sprintSorted = new java.util.ArrayList<>(sprintLatest.values());
                sprintSorted.sort((a, b) -> Integer.compare(
                        ((Number) a.get("position")).intValue(), ((Number) b.get("position")).intValue()));

                for (java.util.Map<String, Object> pos : sprintSorted) {
                    int driverNum = ((Number) pos.get("driver_number")).intValue();
                    int position = ((Number) pos.get("position")).intValue();
                    java.util.Map<String, Object> info = sprintDriverInfo.getOrDefault(driverNum, java.util.Map.of());

                    java.util.Map<String, Object> entry = new java.util.LinkedHashMap<>();
                    entry.put("position", position);
                    entry.put("driver_number", driverNum);
                    entry.put("driver_name", info.getOrDefault("full_name", "Unknown"));
                    entry.put("name_acronym", info.getOrDefault("name_acronym", "???"));
                    entry.put("team_name", info.getOrDefault("team_name", "Unknown"));
                    entry.put("team_colour", info.getOrDefault("team_colour", "888888"));
                    sprintResults.add(entry);
                }
            }

            years.sort((a, b) -> Integer.compare(((Number) b.get("year")).intValue(), ((Number) a.get("year")).intValue()));

            java.util.List<java.util.Map<String, Object>> circuitRanking = new java.util.ArrayList<>(driverStats.values());
            circuitRanking.sort((a, b) -> Integer.compare(
                    ((Number) b.get("totalPoints")).intValue(), ((Number) a.get("totalPoints")).intValue()));

            result.put("years", years);
            result.put("sprintResults", sprintResults);
            result.put("circuitRanking", circuitRanking);

            String json = mapper.writeValueAsString(result);
            cache.put(cacheKey, new CacheEntry(json));
            log.info("赛道历史数据获取完成, meetingKey: {}, years: {}", meetingKey, years.size());
            return json;
        } catch (Exception exception) {
            log.error("获取赛道历史数据失败, meetingKey: {}", meetingKey, exception);
            throw new com.springboot.backend.common.BusinessException(
                    "获取赛道历史数据失败，meetingKey: " + meetingKey, exception);
        }
    }

    /**
     * 获取赛季近期排名（最近N场正赛）
     *
     * @param year 赛季年份
     * @param limit 场次数量
     * @return 赛季排名 JSON 字符串
     */
    public String fetchSeasonRankings(int year, int limit) {
        String cacheKey = "season_rankings_" + year + "_" + limit;
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.debug("缓存命中, cacheKey: {}", cacheKey);
            return cached.data;
        }

        log.info("获取赛季排名, year: {}, limit: {}", year, limit);
        try {
            String sessionsJson = fetchWithRetry("/sessions?year=" + year + "&session_type=Race&session_name=Race");

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.List<java.util.Map<String, Object>> sessions = mapper.readValue(
                    sessionsJson, new com.fasterxml.jackson.core.type.TypeReference<>() {});

            sessions = filterPastSessions(sessions);

            sessions.sort((a, b) -> {
                String dateA = (String) a.getOrDefault("date_start", "");
                String dateB = (String) b.getOrDefault("date_start", "");
                return dateB.compareTo(dateA);
            });

            java.util.List<java.util.Map<String, Object>> recentSessions = sessions.subList(
                    0, Math.min(limit, sessions.size()));

            int[] pointsTable = {25, 18, 15, 12, 10, 8, 6, 4, 2, 1};
            java.util.Map<Integer, java.util.Map<String, Object>> driverStats = new java.util.LinkedHashMap<>();

            for (java.util.Map<String, Object> session : recentSessions) {
                int sessionKey = ((Number) session.get("session_key")).intValue();

                    String positionsJson = fetchWithRetry("/position?session_key=" + sessionKey);

                java.util.List<java.util.Map<String, Object>> positions = mapper.readValue(
                        positionsJson, new com.fasterxml.jackson.core.type.TypeReference<>() {});

                java.util.Map<Integer, java.util.Map<String, Object>> latestByDriver = new java.util.LinkedHashMap<>();
                for (java.util.Map<String, Object> pos : positions) {
                    int driverNum = ((Number) pos.get("driver_number")).intValue();
                    latestByDriver.put(driverNum, pos);
                }

                    String driversJson = fetchWithRetry("/drivers?session_key=" + sessionKey);

                java.util.List<java.util.Map<String, Object>> drivers = mapper.readValue(
                        driversJson, new com.fasterxml.jackson.core.type.TypeReference<>() {});

                java.util.Map<Integer, java.util.Map<String, Object>> driverInfo = new java.util.LinkedHashMap<>();
                for (java.util.Map<String, Object> d : drivers) {
                    int num = ((Number) d.get("driver_number")).intValue();
                    driverInfo.put(num, d);
                }

                for (java.util.Map<String, Object> pos : latestByDriver.values()) {
                    int driverNum = ((Number) pos.get("driver_number")).intValue();
                    int position = ((Number) pos.get("position")).intValue();
                    java.util.Map<String, Object> info = driverInfo.getOrDefault(driverNum, java.util.Map.of());

                    int points = (position >= 1 && position <= 10) ? pointsTable[position - 1] : 0;
                    java.util.Map<String, Object> stats = driverStats.computeIfAbsent(driverNum, k -> {
                        java.util.Map<String, Object> s = new java.util.LinkedHashMap<>();
                        s.put("driver_number", k);
                        s.put("driver_name", info.getOrDefault("full_name", "Unknown"));
                        s.put("name_acronym", info.getOrDefault("name_acronym", "???"));
                        s.put("team_name", info.getOrDefault("team_name", "Unknown"));
                        s.put("team_colour", info.getOrDefault("team_colour", "888888"));
                        s.put("totalPoints", 0);
                        s.put("wins", 0);
                        s.put("podiums", 0);
                        return s;
                    });
                    stats.put("totalPoints", ((Number) stats.get("totalPoints")).intValue() + points);
                    if (position == 1) stats.put("wins", ((Number) stats.get("wins")).intValue() + 1);
                    if (position <= 3) stats.put("podiums", ((Number) stats.get("podiums")).intValue() + 1);
                }
            }

            java.util.List<java.util.Map<String, Object>> rankings = new java.util.ArrayList<>(driverStats.values());
            rankings.sort((a, b) -> Integer.compare(
                    ((Number) b.get("totalPoints")).intValue(), ((Number) a.get("totalPoints")).intValue()));

            for (int i = 0; i < rankings.size(); i++) {
                rankings.get(i).put("position", i + 1);
            }

            java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
            result.put("year", year);
            result.put("racesCount", recentSessions.size());
            result.put("rankings", rankings);

            String json = mapper.writeValueAsString(result);
            cache.put(cacheKey, new CacheEntry(json));
            log.info("赛季排名获取完成, year: {}, races: {}", year, recentSessions.size());
            return json;
        } catch (Exception exception) {
            log.error("获取赛季排名失败, year: {}", year, exception);
            throw new com.springboot.backend.common.BusinessException(
                    "获取赛季排名失败，year: " + year, exception);
        }
    }

    /**
     * 获取车队积分榜（按 team_name 聚合）
     *
     * @param year 赛季年份
     * @return 车队排名 JSON 字符串
     */
    public String fetchConstructorRankings(int year) {
        String cacheKey = "constructor_rankings_" + year;
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.debug("缓存命中, cacheKey: {}", cacheKey);
            return cached.data;
        }

        log.info("获取车队积分榜, year: {}", year);
        try {
            // Fetch all main race sessions for the year (session_name=Race to exclude sprints)
            String sessionsJson = fetchWithRetry("/sessions?year=" + year + "&session_type=Race&session_name=Race");

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.List<java.util.Map<String, Object>> sessions = mapper.readValue(
                    sessionsJson, new com.fasterxml.jackson.core.type.TypeReference<>() {});

            int[] pointsTable = {25, 18, 15, 12, 10, 8, 6, 4, 2, 1};
            java.util.Map<String, java.util.Map<String, Object>> teamStats = new java.util.LinkedHashMap<>();

            for (java.util.Map<String, Object> session : sessions) {
                int sessionKey = ((Number) session.get("session_key")).intValue();

                    String positionsJson = fetchWithRetry("/position?session_key=" + sessionKey);

                java.util.List<java.util.Map<String, Object>> positions = mapper.readValue(
                        positionsJson, new com.fasterxml.jackson.core.type.TypeReference<>() {});

                java.util.Map<Integer, java.util.Map<String, Object>> latestByDriver = new java.util.LinkedHashMap<>();
                for (java.util.Map<String, Object> pos : positions) {
                    int driverNum = ((Number) pos.get("driver_number")).intValue();
                    latestByDriver.put(driverNum, pos);
                }

                    String driversJson = fetchWithRetry("/drivers?session_key=" + sessionKey);

                java.util.List<java.util.Map<String, Object>> drivers = mapper.readValue(
                        driversJson, new com.fasterxml.jackson.core.type.TypeReference<>() {});

                java.util.Map<Integer, java.util.Map<String, Object>> driverInfo = new java.util.LinkedHashMap<>();
                for (java.util.Map<String, Object> d : drivers) {
                    int num = ((Number) d.get("driver_number")).intValue();
                    driverInfo.put(num, d);
                }

                for (java.util.Map<String, Object> pos : latestByDriver.values()) {
                    int driverNum = ((Number) pos.get("driver_number")).intValue();
                    int position = ((Number) pos.get("position")).intValue();
                    java.util.Map<String, Object> info = driverInfo.getOrDefault(driverNum, java.util.Map.of());
                    String teamName = (String) info.getOrDefault("team_name", "Unknown");
                    String teamColour = (String) info.getOrDefault("team_colour", "888888");

                    int points = (position >= 1 && position <= 10) ? pointsTable[position - 1] : 0;
                    java.util.Map<String, Object> stats = teamStats.computeIfAbsent(teamName, k -> {
                        java.util.Map<String, Object> s = new java.util.LinkedHashMap<>();
                        s.put("team_name", k);
                        s.put("team_colour", teamColour);
                        s.put("totalPoints", 0);
                        s.put("wins", 0);
                        s.put("podiums", 0);
                        s.put("races", 0);
                        return s;
                    });
                    stats.put("totalPoints", ((Number) stats.get("totalPoints")).intValue() + points);
                    if (position == 1) stats.put("wins", ((Number) stats.get("wins")).intValue() + 1);
                    if (position <= 3) stats.put("podiums", ((Number) stats.get("podiums")).intValue() + 1);
                }
            }

            java.util.List<java.util.Map<String, Object>> rankings = new java.util.ArrayList<>(teamStats.values());
            rankings.sort((a, b) -> Integer.compare(
                    ((Number) b.get("totalPoints")).intValue(), ((Number) a.get("totalPoints")).intValue()));

            for (int i = 0; i < rankings.size(); i++) {
                rankings.get(i).put("position", i + 1);
                rankings.get(i).put("races", sessions.size());
            }

            java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
            result.put("year", year);
            result.put("rankings", rankings);

            String json = mapper.writeValueAsString(result);
            cache.put(cacheKey, new CacheEntry(json));
            log.info("车队积分榜获取完成, year: {}, teams: {}", year, rankings.size());
            return json;
        } catch (Exception exception) {
            log.error("获取车队积分榜失败, year: {}", year, exception);
            throw new com.springboot.backend.common.BusinessException(
                    "获取车队积分榜失败，year: " + year, exception);
        }
    }

    /**
     * 获取赛季完整积分榜（车手 + 车队，单次数据获取计算两个排行榜）
     *
     * @param year 赛季年份
     * @return 包含 driverRankings 和 constructorRankings 的 JSON 字符串
     */
    public String fetchSeasonStandings(int year) {
        String cacheKey = "season_standings_" + year;
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.debug("缓存命中, cacheKey: {}", cacheKey);
            return cached.data;
        }

        log.info("获取赛季完整积分榜, year: {}", year);
        try {
            String sessionsJson = fetchWithRetry("/sessions?year=" + year + "&session_type=Race&session_name=Race");

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.List<java.util.Map<String, Object>> sessions = mapper.readValue(
                    sessionsJson, new com.fasterxml.jackson.core.type.TypeReference<>() {});

            sessions = filterPastSessions(sessions);

            sessions.sort((a, b) -> {
                String dateA = (String) a.getOrDefault("date_start", "");
                String dateB = (String) b.getOrDefault("date_start", "");
                return dateB.compareTo(dateA);
            });

            int[] pointsTable = {25, 18, 15, 12, 10, 8, 6, 4, 2, 1};
            java.util.Map<Integer, java.util.Map<String, Object>> driverStats = new java.util.LinkedHashMap<>();
            java.util.Map<String, java.util.Map<String, Object>> teamStats = new java.util.LinkedHashMap<>();

            for (java.util.Map<String, Object> session : sessions) {
                int sessionKey = ((Number) session.get("session_key")).intValue();

                    String positionsJson = fetchWithRetry("/position?session_key=" + sessionKey);

                java.util.List<java.util.Map<String, Object>> positions = mapper.readValue(
                        positionsJson, new com.fasterxml.jackson.core.type.TypeReference<>() {});

                java.util.Map<Integer, java.util.Map<String, Object>> latestByDriver = new java.util.LinkedHashMap<>();
                for (java.util.Map<String, Object> pos : positions) {
                    int driverNum = ((Number) pos.get("driver_number")).intValue();
                    latestByDriver.put(driverNum, pos);
                }

                    String driversJson = fetchWithRetry("/drivers?session_key=" + sessionKey);

                java.util.List<java.util.Map<String, Object>> drivers = mapper.readValue(
                        driversJson, new com.fasterxml.jackson.core.type.TypeReference<>() {});

                java.util.Map<Integer, java.util.Map<String, Object>> driverInfo = new java.util.LinkedHashMap<>();
                for (java.util.Map<String, Object> d : drivers) {
                    int num = ((Number) d.get("driver_number")).intValue();
                    driverInfo.put(num, d);
                }

                for (java.util.Map<String, Object> pos : latestByDriver.values()) {
                    int driverNum = ((Number) pos.get("driver_number")).intValue();
                    int position = ((Number) pos.get("position")).intValue();
                    java.util.Map<String, Object> info = driverInfo.getOrDefault(driverNum, java.util.Map.of());
                    String teamName = (String) info.getOrDefault("team_name", "Unknown");
                    String teamColour = (String) info.getOrDefault("team_colour", "888888");

                    int points = (position >= 1 && position <= 10) ? pointsTable[position - 1] : 0;

                    // Driver stats
                    java.util.Map<String, Object> dStats = driverStats.computeIfAbsent(driverNum, k -> {
                        java.util.Map<String, Object> s = new java.util.LinkedHashMap<>();
                        s.put("driver_number", k);
                        s.put("driver_name", info.getOrDefault("full_name", "Unknown"));
                        s.put("name_acronym", info.getOrDefault("name_acronym", "???"));
                        s.put("team_name", teamName);
                        s.put("team_colour", teamColour);
                        s.put("totalPoints", 0);
                        s.put("wins", 0);
                        s.put("podiums", 0);
                        return s;
                    });
                    dStats.put("totalPoints", ((Number) dStats.get("totalPoints")).intValue() + points);
                    if (position == 1) dStats.put("wins", ((Number) dStats.get("wins")).intValue() + 1);
                    if (position <= 3) dStats.put("podiums", ((Number) dStats.get("podiums")).intValue() + 1);

                    // Constructor stats
                    java.util.Map<String, Object> tStats = teamStats.computeIfAbsent(teamName, k -> {
                        java.util.Map<String, Object> s = new java.util.LinkedHashMap<>();
                        s.put("team_name", k);
                        s.put("team_colour", teamColour);
                        s.put("totalPoints", 0);
                        s.put("wins", 0);
                        s.put("podiums", 0);
                        s.put("races", 0);
                        return s;
                    });
                    tStats.put("totalPoints", ((Number) tStats.get("totalPoints")).intValue() + points);
                    if (position == 1) tStats.put("wins", ((Number) tStats.get("wins")).intValue() + 1);
                    if (position <= 3) tStats.put("podiums", ((Number) tStats.get("podiums")).intValue() + 1);
                }
            }

            // Build driver rankings
            java.util.List<java.util.Map<String, Object>> driverRankings = new java.util.ArrayList<>(driverStats.values());
            driverRankings.sort((a, b) -> Integer.compare(
                    ((Number) b.get("totalPoints")).intValue(), ((Number) a.get("totalPoints")).intValue()));
            for (int i = 0; i < driverRankings.size(); i++) {
                driverRankings.get(i).put("position", i + 1);
            }

            // Build constructor rankings
            java.util.List<java.util.Map<String, Object>> constructorRankings = new java.util.ArrayList<>(teamStats.values());
            constructorRankings.sort((a, b) -> Integer.compare(
                    ((Number) b.get("totalPoints")).intValue(), ((Number) a.get("totalPoints")).intValue()));
            for (int i = 0; i < constructorRankings.size(); i++) {
                constructorRankings.get(i).put("position", i + 1);
                constructorRankings.get(i).put("races", sessions.size());
            }

            java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
            result.put("year", year);
            result.put("racesCount", sessions.size());
            result.put("driverRankings", driverRankings);
            result.put("constructorRankings", constructorRankings);

            String json = mapper.writeValueAsString(result);
            cache.put(cacheKey, new CacheEntry(json));
            log.info("赛季完整积分榜获取完成, year: {}, drivers: {}, constructors: {}",
                    year, driverRankings.size(), constructorRankings.size());
            return json;
        } catch (Exception exception) {
            log.error("获取赛季完整积分榜失败, year: {}", year, exception);
            throw new com.springboot.backend.common.BusinessException(
                    "获取赛季完整积分榜失败，year: " + year, exception);
        }
    }

    /**
     * 获取车手详情（基本信息 + 赛季统计 + 比赛成绩）
     *
     * @param driverNumber 车手号码
     * @param year 赛季年份
     * @return 车手详情 JSON 字符串
     */
    public String fetchDriverDetail(int driverNumber, int year) {
        String cacheKey = "driver_detail_" + driverNumber + "_" + year;
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.debug("缓存命中, cacheKey: {}", cacheKey);
            return cached.data;
        }

        log.info("获取车手详情, driverNumber: {}, year: {}", driverNumber, year);
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

            // 1. Fetch driver info from latest session
            String latestSessionJson = fetchWithRetry("/sessions?session_key=latest");
            java.util.List<java.util.Map<String, Object>> latestSessions = mapper.readValue(
                    latestSessionJson, new com.fasterxml.jackson.core.type.TypeReference<>() {});
            if (latestSessions.isEmpty()) {
                throw new com.springboot.backend.common.BusinessException("未找到最新会话");
            }
            int latestSessionKey = ((Number) latestSessions.get(0).get("session_key")).intValue();

            String driverJson = fetchWithRetry("/drivers?driver_number=" + driverNumber + "&session_key=" + latestSessionKey);
            java.util.List<java.util.Map<String, Object>> driverList = mapper.readValue(
                    driverJson, new com.fasterxml.jackson.core.type.TypeReference<>() {});
            if (driverList.isEmpty()) {
                throw new com.springboot.backend.common.BusinessException("未找到车手, driverNumber: " + driverNumber);
            }
            java.util.Map<String, Object> driverInfo = driverList.get(0);

            // 2. Fetch all main race sessions for the year (session_name=Race to exclude sprints)
            String sessionsJson = fetchWithRetry("/sessions?year=" + year + "&session_type=Race&session_name=Race");
            java.util.List<java.util.Map<String, Object>> sessions = mapper.readValue(
                    sessionsJson, new com.fasterxml.jackson.core.type.TypeReference<>() {});

            sessions = filterPastSessions(sessions);

            // 3. For each session, get all positions to compute stats and race results
            int[] pointsTable = {25, 18, 15, 12, 10, 8, 6, 4, 2, 1};
            java.util.Map<Integer, java.util.Map<String, Object>> allDriverStats = new java.util.LinkedHashMap<>();
            java.util.List<java.util.Map<String, Object>> raceResults = new java.util.ArrayList<>();

            for (java.util.Map<String, Object> session : sessions) {
                int sessionKey = ((Number) session.get("session_key")).intValue();
                String meetingName = (String) session.getOrDefault("meeting_name", "Unknown");
                String circuitName = (String) session.getOrDefault("circuit_short_name", "");

                    String positionsJson = fetchWithRetry("/position?session_key=" + sessionKey);

                java.util.List<java.util.Map<String, Object>> positions = mapper.readValue(
                        positionsJson, new com.fasterxml.jackson.core.type.TypeReference<>() {});

                java.util.Map<Integer, java.util.Map<String, Object>> latestByDriver = new java.util.LinkedHashMap<>();
                for (java.util.Map<String, Object> pos : positions) {
                    int dNum = ((Number) pos.get("driver_number")).intValue();
                    latestByDriver.put(dNum, pos);
                }

                // Get driver info for this session
                    String sessionDriversJson = fetchWithRetry("/drivers?session_key=" + sessionKey);
                java.util.List<java.util.Map<String, Object>> sessionDrivers = mapper.readValue(
                        sessionDriversJson, new com.fasterxml.jackson.core.type.TypeReference<>() {});
                java.util.Map<Integer, java.util.Map<String, Object>> sessionDriverInfo = new java.util.LinkedHashMap<>();
                for (java.util.Map<String, Object> d : sessionDrivers) {
                    int num = ((Number) d.get("driver_number")).intValue();
                    sessionDriverInfo.put(num, d);
                }

                // Aggregate all drivers' points for rank computation
                for (java.util.Map<String, Object> pos : latestByDriver.values()) {
                    int dNum = ((Number) pos.get("driver_number")).intValue();
                    int position = ((Number) pos.get("position")).intValue();

                    int pts = (position >= 1 && position <= 10) ? pointsTable[position - 1] : 0;
                    java.util.Map<String, Object> stats = allDriverStats.computeIfAbsent(dNum, k -> {
                        java.util.Map<String, Object> s = new java.util.LinkedHashMap<>();
                        s.put("driver_number", k);
                        s.put("totalPoints", 0);
                        s.put("wins", 0);
                        s.put("podiums", 0);
                        s.put("races", 0);
                        return s;
                    });
                    stats.put("totalPoints", ((Number) stats.get("totalPoints")).intValue() + pts);
                    if (position == 1) stats.put("wins", ((Number) stats.get("wins")).intValue() + 1);
                    if (position <= 3) stats.put("podiums", ((Number) stats.get("podiums")).intValue() + 1);
                    stats.put("races", ((Number) stats.get("races")).intValue() + 1);

                    // Track target driver's race result
                    if (dNum == driverNumber) {
                        java.util.Map<String, Object> raceEntry = new java.util.LinkedHashMap<>();
                        raceEntry.put("meeting_name", meetingName);
                        raceEntry.put("circuit_short_name", circuitName);
                        raceEntry.put("position", position);
                        raceEntry.put("session_key", sessionKey);
                        raceResults.add(raceEntry);
                    }
                }
            }

            // 4. Compute rank for target driver
            java.util.List<java.util.Map<String, Object>> allStats = new java.util.ArrayList<>(allDriverStats.values());
            allStats.sort((a, b) -> Integer.compare(
                    ((Number) b.get("totalPoints")).intValue(), ((Number) a.get("totalPoints")).intValue()));

            int rank = 0;
            for (int i = 0; i < allStats.size(); i++) {
                if (((Number) allStats.get(i).get("driver_number")).intValue() == driverNumber) {
                    rank = i + 1;
                    break;
                }
            }

            java.util.Map<String, Object> targetStats = allDriverStats.get(driverNumber);
            if (targetStats == null) {
                targetStats = new java.util.LinkedHashMap<>();
                targetStats.put("totalPoints", 0);
                targetStats.put("wins", 0);
                targetStats.put("podiums", 0);
                targetStats.put("races", 0);
            }

            // 5. Build response
            java.util.Map<String, Object> seasonStats = new java.util.LinkedHashMap<>();
            seasonStats.put("totalPoints", ((Number) targetStats.get("totalPoints")).intValue());
            seasonStats.put("wins", ((Number) targetStats.get("wins")).intValue());
            seasonStats.put("podiums", ((Number) targetStats.get("podiums")).intValue());
            seasonStats.put("races", ((Number) targetStats.get("races")).intValue());
            seasonStats.put("rank", rank);

            java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
            result.put("driver_number", driverNumber);
            result.put("full_name", driverInfo.getOrDefault("full_name", "Unknown"));
            result.put("name_acronym", driverInfo.getOrDefault("name_acronym", "???"));
            result.put("team_name", driverInfo.getOrDefault("team_name", "Unknown"));
            result.put("team_colour", driverInfo.getOrDefault("team_colour", "888888"));
            result.put("headshot_url", driverInfo.getOrDefault("headshot_url", ""));
            result.put("country_code", driverInfo.get("country_code"));
            result.put("seasonStats", seasonStats);
            result.put("raceResults", raceResults);

            String json = mapper.writeValueAsString(result);
            cache.put(cacheKey, new CacheEntry(json));
            log.info("车手详情获取完成, driverNumber: {}, year: {}, rank: {}", driverNumber, year, rank);
            return json;
        } catch (Exception exception) {
            log.error("获取车手详情失败, driverNumber: {}, year: {}", driverNumber, year, exception);
            throw new com.springboot.backend.common.BusinessException(
                    "获取车手详情失败，driverNumber: " + driverNumber + ", year: " + year, exception);
        }
    }

    /**
     * 带全局限流、重试和 429 处理的 API 调用
     */
    private String fetchWithRetry(String path) {
        int maxRetries = 5;
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                // 429 重试时指数退避：2s, 4s, 8s, 16s
                if (attempt > 0) {
                    long backoff = 2000L * (1L << (attempt - 1));
                    log.warn("429 重试等待 {}ms, 第{}/{}次, path: {}", backoff, attempt + 1, maxRetries, path);
                    try { Thread.sleep(backoff); } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new com.springboot.backend.common.BusinessException("请求被中断", ie);
                    }
                }

                // 全局限流：确保与上次 API 调用间隔至少 API_DELAY_MS
                enforceRateLimit();

                apiSemaphore.acquire();
                try {
                    String result = webClient.get()
                            .uri(path)
                            .retrieve()
                            .bodyToMono(String.class)
                            .block();
                    lastApiCallTime = System.currentTimeMillis();
                    return result;
                } finally {
                    apiSemaphore.release();
                }
            } catch (org.springframework.web.reactive.function.client.WebClientResponseException.TooManyRequests e) {
                log.warn("API 限流 (429), 第{}次重试, path: {}", attempt + 1, path);
                if (attempt == maxRetries - 1) throw e;
            } catch (org.springframework.web.reactive.function.client.WebClientResponseException.NotFound e) {
                log.warn("API 资源不存在 (404), path: {}", path);
                return "[]";
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new com.springboot.backend.common.BusinessException("请求被中断", e);
            } catch (com.springboot.backend.common.BusinessException e) {
                throw e;
            }
        }
        throw new com.springboot.backend.common.BusinessException("API 调用失败，已重试" + maxRetries + "次");
    }

    /**
     * 全局限流：确保两次 API 调用之间至少间隔 API_DELAY_MS
     */
    private void enforceRateLimit() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastApiCallTime;
        if (elapsed < API_DELAY_MS) {
            try { Thread.sleep(API_DELAY_MS - elapsed); } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private String getFromCacheOrFetch(String cacheKey, String path) {
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.debug("缓存命中, cacheKey: {}", cacheKey);
            return cached.data;
        }

        log.info("调用 OpenF1 API, path: {}", path);
        try {
            String response = webClient.get()
                    .uri(path)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            cache.put(cacheKey, new CacheEntry(response));
            log.info("OpenF1 API 调用成功, path: {}", path);
            return response;
        } catch (Exception exception) {
            log.error("OpenF1 API 调用失败, path: {}", path, exception);
            throw new com.springboot.backend.common.BusinessException(
                    "获取 F1 数据失败，path: " + path, exception);
        }
    }

    /**
     * 过滤掉尚未开始的比赛会话（只保留已结束的）
     */
    private java.util.List<java.util.Map<String, Object>> filterPastSessions(
            java.util.List<java.util.Map<String, Object>> sessions) {
        java.time.Instant now = java.time.Instant.now();
        java.util.List<java.util.Map<String, Object>> past = new java.util.ArrayList<>();
        for (java.util.Map<String, Object> s : sessions) {
            String dateStr = (String) s.getOrDefault("date_start", "");
            if (dateStr.isEmpty()) {
                past.add(s);
                continue;
            }
            try {
                java.time.Instant start = java.time.Instant.parse(dateStr);
                if (start.isBefore(now)) {
                    past.add(s);
                }
            } catch (Exception e) {
                past.add(s);
            }
        }
        return past;
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
