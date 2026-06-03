package com.springboot.backend.dto;

/**
 * 添加收藏车手请求
 *
 * @author F1-messageTool
 * @since 2026-06-03
 */
public class FavoriteCreateRequest {

    private Integer driverNumber;
    private String driverName;
    private String teamName;

    public Integer getDriverNumber() { return driverNumber; }
    public void setDriverNumber(Integer driverNumber) { this.driverNumber = driverNumber; }
    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }
    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }
}
