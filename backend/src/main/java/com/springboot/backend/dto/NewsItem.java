package com.springboot.backend.dto;

import java.time.LocalDateTime;

/**
 * 资讯条目（数据源同步用，未落库的原始条目）
 *
 * @author F1-messageTool
 * @since 2026-08-24
 */
public class NewsItem {

    private String source;
    private String title;
    private String summary;
    private String sourceUrl;
    private String coverImage;
    private String category;
    private LocalDateTime publishedAt;

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
}
