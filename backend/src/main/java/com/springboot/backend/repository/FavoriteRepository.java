package com.springboot.backend.repository;

import com.springboot.backend.entity.FavoriteDriver;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * 收藏车手数据访问接口
 *
 * @author F1-messageTool
 * @since 2026-06-03
 */
public interface FavoriteRepository extends JpaRepository<FavoriteDriver, Long> {

    Optional<FavoriteDriver> findByDriverNumber(Integer driverNumber);
}
