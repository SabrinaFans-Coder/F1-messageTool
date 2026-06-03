package com.springboot.backend.service;

import com.springboot.backend.common.BusinessException;
import com.springboot.backend.dto.NoteCreateRequest;
import com.springboot.backend.dto.NoteUpdateRequest;
import com.springboot.backend.entity.Note;
import com.springboot.backend.repository.NoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * 笔记业务逻辑服务
 *
 * @author F1-messageTool
 * @since 2026-06-03
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository noteRepository;

    /**
     * 查询笔记列表（支持按标签、赛事、车手筛选）
     *
     * @param tag        标签筛选（可选）
     * @param raceName   赛事名称筛选（可选）
     * @param driverName 车手名称筛选（可选）
     * @return 笔记列表
     */
    public List<Note> queryNoteList(String tag, String raceName, String driverName) {
        log.info("查询笔记列表, tag: {}, raceName: {}, driverName: {}", tag, raceName, driverName);
        if (tag != null && !tag.isEmpty()) {
            return noteRepository.findByTagOrderByCreatedAtDesc(tag);
        }
        if (raceName != null && !raceName.isEmpty()) {
            return noteRepository.findByRaceNameOrderByCreatedAtDesc(raceName);
        }
        if (driverName != null && !driverName.isEmpty()) {
            return noteRepository.findByDriverNameOrderByCreatedAtDesc(driverName);
        }
        return noteRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * 根据ID获取笔记
     *
     * @param noteId 笔记ID
     * @return 笔记实体
     * @throws BusinessException 当笔记不存在时抛出
     */
    public Note queryNoteById(Long noteId) {
        log.info("查询笔记详情, noteId: {}", noteId);
        return noteRepository.findById(noteId)
                .orElseThrow(() -> new BusinessException("笔记不存在，noteId: " + noteId));
    }

    /**
     * 创建笔记
     *
     * @param request 创建请求
     * @return 创建的笔记
     */
    public Note createNote(NoteCreateRequest request) {
        log.info("创建笔记, title: {}", request.getTitle());
        try {
            Note note = new Note();
            note.setTitle(request.getTitle());
            note.setContent(request.getContent());
            note.setTag(request.getTag());
            note.setRaceName(request.getRaceName());
            note.setDriverName(request.getDriverName());
            Note saved = noteRepository.save(note);
            log.info("笔记创建成功, noteId: {}", saved.getId());
            return saved;
        } catch (Exception exception) {
            log.error("笔记创建失败, title: {}", request.getTitle(), exception);
            throw new BusinessException("笔记创建失败，title: " + request.getTitle(), exception);
        }
    }

    /**
     * 更新笔记
     *
     * @param noteId  笔记ID
     * @param request 更新请求
     * @return 更新后的笔记
     * @throws BusinessException 当笔记不存在时抛出
     */
    public Note updateNote(Long noteId, NoteUpdateRequest request) {
        log.info("更新笔记, noteId: {}", noteId);
        Note note = queryNoteById(noteId);
        if (request.getTitle() != null) {
            note.setTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            note.setContent(request.getContent());
        }
        if (request.getTag() != null) {
            note.setTag(request.getTag());
        }
        if (request.getRaceName() != null) {
            note.setRaceName(request.getRaceName());
        }
        if (request.getDriverName() != null) {
            note.setDriverName(request.getDriverName());
        }
        Note updated = noteRepository.save(note);
        log.info("笔记更新成功, noteId: {}", noteId);
        return updated;
    }

    /**
     * 删除笔记
     *
     * @param noteId 笔记ID
     * @throws BusinessException 当笔记不存在时抛出
     */
    public void deleteNote(Long noteId) {
        log.info("删除笔记, noteId: {}", noteId);
        if (!noteRepository.existsById(noteId)) {
            throw new BusinessException("笔记不存在，noteId: " + noteId);
        }
        noteRepository.deleteById(noteId);
        log.info("笔记删除成功, noteId: {}", noteId);
    }
}
