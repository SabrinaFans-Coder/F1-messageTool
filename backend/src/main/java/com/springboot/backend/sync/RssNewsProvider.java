package com.springboot.backend.sync;

import com.springboot.backend.config.RssProperties;
import com.springboot.backend.dto.NewsItem;
import com.rometools.rome.feed.synd.SyndEnclosure;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

/**
 * RSS 资讯数据源实现，从 application.yml 配置的多个 RSS 源拉取 F1 资讯
 *
 * @author F1-messageTool
 * @since 2026-08-24
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RssNewsProvider implements NewsProvider {

    private final RssProperties rssProperties;

    @Override
    public List<NewsItem> fetchLatest() {
        List<NewsItem> items = new ArrayList<>();
        for (RssProperties.Feed feed : rssProperties.getFeeds()) {
            try {
                items.addAll(fetchFeed(feed));
            } catch (Exception exception) {
                log.error("RSS 源拉取失败, source: {}, url: {}", feed.getName(), feed.getUrl(), exception);
            }
        }
        return items;
    }

    private List<NewsItem> fetchFeed(RssProperties.Feed feed) throws Exception {
        SyndFeed syndFeed = new SyndFeedInput().build(new XmlReader(URI.create(feed.getUrl()).toURL()));
        List<NewsItem> items = new ArrayList<>();
        for (SyndEntry entry : syndFeed.getEntries()) {
            if (entry.getLink() == null || entry.getTitle() == null) {
                continue;
            }
            NewsItem item = new NewsItem();
            item.setSource(feed.getName());
            item.setTitle(entry.getTitle().trim());
            item.setSummary(extractSummary(entry));
            item.setSourceUrl(entry.getLink());
            item.setCoverImage(extractCoverImage(entry));
            item.setCategory(classify(entry));
            item.setPublishedAt(extractPublishedAt(entry));
            items.add(item);
        }
        log.info("RSS 源拉取完成, source: {}, count: {}", feed.getName(), items.size());
        return items;
    }

    private String extractSummary(SyndEntry entry) {
        if (entry.getDescription() == null || entry.getDescription().getValue() == null) {
            return null;
        }
        String text = entry.getDescription().getValue().replaceAll("<[^>]+>", "").trim();
        return text.isEmpty() ? null : text;
    }

    private String extractCoverImage(SyndEntry entry) {
        if (entry.getEnclosures() == null) {
            return null;
        }
        return entry.getEnclosures().stream()
                .filter(enclosure -> enclosure.getType() == null || enclosure.getType().startsWith("image"))
                .map(SyndEnclosure::getUrl)
                .findFirst()
                .orElse(null);
    }

    private LocalDateTime extractPublishedAt(SyndEntry entry) {
        Date date = entry.getPublishedDate() != null ? entry.getPublishedDate() : entry.getUpdatedDate();
        return date != null
                ? LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault())
                : LocalDateTime.now();
    }

    /**
     * 基于标题关键词的简单分类（V1 启发式规则，按词边界匹配避免误判）
     */
    private String classify(SyndEntry entry) {
        String text = (entry.getTitle() + " " + (entry.getDescription() != null && entry.getDescription().getValue() != null ? entry.getDescription().getValue() : "")).toLowerCase();
        if (containsAny(text, "sign", "contract", "seat", "move to", "switch to", "join", "departure", "leave")) {
            return "TRANSFER";
        }
        if (containsAny(text, "upgrade", "engine", "power unit", "aero", "floor", "wing", "technical")) {
            return "TECH";
        }
        if (containsAny(text, "driver")) {
            return "DRIVER";
        }
        if (containsAny(text, "grand prix", "qualifying", "pole", "podium", "sprint", "race", "lap", "grid", "win", "victory")) {
            return "RACE";
        }
        if (containsAny(text, "ferrari", "mercedes", "red bull", "mclaren", "aston martin", "williams", "alpine", "haas", "sauber", "team principal")) {
            return "TEAM";
        }
        return "GENERAL";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (Pattern.compile("\\b" + Pattern.quote(keyword) + "\\b").matcher(text).find()) {
                return true;
            }
        }
        return false;
    }
}
