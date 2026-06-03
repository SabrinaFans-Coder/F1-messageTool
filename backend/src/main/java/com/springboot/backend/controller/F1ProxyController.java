package com.springboot.backend.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.backend.common.Result;
import com.springboot.backend.service.JolpicaService;
import com.springboot.backend.service.OpenF1Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * F1 数据代理控制器
 * 代理 OpenF1 API 请求，提供缓存支持
 *
 * @author F1-messageTool
 * @since 2026-06-03
 */
@Slf4j
@RestController
@RequestMapping("/api/f1")
@RequiredArgsConstructor
public class F1ProxyController {

    private final OpenF1Service openF1Service;
    private final JolpicaService jolpicaService;
    private final ObjectMapper objectMapper;

    @GetMapping("/meetings")
    public Result<Object> queryMeetings(@RequestParam(defaultValue = "2026") int year) throws Exception {
        String json = openF1Service.fetchMeetings(year);
        return Result.success(objectMapper.readValue(json, new TypeReference<>() {}));
    }

    @GetMapping("/sessions")
    public Result<Object> querySessions(@RequestParam(defaultValue = "2026") int year) throws Exception {
        String json = openF1Service.fetchSessions(year);
        return Result.success(objectMapper.readValue(json, new TypeReference<>() {}));
    }

    @GetMapping("/sessions/latest")
    public Result<Object> queryLatestRaceSession() throws Exception {
        String json = openF1Service.fetchLatestRaceSession();
        return Result.success(objectMapper.readValue(json, new TypeReference<>() {}));
    }

    @GetMapping("/drivers")
    public Result<Object> queryDrivers(@RequestParam int sessionKey) throws Exception {
        String json = openF1Service.fetchDrivers(sessionKey);
        return Result.success(objectMapper.readValue(json, new TypeReference<>() {}));
    }

    @GetMapping("/positions")
    public Result<Object> queryPositions(@RequestParam int sessionKey) throws Exception {
        String json = openF1Service.fetchPositions(sessionKey);
        return Result.success(objectMapper.readValue(json, new TypeReference<>() {}));
    }

    @GetMapping("/circuit-history")
    public Result<Object> queryCircuitHistory(@RequestParam int meetingKey) throws Exception {
        String json = openF1Service.fetchCircuitHistory(meetingKey);
        return Result.success(objectMapper.readValue(json, new TypeReference<>() {}));
    }

    @GetMapping("/season-rankings")
    public Result<Object> querySeasonRankings(
            @RequestParam(defaultValue = "2026") int year,
            @RequestParam(defaultValue = "5") int limit) throws Exception {
        String json = openF1Service.fetchSeasonRankings(year, limit);
        return Result.success(objectMapper.readValue(json, new TypeReference<>() {}));
    }

