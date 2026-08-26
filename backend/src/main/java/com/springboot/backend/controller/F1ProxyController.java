package com.springboot.backend.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.backend.common.Result;
import java.util.List;
import java.util.Map;

import com.springboot.backend.service.F1QueryService;
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
    private final ObjectMapper objectMapper;
    private final F1QueryService f1QueryService;

    @GetMapping("/meetings")
    public Result<Object> queryMeetings(@RequestParam(defaultValue = "2026") int year) throws Exception {
        String json = f1QueryService.getMeetings(year);
        return Result.success(objectMapper.readValue(json, new TypeReference<>() {}));
    }

    @GetMapping("/sessions")
    public Result<Object> querySessions(@RequestParam(defaultValue = "2026") int year) throws Exception {
        String json = openF1Service.fetchSessions(year);
        return Result.success(objectMapper.readValue(json, new TypeReference<>() {}));
    }

    @GetMapping("/sessions/latest")
    public Result<Object> queryLatestRaceSession() throws Exception {
        String json = f1QueryService.getLatestSession();
        return Result.success(objectMapper.readValue(json, new TypeReference<>() {}));
    }

    @GetMapping("/drivers")
    public Result<Object> queryDrivers(@RequestParam int sessionKey) throws Exception {
        String json = openF1Service.fetchDrivers(sessionKey);
        return Result.success(objectMapper.readValue(json, new TypeReference<>() {}));
    }

    @GetMapping("/positions")
    public Result<Object> queryPositions(@RequestParam int sessionKey) throws Exception {
        String json = f1QueryService.getPositions(sessionKey);
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
        String json = f1QueryService.getSeasonRankings(year, limit);
        return Result.success(objectMapper.readValue(json, new TypeReference<>() {}));
    }

    @GetMapping("/points-trend")
    public Result<Object> queryPointsTrend(
            @RequestParam(defaultValue = "2026") int year,
            @RequestParam(defaultValue = "10") int top) throws Exception {
        String allJson = f1QueryService.getAllDriversPointsTrend(year);
        com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(allJson);
        java.util.List<Object> drivers = new java.util.ArrayList<>();
        root.get("drivers").forEach(node -> {
            if (drivers.size() < Math.max(top, 1)) drivers.add(node);
        });
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("year", year);
        result.put("estimated", true);
        result.put("rounds", objectMapper.convertValue(root.get("rounds"), List.class));
        result.put("drivers", drivers);
        return Result.success(result);
    }

    @GetMapping("/season-standings")
    public Result<Object> querySeasonStandings(
            @RequestParam(defaultValue = "2026") int year) throws Exception {
        // 车手/车队双榜单均由本地积分榜快照转换
        String json = f1QueryService.getSeasonStandings(year);
        return Result.success(objectMapper.readValue(json, new TypeReference<>() {}));
    }

    /**
     * 获取车手列表（本地快照版）
     * 积分榜快照定顺序/国籍，车手名单快照补头像/涂装，无上游调用
     */
    @GetMapping("/drivers-list")
    public Result<Object> queryDriversList(
            @RequestParam(defaultValue = "2026") int year) throws Exception {
        String json = f1QueryService.getDriversList(year);
        return Result.success(objectMapper.readValue(json, new TypeReference<>() {}));
    }

    /**
     * 车手详情（本地快照版）
     * 元数据/积分取本地库，逐场结果取名次快照，无上游实时调用
     */
    @GetMapping("/driver-detail-v2")
    public Result<Object> queryDriverDetailV2(
            @RequestParam int driverNumber,
            @RequestParam(defaultValue = "2026") int year) throws Exception {
        String json = f1QueryService.getDriverDetail(driverNumber, year);
        return Result.success(objectMapper.readValue(json, new TypeReference<>() {}));
    }
}
