package com.springboot.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 资讯实体（仅保存元数据和摘要，原文通过 sourceUrl 跳转）
 *
 * @author F1-messageTool
 * @since 2026-08-24
 */
@Entity
@Table(
        name = "news",
        uniqueConstraints = @UniqueConstraint(name = "uk_news_source_url", columnNames = {"source", "source_url"})
)
public class News {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 来源名称，如 Autosport F1 */
    @Column(nullable = false, length = 100)
    private String source;

    /** 标题 */
    @Column(nullable = false, length = 500)
    private String title;

    /** 摘要 */
    @Column(columnDefinition = "TEXT")
    private String summary;

    /** 原文链接（长度受唯一索引行大小限制） */
    @Column(name = "source_url", nullable = false, length = 512)
    private String sourceUrl;

    /** 封面图（可为空） */
    @Column(name = "cover_image", length = 1000)
    private String coverImage;

    /** 分类：RACE / DRIVER / TEAM / TECH / TRANSFER / GENERAL */
    @Column(nullable = false, length = 20)
    private String category;

    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public String getCoverImage() { return coverImage; }
    public void setCoverImage(String coverImage) { this.coverImage = coverImage; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
