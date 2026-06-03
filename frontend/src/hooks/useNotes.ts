import { useState, useEffect, useCallback } from 'react';
import type { Note, NoteCreateRequest, NoteUpdateRequest } from '../types';
import { fetchJson, postJson, putJson, deleteJson } from '../api/client';

export function useNotes() {
  const [notes, setNotes] = useState<Note[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const refreshNotes = useCallback(() => {
    setLoading(true);
    fetchJson<Note[]>('/notes')
      .then((data) => {
        setNotes(data);
        setError(null);
      })
      .catch((err) => {
        setError(err instanceof Error ? err.message : '加载笔记失败');
      })
      .finally(() => {
        setLoading(false);
      });
  }, []);

  useEffect(() => {
    refreshNotes();
  }, [refreshNotes]);

  const createNote = useCallback(async (request: NoteCreateRequest) => {
    const note = await postJson<Note>('/notes', request);
    setNotes((prev) => [note, ...prev]);
    return note;
  }, []);

  const updateNote = useCallback(async (id: number, request: NoteUpdateRequest) => {
    const note = await putJson<Note>(`/notes/${id}`, request);
    setNotes((prev) => prev.map((n) => (n.id === id ? note : n)));
    return note;
  }, []);

  const deleteNote = useCallback(async (id: number) => {
    await deleteJson(`/notes/${id}`);
    setNotes((prev) => prev.filter((n) => n.id !== id));
  }, []);

  return { notes, loading, error, createNote, updateNote, deleteNote, refreshNotes };
}

export function useNote(id: number | null) {
  const [note, setNote] = useState<Note | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!id) {
      setLoading(false);
      return;
    }

    let cancelled = false;
    setLoading(true);

    fetchJson<Note>(`/notes/${id}`)
      .then((data) => {
        if (!cancelled) {
          setNote(data);
          setError(null);
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : '加载笔记失败');
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [id]);

  return { note, loading, error };
}
