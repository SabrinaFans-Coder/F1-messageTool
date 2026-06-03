import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Save, Trash2 } from 'lucide-react';
import { useNotes, useNote } from '../../hooks/useNotes';
import LoadingState from '../../components/LoadingState';
import styles from './NoteEditor.module.css';

export default function NoteEditor() {
  const { id } = useParams<{ id: string }>();
  const noteId = id ? Number(id) : null;
  const isEdit = noteId !== null;

  const navigate = useNavigate();
  const { createNote, updateNote, deleteNote } = useNotes();
  const { note, loading } = useNote(noteId);

  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [tag, setTag] = useState('race');
  const [raceName, setRaceName] = useState('');
  const [driverName, setDriverName] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (note) {
      setTitle(note.title);
      setContent(note.content);
      setTag(note.tag);
      setRaceName(note.raceName || '');
      setDriverName(note.driverName || '');
    }
  }, [note]);

  if (loading) return <LoadingState label="Note" />;

  const handleSave = async () => {
    if (!title.trim() || !content.trim()) return;
    setSaving(true);
    try {
      if (isEdit) {
        await updateNote(noteId, { title, content, tag, raceName: raceName || undefined, driverName: driverName || undefined });
      } else {
        await createNote({ title, content, tag, raceName: raceName || undefined, driverName: driverName || undefined });
      }
      navigate('/notes');
    } catch {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!isEdit) return;
    await deleteNote(noteId);
    navigate('/notes');
  };

  return (
    <div className={styles.container}>
      <div className={styles.formGroup}>
        <label className={styles.label}>Title</label>
        <input
          className={styles.input}
          type="text"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="e.g. Monaco GP - Strategy Thoughts"
        />
      </div>

      <div className={styles.row}>
        <div className={styles.formGroup}>
          <label className={styles.label}>Tag</label>
          <select className={styles.select} value={tag} onChange={(e) => setTag(e.target.value)}>
            <option value="race">Race</option>
            <option value="driver">Driver</option>
            <option value="general">General</option>
          </select>
        </div>
        <div className={styles.formGroup}>
          <label className={styles.label}>Linked Race (optional)</label>
          <input
            className={styles.input}
            type="text"
            value={raceName}
            onChange={(e) => setRaceName(e.target.value)}
            placeholder="e.g. Monaco Grand Prix"
          />
        </div>
      </div>

      <div className={styles.formGroup}>
        <label className={styles.label}>Driver (optional)</label>
        <input
          className={styles.input}
          type="text"
          value={driverName}
          onChange={(e) => setDriverName(e.target.value)}
          placeholder="e.g. Lando Norris"
        />
      </div>

      <div className={styles.formGroup}>
        <label className={styles.label}>Content</label>
        <textarea
          className={styles.textarea}
          value={content}
          onChange={(e) => setContent(e.target.value)}
          placeholder="Write your race observations..."
        />
      </div>

      <div className={styles.actions}>
        {isEdit && (
          <button className={`${styles.btn} ${styles.btnDanger}`} onClick={handleDelete}>
            <Trash2 size={16} />
            Delete
          </button>
        )}
        <button className={`${styles.btn} ${styles.btnSecondary}`} onClick={() => navigate('/notes')}>
          Cancel
        </button>
        <button
          className={`${styles.btn} ${styles.btnPrimary}`}
          onClick={handleSave}
          disabled={saving || !title.trim() || !content.trim()}
        >
          <Save size={16} />
          {saving ? 'Saving...' : 'Save Note'}
        </button>
      </div>
    </div>
  );
}
