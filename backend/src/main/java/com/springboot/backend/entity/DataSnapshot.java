package com.springboot.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * F1 数据快照：以内容键存储上游 API 的原始 JSON，
 * 定时同步写入，读路径只查本地库不再实时爬取。
 *
 * 键规范：f1:meetings:{year} / f1:positions:{sessionKey} / f1:standings:{year} ...
 *
 * @author F1-messageTool
 * @since 2026-08-25
 */
@Entity
@Table(
        name = "data_snapshot",
        uniqueConstraints = @UniqueConstraint(name = "uk_snapshot_key", columnNames = "cache_key")
)
public class DataSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cache_key", nullable = false, length = 200)
    private String cacheKey;

    @Column(name = "json_content", nullable = false, columnDefinition = "TEXT")
    private String jsonContent;

    @Column(name = "synced_at", nullable = false)
    private LocalDateTime syncedAt;

    @PrePersist
    protected void onCreate() {
        syncedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCacheKey() { return cacheKey; }
    public void setCacheKey(String cacheKey) { this.cacheKey = cacheKey; }
    public String getJsonContent() { return jsonContent; }
    public void setJsonContent(String jsonContent) { this.jsonContent = jsonContent; }
    public LocalDateTime getSyncedAt() { return syncedAt; }
    public void setSyncedAt(LocalDateTime syncedAt) { this.syncedAt = syncedAt; }
}
