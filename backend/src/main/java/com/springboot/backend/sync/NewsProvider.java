package com.springboot.backend.sync;

import com.springboot.backend.dto.NewsItem;

import java.util.List;

/**
 * 资讯数据源抽象接口，避免与单一数据源强耦合
 *
 * @author F1-messageTool
 * @since 2026-08-24
 */
public interface NewsProvider {

    /**
     * 拉取最新资讯条目
     *
     * @return 原始资讯条目列表
     */
    List<NewsItem> fetchLatest();
}