    @GetMapping("/season-standings")
    public Result<Object> querySeasonStandings(
            @RequestParam(defaultValue = "2026") int year) throws Exception {
        // 用 Jolpica 获取车手积分榜（单次请求）
        String driverJson = jolpicaService.fetchDriverStandings(year);
        java.util.Map<String, Object> driverData = objectMapper.readValue(driverJson, new TypeReference<>() {});

        // 用 Jolpica 获取车队积分榜（单次请求）
        String constructorJson = jolpicaService.fetchConstructorStandings(year);
        java.util.Map<String, Object> constructorData = objectMapper.readValue(constructorJson, new TypeReference<>() {});

        // 转换车手积分榜
        java.util.List<java.util.Map<String, Object>> driverRankings = new java.util.ArrayList<>();
        java.util.Map<String, Object> mrData = (java.util.Map<String, Object>) driverData.get("MRData");
        java.util.Map<String, Object> standingsTable = (java.util.Map<String, Object>) mrData.get("StandingsTable");
        java.util.List<java.util.Map<String, Object>> standingsLists = (java.util.List<java.util.Map<String, Object>>) standingsTable.get("StandingsLists");

        int racesCount = 0;
        if (!standingsLists.isEmpty()) {
            java.util.Map<String, Object> firstList = standingsLists.get(0);
            racesCount = Integer.parseInt((String) firstList.getOrDefault("round", "0"));
            java.util.List<java.util.Map<String, Object>> driverStandings =
                    (java.util.List<java.util.Map<String, Object>>) firstList.get("DriverStandings");

            for (java.util.Map<String, Object> entry : driverStandings) {
                java.util.Map<String, Object> driver = (java.util.Map<String, Object>) entry.get("Driver");
                java.util.List<java.util.Map<String, Object>> constructors =
                        (java.util.List<java.util.Map<String, Object>>) entry.get("Constructors");
                String teamName = !constructors.isEmpty() ? (String) constructors.get(0).get("name") : "Unknown";

                java.util.Map<String, Object> ranking = new java.util.LinkedHashMap<>();
                ranking.put("position", Integer.parseInt((String) entry.get("position")));
                ranking.put("driver_number", Integer.parseInt((String) driver.get("permanentNumber")));
                ranking.put("driver_name", driver.get("givenName") + " " + driver.get("familyName"));
                ranking.put("name_acronym", driver.get("code"));
                ranking.put("team_name", teamName);
                ranking.put("totalPoints", Integer.parseInt((String) entry.get("points")));
                ranking.put("wins", Integer.parseInt((String) entry.get("wins")));
                ranking.put("podiums", 0); // Jolpica 不提供领奖台数据
                driverRankings.add(ranking);
            }
        }

        // 转换车队积分榜
        java.util.List<java.util.Map<String, Object>> constructorRankings = new java.util.ArrayList<>();
        java.util.Map<String, Object> cMrData = (java.util.Map<String, Object>) constructorData.get("MRData");
        java.util.Map<String, Object> cStandingsTable = (java.util.Map<String, Object>) cMrData.get("StandingsTable");
        java.util.List<java.util.Map<String, Object>> cStandingsLists = (java.util.List<java.util.Map<String, Object>>) cStandingsTable.get("StandingsLists");

        if (!cStandingsLists.isEmpty()) {
            java.util.List<java.util.Map<String, Object>> teamStandings =
                    (java.util.List<java.util.Map<String, Object>>) cStandingsLists.get(0).get("ConstructorStandings");

            for (java.util.Map<String, Object> entry : teamStandings) {
                java.util.Map<String, Object> constructor = (java.util.Map<String, Object>) entry.get("Constructor");

                java.util.Map<String, Object> ranking = new java.util.LinkedHashMap<>();
                ranking.put("position", Integer.parseInt((String) entry.get("position")));
                ranking.put("team_name", constructor.get("name"));
                ranking.put("totalPoints", Integer.parseInt((String) entry.get("points")));
                ranking.put("wins", Integer.parseInt((String) entry.get("wins")));
                ranking.put("podiums", 0);
                ranking.put("races", racesCount);
                constructorRankings.add(ranking);
            }
        }

        // 构造统一响应
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("year", year);
        result.put("racesCount", racesCount);
        result.put("driverRankings", driverRankings);
        result.put("constructorRankings", constructorRankings);

        return Result.success(result);
    }

    @GetMapping("/driver-detail")
    public Result<Object> queryDriverDetail(
            @RequestParam int driverNumber,
            @RequestParam(defaultValue = "2026") int year) throws Exception {
        String json = openF1Service.fetchDriverDetail(driverNumber, year);
        return Result.success(objectMapper.readValue(json, new TypeReference<>() {}));
    }

    @GetMapping("/constructor-rankings")
    public Result<Object> queryConstructorRankings(
            @RequestParam(defaultValue = "2026") int year) throws Exception {
        String json = openF1Service.fetchConstructorRankings(year);
        return Result.success(objectMapper.readValue(json, new TypeReference<>() {}));
    }

