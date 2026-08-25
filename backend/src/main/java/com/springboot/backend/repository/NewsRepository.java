package com.springboot.backend.repository;

import com.springboot.backend.entity.News;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 资讯数据访问接口
 *
 * @author F1-messageTool
 * @since 2026-08-24
 */
public interface NewsRepository extends JpaRepository<News, Long> {

    boolean existsBySourceAndSourceUrl(String source, String sourceUrl);

    Page<News> findByCategoryOrderByPublishedAtDesc(String category, Pageable pageable);

    Page<News> findAllByOrderByPublishedAtDesc(Pageable pageable);
}
