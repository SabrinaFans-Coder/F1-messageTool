package com.springboot.backend.repository;

import com.springboot.backend.entity.DataSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 数据快照访问接口
 *
 * @author F1-messageTool
 * @since 2026-08-25
 */
public interface DataSnapshotRepository extends JpaRepository<DataSnapshot, Long> {

    Optional<DataSnapshot> findByCacheKey(String cacheKey);

    List<DataSnapshot> findByCacheKeyStartingWith(String prefix);

    boolean existsByCacheKey(String cacheKey);
}