    /**
     * 获取车手列表（Jolpica 积分榜 + OpenF1 headshot/colour）
     * Jolpica: 车手信息、车队、积分
     * OpenF1: headshot_url、team_colour（仅 1 次 API 调用）
     */
    @GetMapping("/drivers-list")
    public Result<Object> queryDriversList(
            @RequestParam(defaultValue = "2026") int year) throws Exception {
        // 1. 从 Jolpica 获取积分榜（含所有车手基本信息）
        String standingsJson = jolpicaService.fetchDriverStandings(year);
        java.util.Map<String, Object> data = objectMapper.readValue(standingsJson, new TypeReference<>() {});

        java.util.Map<String, Object> mrData = (java.util.Map<String, Object>) data.get("MRData");
        java.util.Map<String, Object> standingsTable = (java.util.Map<String, Object>) mrData.get("StandingsTable");
        java.util.List<java.util.Map<String, Object>> standingsLists =
                (java.util.List<java.util.Map<String, Object>>) standingsTable.get("StandingsLists");

        // 2. 从 OpenF1 获取最新会话的车手数据（headshot_url, team_colour, team_name）
        java.util.Map<Integer, java.util.Map<String, Object>> openF1Drivers = fetchOpenF1DriverMap();

        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();

        if (!standingsLists.isEmpty()) {
            java.util.List<java.util.Map<String, Object>> driverStandings =
                    (java.util.List<java.util.Map<String, Object>>) standingsLists.get(0).get("DriverStandings");

            for (java.util.Map<String, Object> entry : driverStandings) {
                java.util.Map<String, Object> driver = (java.util.Map<String, Object>) entry.get("Driver");
                java.util.List<java.util.Map<String, Object>> constructors =
                        (java.util.List<java.util.Map<String, Object>>) entry.get("Constructors");

                String givenName = (String) driver.get("givenName");
                String familyName = (String) driver.get("familyName");
                String code = (String) driver.get("code");
                int driverNumber = Integer.parseInt((String) driver.get("permanentNumber"));
                String constructorId = !constructors.isEmpty() ? (String) constructors.get(0).get("constructorId") : "";
                String jolpicaTeamName = !constructors.isEmpty() ? (String) constructors.get(0).get("name") : "Unknown";

                // 优先用 OpenF1 的 headshot、colour、team_name
                java.util.Map<String, Object> oF1 = openF1Drivers.getOrDefault(driverNumber, java.util.Map.of());

                java.util.Map<String, Object> driverInfo = new java.util.LinkedHashMap<>();
                driverInfo.put("driver_number", driverNumber);
                driverInfo.put("full_name", givenName + " " + familyName);
                driverInfo.put("name_acronym", code);
                driverInfo.put("team_name", oF1.getOrDefault("team_name", jolpicaTeamName));
                driverInfo.put("team_colour", oF1.getOrDefault("team_colour", getTeamColour(constructorId)));
                driverInfo.put("headshot_url", oF1.getOrDefault("headshot_url", ""));
                driverInfo.put("nationality", driver.get("nationality"));
                result.add(driverInfo);
            }
        }

        return Result.success(result);
    }

