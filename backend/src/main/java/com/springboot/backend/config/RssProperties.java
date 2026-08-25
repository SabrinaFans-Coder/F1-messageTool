package com.springboot.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * RSS 资讯源配置（application.yml 中 news.rss.feeds）
 *
 * @author F1-messageTool
 * @since 2026-08-24
 */
@Component
@ConfigurationProperties(prefix = "news.rss")
public class RssProperties {

    private List<Feed> feeds = new ArrayList<>();

    public List<Feed> getFeeds() { return feeds; }
    public void setFeeds(List<Feed> feeds) { this.feeds = feeds; }

    public static class Feed {

        /** 来源名称 */
        private String name;

        /** RSS 地址 */
        private String url;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }
}
