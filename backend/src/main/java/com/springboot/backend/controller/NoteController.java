package com.springboot.backend.controller;

import com.springboot.backend.common.Result;
import com.springboot.backend.dto.NoteCreateRequest;
import com.springboot.backend.dto.NoteUpdateRequest;
import com.springboot.backend.entity.Note;
import com.springboot.backend.service.NoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 笔记 CRUD 控制器
 *
 * @author F1-messageTool
 * @since 2026-06-03
 */
@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    @GetMapping
    public Result<List<Note>> queryNoteList(
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String raceName,
            @RequestParam(required = false) String driverName) {
        return Result.success(noteService.queryNoteList(tag, raceName, driverName));
    }

    @GetMapping("/{noteId}")
    public Result<Note> queryNoteById(@PathVariable Long noteId) {
        return Result.success(noteService.queryNoteById(noteId));
    }

    @PostMapping
    public Result<Note> createNote(@RequestBody NoteCreateRequest request) {
        return Result.success(noteService.createNote(request));
    }

    @PutMapping("/{noteId}")
    public Result<Note> updateNote(@PathVariable Long noteId, @RequestBody NoteUpdateRequest request) {
        return Result.success(noteService.updateNote(noteId, request));
    }

    @DeleteMapping("/{noteId}")
    public Result<Void> deleteNote(@PathVariable Long noteId) {
        noteService.deleteNote(noteId);
        return Result.success();
    }
}
