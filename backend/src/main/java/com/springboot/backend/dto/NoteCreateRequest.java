package com.springboot.backend.dto;

/**
 * 创建笔记请求
 *
 * @author F1-messageTool
 * @since 2026-06-03
 */
public class NoteCreateRequest {

    private String title;
    private String content;
    private String tag;
    private String raceName;
    private String driverName;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }
    public String getRaceName() { return raceName; }
    public void setRaceName(String raceName) { this.raceName = raceName; }
    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }
}
