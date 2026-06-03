package com.springboot.backend.controller;

import com.springboot.backend.common.Result;
import com.springboot.backend.dto.FavoriteCreateRequest;
import com.springboot.backend.entity.FavoriteDriver;
import com.springboot.backend.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 收藏车手控制器
 *
 * @author F1-messageTool
 * @since 2026-06-03
 */
@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @GetMapping
    public Result<List<FavoriteDriver>> queryFavoriteList() {
        return Result.success(favoriteService.queryFavoriteList());
    }

    @PostMapping
    public Result<FavoriteDriver> addFavorite(@RequestBody FavoriteCreateRequest request) {
        return Result.success(favoriteService.addFavorite(request));
    }

    @DeleteMapping("/{favoriteId}")
    public Result<Void> removeFavorite(@PathVariable Long favoriteId) {
        favoriteService.removeFavorite(favoriteId);
        return Result.success();
    }
}
