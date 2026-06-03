package com.springboot.backend.service;

import com.springboot.backend.common.BusinessException;
import com.springboot.backend.dto.FavoriteCreateRequest;
import com.springboot.backend.entity.FavoriteDriver;
import com.springboot.backend.repository.FavoriteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * 收藏车手业务逻辑服务
 *
 * @author F1-messageTool
 * @since 2026-06-03
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;

    /**
     * 查询所有收藏车手
     *
     * @return 收藏车手列表
     */
    public List<FavoriteDriver> queryFavoriteList() {
        log.info("查询收藏车手列表");
        return favoriteRepository.findAll();
    }

    /**
     * 添加收藏车手
     *
     * @param request 添加请求
     * @return 创建的收藏记录
     * @throws BusinessException 当车手已被收藏时抛出
     */
    public FavoriteDriver addFavorite(FavoriteCreateRequest request) {
        log.info("添加收藏车手, driverNumber: {}", request.getDriverNumber());
        favoriteRepository.findByDriverNumber(request.getDriverNumber())
                .ifPresent(existing -> {
                    throw new BusinessException("车手已被收藏，driverNumber: " + request.getDriverNumber());
                });
        try {
            FavoriteDriver favorite = new FavoriteDriver();
            favorite.setDriverNumber(request.getDriverNumber());
            favorite.setDriverName(request.getDriverName());
            favorite.setTeamName(request.getTeamName());
            FavoriteDriver saved = favoriteRepository.save(favorite);
            log.info("收藏车手添加成功, id: {}", saved.getId());
            return saved;
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("收藏车手添加失败, driverNumber: {}", request.getDriverNumber(), exception);
            throw new BusinessException("收藏车手添加失败", exception);
        }
    }

    /**
     * 删除收藏车手
     *
     * @param favoriteId 收藏记录ID
     * @throws BusinessException 当收藏记录不存在时抛出
     */
    public void removeFavorite(Long favoriteId) {
        log.info("删除收藏车手, favoriteId: {}", favoriteId);
        if (!favoriteRepository.existsById(favoriteId)) {
            throw new BusinessException("收藏记录不存在，favoriteId: " + favoriteId);
        }
        favoriteRepository.deleteById(favoriteId);
        log.info("收藏车手删除成功, favoriteId: {}", favoriteId);
    }
}
