package com.springboot.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.backend.repository.DataSnapshotRepository;
import com.springboot.backend.sync.F1DataSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 基于本地数据快照的 F1 查询服务：
 * 读路径只查本地库，缺失时触发一次内联同步，不再实时爬取上游 API。
 *
 * @author F1-messageTool
 * @since 2026-08-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class F1QueryService {

    private final DataSnapshotRepository snapshotRepository;
    private final F1DataSyncService syncService;
    private final OpenF1Service openF1Service;
    private final ObjectMapper objectMapper;

    /** 一轮比赛：名称 + 车手号→该轮后的累计积分 */
    private record Round(String name, Map<Integer, Integer> cumulativePoints) {}

    public String getMeetings(int year) {
        ensureYear(year);
        return getByKey(F1DataSyncService.keyMeetings(year));
    }

    public String getLatestSession() throws Exception {
        List<Map<String, Object>> sessions = readSessions(F1DataSyncService.CURRENT_YEAR);
        // 只在已完赛场次中取最新（未来场次无名次数据）
        Map<String, Object> latest = null;
        String now = java.time.LocalDate.now().toString();
        for (Map<String, Object> session : sessions) {
            String dateStart = (String) session.getOrDefault("date_start", "");
            if (latest == null || compareDate(session, latest) > 0) {
                if (dateStart.substring(0, 10).compareTo(now) <= 0 || latest == null) {
                    latest = session;
                }
            }
        }
        return objectMapper.writeValueAsString(enrichWithMeeting(latest));
    }

    /** 场次条目缺 meeting_name，从赛历快照按 meeting_key 补齐 */
    private Map<String, Object> enrichWithMeeting(Map<String, Object> session) throws Exception {
        if (session.get("meeting_key") == null) {
            return session;
        }
        int meetingKey = ((Number) session.get("meeting_key")).intValue();
        List<Map<String, Object>> meetings = objectMapper.readValue(
                getByKey(F1DataSyncService.keyMeetings(F1DataSyncService.CURRENT_YEAR)),
                new com.fasterxml.jackson.core.type.TypeReference<>() {});
        for (Map<String, Object> meeting : meetings) {
            if (meeting.get("meeting_key") != null
                    && ((Number) meeting.get("meeting_key")).intValue() == meetingKey) {
                session.put("meeting_name", meeting.get("meeting_name"));
                session.put("country_name", session.getOrDefault("country_name", meeting.get("country_name")));
                break;
            }
        }
        return session;
    }

    /**
     * 名次数据：优先本地快照，未同步的场次回退实时代理并尽力补存
     */
    public String getPositions(int sessionKey) throws Exception {
        Optional<com.springboot.backend.entity.DataSnapshot> snapshot =
                snapshotRepository.findByCacheKey(F1DataSyncService.keyPositions(sessionKey));
        if (snapshot.isPresent()) {
            return snapshot.get().getJsonContent();
        }
        try {
            String json = openF1Service.fetchPositions(sessionKey);
            try {
                snapshotRepository.save(newSnapshot(F1DataSyncService.keyPositions(sessionKey), json));
            } catch (Exception exception) {
                log.warn("名次快照补存失败, sessionKey: {}", sessionKey);
            }
            return json;
        } catch (Exception exception) {
            // 未来场次/上游异常时返回空数组，前端优雅降级
            log.warn("名次数据获取失败, 返回空数组, sessionKey: {}", sessionKey);
            return "[]";
        }
    }

    /**
     * 赛季排名（逐轮聚合），算法与原实时版本一致，数据源改为本地快照
     */
    public String getSeasonRankings(int year, int limit) throws Exception {
        Aggregation aggregation = aggregateRounds(year);
        List<Map<String, Object>> rankings = new ArrayList<>(aggregation.stats().values());
        rankings.sort((a, b) -> Integer.compare(
                ((Number) b.get("totalPoints")).intValue(),
                ((Number) a.get("totalPoints")).intValue()));
        for (int i = 0; i < rankings.size(); i++) {
            rankings.get(i).put("position", i + 1);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("year", year);
        result.put("racesCount", Math.min(limit, aggregation.roundCount()));
        result.put("rankings", rankings.subList(0, Math.min(limit, rankings.size())));
        return objectMapper.writeValueAsString(result);
    }

    /**
     * 车手列表（本地快照版）：积分榜快照定顺序/国籍，车手名单快照补头像/涂装
     */
    public String getDriversList(int year) throws Exception {
        ensureYear(year);
        Map<Integer, Map<String, Object>> meta = readDriverMeta(year);

        List<Map<String, Object>> result = new ArrayList<>();
        try {
            com.fasterxml.jackson.databind.JsonNode lists = objectMapper
                    .readTree(getByKey(F1DataSyncService.keyStandings(year)))
                    .path("MRData").path("StandingsTable").path("StandingsLists");
            if (lists.isArray() && lists.size() > 0) {
                for (com.fasterxml.jackson.databind.JsonNode entry
                        : lists.get(0).path("DriverStandings")) {
                    int number = entry.path("Driver").path("permanentNumber").asInt(0);
                    Map<String, Object> source = meta.getOrDefault(number, Map.of());
                    Map<String, Object> driverInfo = new LinkedHashMap<>();
                    driverInfo.put("driver_number", number);
                    driverInfo.put("full_name", source.getOrDefault("full_name",
                            entry.path("Driver").path("givenName").asText() + " "
                                    + entry.path("Driver").path("familyName").asText()));
                    driverInfo.put("name_acronym", source.getOrDefault("name_acronym",
                            entry.path("Driver").path("code").asText("???")));
                    driverInfo.put("team_name", source.getOrDefault("team_name",
                            entry.path("Constructors").path(0).path("name").asText("Unknown")));
                    driverInfo.put("team_colour", source.getOrDefault("team_colour", "888888"));
                    driverInfo.put("headshot_url", source.getOrDefault("headshot_url", ""));
                    driverInfo.put("nationality", entry.path("Driver").path("nationality").asText(""));
                    result.add(driverInfo);
                }
            }
        } catch (Exception exception) {
            log.warn("积分榜快照解析失败, 降级为车手名单: {}", exception.getMessage());
        }

        // 积分榜缺失时兜底：直接用车手名单快照
        if (result.isEmpty()) {
            for (Map.Entry<Integer, Map<String, Object>> e : meta.entrySet()) {
                Map<String, Object> driverInfo = new LinkedHashMap<>(e.getValue());
                driverInfo.putIfAbsent("driver_number", e.getKey());
                driverInfo.putIfAbsent("nationality", "");
                result.add(driverInfo);
            }
        }
        return objectMapper.writeValueAsString(result);
    }

    /**
     * 赛季双榜单（本地快照版）：车手/车队积分榜均由 f1:standings 快照转换，无上游调用
     */
    public String getSeasonStandings(int year) throws Exception {
        ensureYear(year);
        com.fasterxml.jackson.databind.JsonNode driverLists = objectMapper
                .readTree(getByKey(F1DataSyncService.keyStandings(year)))
                .path("MRData").path("StandingsTable").path("StandingsLists");
        com.fasterxml.jackson.databind.JsonNode constructorLists = objectMapper
                .readTree(getByKey(F1DataSyncService.keyConstructorStandings(year)))
                .path("MRData").path("StandingsTable").path("StandingsLists");

        int racesCount = 0;
        List<Map<String, Object>> driverRankings = new ArrayList<>();
        List<Map<String, Object>> constructorRankings = new ArrayList<>();

        if (driverLists.isArray() && driverLists.size() > 0) {
            com.fasterxml.jackson.databind.JsonNode firstList = driverLists.get(0);
            racesCount = firstList.path("round").asInt(0);

            for (com.fasterxml.jackson.databind.JsonNode entry : firstList.path("DriverStandings")) {
                Map<String, Object> ranking = new LinkedHashMap<>();
                ranking.put("position", entry.path("position").asInt(0));
                ranking.put("driver_number", entry.path("Driver").path("permanentNumber").asInt(0));
                ranking.put("driver_name", entry.path("Driver").path("givenName").asText() + " "
                        + entry.path("Driver").path("familyName").asText());
                ranking.put("name_acronym", entry.path("Driver").path("code").asText("???"));
                ranking.put("team_name", entry.path("Constructors").path(0).path("name").asText("Unknown"));
                ranking.put("totalPoints", entry.path("points").asInt(0));
                ranking.put("wins", entry.path("wins").asInt(0));
                ranking.put("podiums", 0); // Jolpica 不提供领奖台数据
                driverRankings.add(ranking);
            }
        }

        if (constructorLists.isArray() && constructorLists.size() > 0) {
            for (com.fasterxml.jackson.databind.JsonNode entry
                    : constructorLists.get(0).path("ConstructorStandings")) {
                Map<String, Object> ranking = new LinkedHashMap<>();
                ranking.put("position", entry.path("position").asInt(0));
                ranking.put("team_name", entry.path("Constructor").path("name").asText("Unknown"));
                ranking.put("totalPoints", entry.path("points").asInt(0));
                ranking.put("wins", entry.path("wins").asInt(0));
                ranking.put("podiums", 0);
                ranking.put("races", racesCount);
                constructorRankings.add(ranking);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("year", year);
        result.put("racesCount", racesCount);
        result.put("driverRankings", driverRankings);
        result.put("constructorRankings", constructorRankings);
        return objectMapper.writeValueAsString(result);
    }

    /**
     * 车手详情（本地快照版）：
     * 元数据取车手名单/积分榜快照，逐场结果取名次快照，全程不调上游 API
     */
    public String getDriverDetail(int driverNumber, int year) throws Exception {
        ensureYear(year);

        Map<String, Object> meta =
                readDriverMeta(year).getOrDefault(driverNumber, new LinkedHashMap<>());

        // 赛历快照：meeting_key → 名称/赛道信息
        Map<Integer, Map<String, Object>> meetingsByKey = new LinkedHashMap<>();
        for (Map<String, Object> meeting : objectMapper.readValue(
                getByKey(F1DataSyncService.keyMeetings(year)),
                new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {})) {
            if (meeting.get("meeting_key") != null) {
                meetingsByKey.put(((Number) meeting.get("meeting_key")).intValue(), meeting);
            }
        }

        // 逐场正赛结果（按时间升序遍历，未同步的场次视为未完赛跳过）
        int[] pointsTable = {25, 18, 15, 12, 10, 8, 6, 4, 2, 1};
        List<Map<String, Object>> raceResults = new ArrayList<>();
        int completedRounds = 0;
        int round = 0;
        for (Map<String, Object> session : readSessionsSortedAsc(year)) {
            round++;
            int sessionKey = ((Number) session.get("session_key")).intValue();
            Optional<com.springboot.backend.entity.DataSnapshot> positionsSnap =
                    snapshotRepository.findByCacheKey(F1DataSyncService.keyPositions(sessionKey));
            if (positionsSnap.isEmpty()) {
                continue;
            }
            completedRounds++;
            Integer position = parseLatestPositions(positionsSnap.get().getJsonContent())
                    .get(driverNumber);
            if (position == null) {
                continue;
            }
            Map<String, Object> meeting =
                    meetingsByKey.getOrDefault(
                            session.get("meeting_key") == null ? -1
                                    : ((Number) session.get("meeting_key")).intValue(),
                            Map.of());
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("meeting_name", meeting.getOrDefault("meeting_name", "Unknown"));
            entry.put("circuit_short_name", meeting.getOrDefault("circuit_short_name", ""));
            entry.put("position", position);
            entry.put("round", round);
            entry.put("session_key", sessionKey);
            raceResults.add(entry);
        }

        // 官方积分数据优先取积分榜快照；缺失时从逐场结果估算
        int totalPoints = 0;
        int wins = 0;
        int rank = 0;
        String nationality = null;
        boolean driverInStandings = false;
        try {
            Optional<com.springboot.backend.entity.DataSnapshot> standings =
                    snapshotRepository.findByCacheKey(F1DataSyncService.keyStandings(year));
            if (standings.isPresent()) {
                com.fasterxml.jackson.databind.JsonNode lists = objectMapper
                        .readTree(standings.get().getJsonContent())
                        .path("MRData").path("StandingsTable").path("StandingsLists");
                if (lists.isArray() && lists.size() > 0) {
                    for (com.fasterxml.jackson.databind.JsonNode entry
                            : lists.get(0).path("DriverStandings")) {
                        if (entry.path("Driver").path("permanentNumber").asInt(0) == driverNumber) {
                            totalPoints = entry.path("points").asInt(0);
                            wins = entry.path("wins").asInt(0);
                            rank = entry.path("position").asInt(0);
                            nationality = entry.path("Driver").path("nationality").asText(null);
                            driverInStandings = true;
                            break;
                        }
                    }
                }
            }
        } catch (Exception exception) {
            log.warn("积分榜快照解析失败, 降级为逐场估算: {}", exception.getMessage());
        }

        boolean knownDriver = driverInStandings || !meta.isEmpty() || !raceResults.isEmpty();
        if (!knownDriver) {
            throw new com.springboot.backend.common.BusinessException(
                    "未找到车手, driverNumber: " + driverNumber);
        }

        int podiums = 0;
        if (!driverInStandings) {
            for (Map<String, Object> r : raceResults) {
                int position = ((Number) r.get("position")).intValue();
                if (position >= 1 && position <= 10) {
                    totalPoints += pointsTable[position - 1];
                }
                if (position == 1) wins++;
            }
        }
        for (Map<String, Object> r : raceResults) {
            if (((Number) r.get("position")).intValue() <= 3) {
                podiums++;
            }
        }

        Map<String, Object> seasonStats = new LinkedHashMap<>();
        seasonStats.put("totalPoints", totalPoints);
        seasonStats.put("wins", wins);
        seasonStats.put("podiums", podiums);
        seasonStats.put("races", completedRounds);
        seasonStats.put("rank", rank);

        java.util.Collections.reverse(raceResults);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("driver_number", driverNumber);
        result.put("full_name", meta.getOrDefault("full_name", "Unknown"));
        result.put("name_acronym", meta.getOrDefault("name_acronym", "???"));
        result.put("team_name", meta.getOrDefault("team_name", "Unknown"));
        result.put("team_colour", meta.getOrDefault("team_colour", "888888"));
        result.put("headshot_url", meta.getOrDefault("headshot_url", ""));
        result.put("country_code", meta.get("country_code"));
        result.put("nationality", nationality);
        result.put("seasonStats", seasonStats);
        result.put("raceResults", raceResults);
        return objectMapper.writeValueAsString(result);
    }

    /**
     * 逐轮累计积分趋势（全量车手，按最终积分降序）
     */
    public String getAllDriversPointsTrend(int year) throws Exception {
        Aggregation aggregation = aggregateRounds(year);

        List<Integer> ordered = aggregation.stats().values().stream()
                .sorted((a, b) -> Integer.compare(
                        ((Number) b.get("totalPoints")).intValue(),
                        ((Number) a.get("totalPoints")).intValue()))
                .map(s -> ((Number) s.get("driver_number")).intValue())
                .toList();

        List<Map<String, Object>> driverSeries = new ArrayList<>();
        for (Integer driverNumber : ordered) {
            List<Integer> points = new ArrayList<>(aggregation.roundCount());
            int carried = 0;
            for (Round round : aggregation.rounds()) {
                carried = round.cumulativePoints().getOrDefault(driverNumber, carried);
                points.add(carried);
            }
            Map<String, Object> meta = aggregation.meta().getOrDefault(driverNumber, Map.of());
            Map<String, Object> series = new LinkedHashMap<>();
            series.put("driver_number", driverNumber);
            series.put("driver_name", meta.getOrDefault("full_name", "Driver " + driverNumber));
            series.put("name_acronym", meta.getOrDefault("name_acronym", "???"));
            series.put("team_name", meta.getOrDefault("team_name", "Unknown"));
            series.put("team_colour", meta.getOrDefault("team_colour", "888888"));
            series.put("points", points);
            driverSeries.add(series);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("year", year);
        result.put("estimated", true);
        result.put("rounds", aggregation.rounds().stream().map(Round::name).toList());
        result.put("drivers", driverSeries);
        return objectMapper.writeValueAsString(result);
    }

    // ------------------------------------------------------------------
    // 内部：轮次聚合
    // ------------------------------------------------------------------

    private record Aggregation(
            List<Round> rounds,
            Map<Integer, Map<String, Object>> stats,
            Map<Integer, Map<String, Object>> meta,
            int roundCount) {}

    private Aggregation aggregateRounds(int year) throws Exception {
        ensureYear(year);
        List<Map<String, Object>> sessions = readSessionsSortedAsc(year);
        Map<Integer, Map<String, Object>> driverMeta = readDriverMeta(year);

        int[] pointsTable = {25, 18, 15, 12, 10, 8, 6, 4, 2, 1};
        Map<Integer, Integer> totals = new LinkedHashMap<>();
        Map<Integer, Map<String, Object>> stats = new LinkedHashMap<>();
        List<Round> rounds = new ArrayList<>();

        for (Map<String, Object> session : sessions) {
            int sessionKey = ((Number) session.get("session_key")).intValue();
            Optional<com.springboot.backend.entity.DataSnapshot> positionsSnap =
                    snapshotRepository.findByCacheKey(F1DataSyncService.keyPositions(sessionKey));
            if (positionsSnap.isEmpty()) {
                continue; // 未同步/未完赛的场次不参与聚合
            }
            Map<Integer, Integer> roundPositions =
                    parseLatestPositions(positionsSnap.get().getJsonContent());
            for (Map.Entry<Integer, Integer> entry : roundPositions.entrySet()) {
                int position = entry.getValue();
                int pts = (position >= 1 && position <= 10) ? pointsTable[position - 1] : 0;
                totals.merge(entry.getKey(), pts, Integer::sum);
                Map<String, Object> s = stats.computeIfAbsent(entry.getKey(), k -> {
                    Map<String, Object> created = new LinkedHashMap<>();
                    created.put("driver_number", k);
                    return created;
                });
                s.putAll(metaWithFallback(driverMeta, entry.getKey()));
                s.put("totalPoints", totals.get(entry.getKey()));
                s.put("wins", ((Number) s.getOrDefault("wins", 0)).intValue() + (position == 1 ? 1 : 0));
                s.put("podiums", ((Number) s.getOrDefault("podiums", 0)).intValue() + (position <= 3 ? 1 : 0));
            }
            // 快照该轮结束后的累计积分（趋势图数据源）
            rounds.add(new Round(
                    (String) session.getOrDefault("meeting_name", "Unknown"),
                    new LinkedHashMap<>(totals)));
        }
        return new Aggregation(rounds, stats, driverMeta, rounds.size());
    }

    private Map<String, Object> metaWithFallback(Map<Integer, Map<String, Object>> driverMeta, int driverNumber) {
        Map<String, Object> meta = new LinkedHashMap<>();
        Map<String, Object> source = driverMeta.get(driverNumber);
        meta.put("driver_name", source != null ? source.getOrDefault("full_name", "Unknown") : "Unknown");
        meta.put("name_acronym", source != null ? source.getOrDefault("name_acronym", "???") : "???");
        meta.put("team_name", source != null ? source.getOrDefault("team_name", "Unknown") : "Unknown");
        meta.put("team_colour", source != null ? source.getOrDefault("team_colour", "888888") : "888888");
        return meta;
    }

    /** 解析名次快照：同一车手取最新一条记录 */
    private Map<Integer, Integer> parseLatestPositions(String json) throws Exception {
        List<Map<String, Object>> positions = objectMapper.readValue(
                json, new com.fasterxml.jackson.core.type.TypeReference<>() {});
        Map<Integer, Integer> latestByDriver = new LinkedHashMap<>();
        String latestDate = "";
        for (Map<String, Object> pos : positions) {
            int driverNumber = ((Number) pos.get("driver_number")).intValue();
            String date = (String) pos.getOrDefault("date", "");
            if (date.compareTo(latestDate) >= 0) {
                latestByDriver.put(driverNumber, ((Number) pos.get("position")).intValue());
                latestDate = date;
            }
        }
        return latestByDriver;
    }

    private List<Map<String, Object>> readSessions(int year) throws Exception {
        ensureYear(year);
        return objectMapper.readValue(
                getByKey(F1DataSyncService.keySessions(year)),
                new com.fasterxml.jackson.core.type.TypeReference<>() {});
    }

    private List<Map<String, Object>> readSessionsSortedAsc(int year) throws Exception {
        List<Map<String, Object>> sessions = readSessions(year);
        sessions.sort((a, b) -> compareDate(a, b));
        return sessions;
    }

    /** 车手元数据：最新场次名单为基础，缺席车手从 Jolpica 积分榜补齐 */
    private Map<Integer, Map<String, Object>> readDriverMeta(int year) throws Exception {
        Map<Integer, Map<String, Object>> meta = new LinkedHashMap<>();
        Optional<com.springboot.backend.entity.DataSnapshot> snapshot =
                snapshotRepository.findByCacheKey("f1:drivers:" + year);
        if (snapshot.isPresent()) {
            List<Map<String, Object>> drivers = objectMapper.readValue(
                    snapshot.get().getJsonContent(),
                    new com.fasterxml.jackson.core.type.TypeReference<>() {});
            for (Map<String, Object> d : drivers) {
                meta.put(((Number) d.get("driver_number")).intValue(), d);
            }
        }
        // 缺席最近场次的车手（如伤退）不在名单里，从官方积分榜补齐姓名/代码
        try {
            Optional<com.springboot.backend.entity.DataSnapshot> standings =
                    snapshotRepository.findByCacheKey(F1DataSyncService.keyStandings(year));
            if (standings.isPresent()) {
                com.fasterxml.jackson.databind.JsonNode lists = objectMapper
                        .readTree(standings.get().getJsonContent())
                        .path("MRData").path("StandingsTable").path("StandingsLists");
                if (lists.isArray() && lists.size() > 0) {
                    for (com.fasterxml.jackson.databind.JsonNode entry : lists.get(0).path("DriverStandings")) {
                        int number = entry.path("Driver").path("permanentNumber").asInt(0);
                        if (number == 0 || meta.containsKey(number)) {
                            continue;
                        }
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("full_name", entry.path("Driver").path("givenName").asText() + " "
                                + entry.path("Driver").path("familyName").asText());
                        m.put("name_acronym", entry.path("Driver").path("code").asText("???"));
                        m.put("team_name", entry.path("Constructors").path(0).path("name").asText("Unknown"));
                        m.put("team_colour", "888888");
                        meta.put(number, m);
                    }
                }
            }
        } catch (Exception exception) {
            log.warn("车手元数据合并积分榜失败: {}", exception.getMessage());
        }
        return meta;
    }

    private void ensureYear(int year) {
        if (!snapshotRepository.existsByCacheKey(F1DataSyncService.keyMeetings(year))) {
            syncService.ensureYearSynced(year);
        }
    }

    private String getByKey(String cacheKey) {
        return snapshotRepository.findByCacheKey(cacheKey)
                .orElseThrow(() -> new IllegalStateException("数据快照缺失: " + cacheKey))
                .getJsonContent();
    }

    private com.springboot.backend.entity.DataSnapshot newSnapshot(String cacheKey, String json) {
        com.springboot.backend.entity.DataSnapshot snapshot = new com.springboot.backend.entity.DataSnapshot();
        snapshot.setCacheKey(cacheKey);
        snapshot.setJsonContent(json);
        return snapshot;
    }

    private static int compareDate(Map<String, Object> a, Map<String, Object> b) {
        return ((String) a.getOrDefault("date_start", ""))
                .compareTo((String) b.getOrDefault("date_start", ""));
    }
}