    /**
     * 车手详情（Jolpica 版本）
     * 用 Jolpica 获取赛季统计和比赛结果，无需 OpenF1
     */
    @GetMapping("/driver-detail-v2")
    public Result<Object> queryDriverDetailV2(
            @RequestParam int driverNumber,
            @RequestParam(defaultValue = "2026") int year) throws Exception {
        // 1. 从积分榜获取车手基本信息
        String standingsJson = jolpicaService.fetchDriverStandings(year);
        java.util.Map<String, Object> standingsData = objectMapper.readValue(standingsJson, new TypeReference<>() {});

        java.util.Map<String, Object> mrData = (java.util.Map<String, Object>) standingsData.get("MRData");
        java.util.Map<String, Object> standingsTable = (java.util.Map<String, Object>) mrData.get("StandingsTable");
        java.util.List<java.util.Map<String, Object>> standingsLists =
                (java.util.List<java.util.Map<String, Object>>) standingsTable.get("StandingsLists");

        if (standingsLists.isEmpty()) {
            throw new com.springboot.backend.common.BusinessException("未找到赛季数据, year: " + year);
        }

        java.util.Map<String, Object> firstList = standingsLists.get(0);
        int racesCount = Integer.parseInt((String) firstList.getOrDefault("round", "0"));
        java.util.List<java.util.Map<String, Object>> driverStandings =
                (java.util.List<java.util.Map<String, Object>>) firstList.get("DriverStandings");

        // 查找目标车手
        java.util.Map<String, Object> targetEntry = null;
        for (java.util.Map<String, Object> entry : driverStandings) {
            java.util.Map<String, Object> d = (java.util.Map<String, Object>) entry.get("Driver");
            if (Integer.parseInt((String) d.get("permanentNumber")) == driverNumber) {
                targetEntry = entry;
                break;
            }
        }

        if (targetEntry == null) {
            throw new com.springboot.backend.common.BusinessException("未找到车手, driverNumber: " + driverNumber);
        }

        java.util.Map<String, Object> driver = (java.util.Map<String, Object>) targetEntry.get("Driver");
        java.util.List<java.util.Map<String, Object>> constructors =
                (java.util.List<java.util.Map<String, Object>>) targetEntry.get("Constructors");
        String constructorId = !constructors.isEmpty() ? (String) constructors.get(0).get("constructorId") : "";
        String teamName = !constructors.isEmpty() ? (String) constructors.get(0).get("name") : "Unknown";
        String givenName = (String) driver.get("givenName");
        String familyName = (String) driver.get("familyName");
        String code = (String) driver.get("code");

        // 2. 获取每场比赛结果
        java.util.List<java.util.Map<String, Object>> raceResults = new java.util.ArrayList<>();
        for (int round = 1; round <= racesCount; round++) {
            try {
                String raceJson = jolpicaService.fetchRaceResult(year, round);
                java.util.Map<String, Object> raceData = objectMapper.readValue(raceJson, new TypeReference<>() {});

                java.util.Map<String, Object> raceMrData = (java.util.Map<String, Object>) raceData.get("MRData");
                java.util.Map<String, Object> raceTable = (java.util.Map<String, Object>) raceMrData.get("RaceTable");
                java.util.List<java.util.Map<String, Object>> races =
                        (java.util.List<java.util.Map<String, Object>>) raceTable.get("Races");

                if (!races.isEmpty()) {
                    java.util.Map<String, Object> race = races.get(0);
                    String meetingName = (String) race.get("raceName");
                    java.util.Map<String, Object> circuit = (java.util.Map<String, Object>) race.get("Circuit");
                    String circuitName = (String) circuit.get("circuitName");

                    java.util.List<java.util.Map<String, Object>> results =
                            (java.util.List<java.util.Map<String, Object>>) race.get("Results");

                    for (java.util.Map<String, Object> r : results) {
                        java.util.Map<String, Object> rDriver = (java.util.Map<String, Object>) r.get("Driver");
                        if (Integer.parseInt((String) rDriver.get("permanentNumber")) == driverNumber) {
                            java.util.Map<String, Object> raceEntry = new java.util.LinkedHashMap<>();
                            raceEntry.put("meeting_name", meetingName);
                            raceEntry.put("circuit_short_name", circuitName);
                            raceEntry.put("position", Integer.parseInt((String) r.get("position")));
                            raceEntry.put("round", round);
                            raceResults.add(raceEntry);
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("获取第{}轮比赛结果失败, 跳过", round);
            }
        }

        // 3. 从 OpenF1 获取 headshot 和 team_colour（1 次调用）
        java.util.Map<Integer, java.util.Map<String, Object>> openF1Drivers = fetchOpenF1DriverMap();
        java.util.Map<String, Object> oF1 = openF1Drivers.getOrDefault(driverNumber, java.util.Map.of());

        // 4. 构造响应
        java.util.Map<String, Object> seasonStats = new java.util.LinkedHashMap<>();
        seasonStats.put("totalPoints", Integer.parseInt((String) targetEntry.get("points")));
        seasonStats.put("wins", Integer.parseInt((String) targetEntry.get("wins")));
        seasonStats.put("podiums", 0);
        seasonStats.put("races", racesCount);
        seasonStats.put("rank", Integer.parseInt((String) targetEntry.get("position")));

        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("driver_number", driverNumber);
        result.put("full_name", givenName + " " + familyName);
        result.put("name_acronym", code);
        result.put("team_name", oF1.getOrDefault("team_name", teamName));
        result.put("team_colour", oF1.getOrDefault("team_colour", getTeamColour(constructorId)));
        result.put("headshot_url", oF1.getOrDefault("headshot_url", ""));
        result.put("nationality", driver.get("nationality"));
        result.put("seasonStats", seasonStats);
        result.put("raceResults", raceResults);

        return Result.success(result);
    }

    /**
     * 从 OpenF1 获取最新会话的车手数据，构建 driver_number → info 的映射
     * 仅调用 1 次 OpenF1 API，结果包含 headshot_url、team_colour、team_name
     */
    private java.util.Map<Integer, java.util.Map<String, Object>> fetchOpenF1DriverMap() {
        try {
            String latestJson = openF1Service.fetchLatestRaceSession();
            java.util.List<java.util.Map<String, Object>> sessions =
                    objectMapper.readValue(latestJson, new TypeReference<>() {});
            if (sessions.isEmpty()) return java.util.Map.of();

            int latestKey = ((Number) sessions.get(0).get("session_key")).intValue();
            String driversJson = openF1Service.fetchDrivers(latestKey);
            java.util.List<java.util.Map<String, Object>> drivers =
                    objectMapper.readValue(driversJson, new TypeReference<>() {});

            java.util.Map<Integer, java.util.Map<String, Object>> map = new java.util.LinkedHashMap<>();
            for (java.util.Map<String, Object> d : drivers) {
                int num = ((Number) d.get("driver_number")).intValue();
                map.put(num, d);
            }
            return map;
        } catch (Exception e) {
            log.warn("获取 OpenF1 车手数据失败，使用 Jolpica fallback", e);
            return java.util.Map.of();
        }
    }

    /**
     * 构造 F1 官方 headshot URL
     * 模式: https://www.formula1.com/content/dam/fom-website/drivers/{DIR}/{FIRST3}{CODE}01_{FirstName}_{LastName}/{first3}{code}01.png.transform/1col/image.png
     */
    private String buildHeadshotUrl(String givenName, String familyName, String code) {
        if (givenName == null || familyName == null || code == null) return "";
        String first3 = givenName.length() >= 3 ? givenName.substring(0, 3) : givenName;
        String code3 = code.length() >= 3 ? code : familyName.substring(0, Math.min(3, familyName.length()));
        String dir = String.valueOf(Character.toUpperCase(familyName.charAt(0)));
        String dirName = first3.toUpperCase() + code3.toUpperCase() + "01_" + givenName + "_" + familyName;
        String fileName = first3.toLowerCase() + code3.toLowerCase() + "01.png.transform/1col/image.png";
        return "https://www.formula1.com/content/dam/fom-website/drivers/" + dir + "/" + dirName + "/" + fileName;
    }

    private String getTeamColour(String constructorId) {
        return switch (constructorId) {
            case "mclaren" -> "FF8000";
            case "mercedes" -> "27F4D2";
            case "red_bull" -> "3671C6";
            case "ferrari" -> "E8002D";
            case "williams" -> "64C4FF";
            case "aston_martin" -> "229971";
            case "alpine" -> "FF87BC";
            case "haas" -> "B6BABD";
            case "sauber" -> "52E252";
            case "rb" -> "6692FF";
            default -> "888888";
        };
    }
}
