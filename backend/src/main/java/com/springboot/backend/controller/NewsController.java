package com.springboot.backend.controller;

import com.springboot.backend.common.Result;
import com.springboot.backend.entity.News;
import com.springboot.backend.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.*;

/**
 * 资讯查询控制器
 *
 * @author F1-messageTool
 * @since 2026-08-24
 */
@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;

    @GetMapping
    public Result<PagedModel<News>> queryNewsList(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(new PagedModel<>(newsService.queryNewsList(category, page, size)));
    }
}
