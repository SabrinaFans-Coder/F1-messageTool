package com.springboot.backend.entity;

import jakarta.persistence.*;

/**
 * 收藏车手实体
 *
 * @author F1-messageTool
 * @since 2026-06-03
 */
@Entity
@Table(name = "favorite_drivers")
public class FavoriteDriver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "driver_number", nullable = false, unique = true)
    private Integer driverNumber;

    @Column(name = "driver_name", nullable = false)
    private String driverName;

    @Column(name = "team_name", nullable = false)
    private String teamName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getDriverNumber() { return driverNumber; }
    public void setDriverNumber(Integer driverNumber) { this.driverNumber = driverNumber; }
    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }
    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }
}
