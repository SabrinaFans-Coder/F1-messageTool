package com.springboot.backend.repository;

import com.springboot.backend.entity.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * 笔记数据访问接口
 *
 * @author F1-messageTool
 * @since 2026-06-03
 */
public interface NoteRepository extends JpaRepository<Note, Long> {

    List<Note> findByTagOrderByCreatedAtDesc(String tag);

    List<Note> findByRaceNameOrderByCreatedAtDesc(String raceName);

    List<Note> findByDriverNameOrderByCreatedAtDesc(String driverName);

    List<Note> findAllByOrderByCreatedAtDesc();
}
