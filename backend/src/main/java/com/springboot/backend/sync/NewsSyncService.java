package com.springboot.backend.sync;

import com.springboot.backend.dto.NewsItem;
import com.springboot.backend.entity.News;
import com.springboot.backend.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 资讯定时同步服务：拉取 → 去重 → 落库
 *
 * @author F1-messageTool
 * @since 2026-08-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "news.sync-enabled", havingValue = "true", matchIfMissing = true)
public class NewsSyncService {

    private final NewsProvider newsProvider;
    private final NewsRepository newsRepository;

    /**
     * 启动时同步一次，之后每 30 分钟同步一次（单线程内串行执行，天然防止并发重叠）
     */
    @Scheduled(initialDelay = 0, fixedDelay = 30 * 60 * 1000)
    public void syncNews() {
        log.info("开始同步 F1 资讯");
        List<NewsItem> items = newsProvider.fetchLatest();
        int inserted = 0;
        for (NewsItem item : items) {
            if (item.getSourceUrl() == null || item.getTitle() == null) {
                continue;
            }
            if (newsRepository.existsBySourceAndSourceUrl(item.getSource(), item.getSourceUrl())) {
                continue;
            }
            newsRepository.save(toEntity(item));
            inserted++;
        }
        log.info("F1 资讯同步完成, fetched: {}, inserted: {}", items.size(), inserted);
    }

    private News toEntity(NewsItem item) {
        News news = new News();
        news.setSource(item.getSource());
        news.setTitle(item.getTitle());
        news.setSummary(item.getSummary());
        news.setSourceUrl(item.getSourceUrl());
        news.setCoverImage(item.getCoverImage());
        news.setCategory(item.getCategory() != null ? item.getCategory() : "GENERAL");
        news.setPublishedAt(item.getPublishedAt() != null ? item.getPublishedAt() : LocalDateTime.now());
        return news;
    }
}
