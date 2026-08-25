package com.springboot.backend.service;

import com.springboot.backend.entity.News;
import com.springboot.backend.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * 资讯业务逻辑服务
 *
 * @author F1-messageTool
 * @since 2026-08-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsService {

    private final NewsRepository newsRepository;

    /**
     * 分页查询资讯列表（支持按分类筛选，按发布时间倒序）
     *
     * @param category 分类筛选（可选）
     * @param page     页码（从 0 开始）
     * @param size     每页条数
     * @return 分页结果
     */
    public Page<News> queryNewsList(String category, int page, int size) {
        log.info("查询资讯列表, category: {}, page: {}, size: {}", category, page, size);
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));
        if (category != null && !category.isEmpty()) {
            return newsRepository.findByCategoryOrderByPublishedAtDesc(category, pageable);
        }
        return newsRepository.findAllByOrderByPublishedAtDesc(pageable);
    }
}
