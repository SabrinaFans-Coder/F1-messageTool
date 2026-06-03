import { useNavigate } from 'react-router-dom';
import { Plus, FileText } from 'lucide-react';
import { useNotes } from '../../hooks/useNotes';
import NoteCard from '../../components/NoteCard';
import LoadingState from '../../components/LoadingState';
import ErrorState from '../../components/ErrorState';
import type { Note } from '../../types';
import styles from './Notes.module.css';

export default function Notes() {
  const { notes, loading, error } = useNotes();
  const navigate = useNavigate();

  if (loading) return <LoadingState label="Notes" />;
  if (error) return <ErrorState message={error} />;

  const handleNoteClick = (note: Note) => {
    navigate(`/notes/${note.id}`);
  };

  return (
    <>
      <div className={styles.header}>
        <div>
          <div className={styles.title}>My Notes</div>
          <div className={styles.subtitle}>Race observations and thoughts</div>
        </div>
        <button className={`${styles.btn} ${styles.btnPrimary}`} onClick={() => navigate('/notes/new')}>
          <Plus size={16} />
          New Note
        </button>
      </div>

      {notes.length === 0 ? (
        <div className={styles.empty}>
          <FileText />
          <p>No notes yet</p>
          <button className={`${styles.btn} ${styles.btnPrimary}`} onClick={() => navigate('/notes/new')}>
            Create your first note
          </button>
        </div>
      ) : (
        <div className={styles.grid}>
          {notes.map((note) => (
            <NoteCard key={note.id} note={note} onClick={handleNoteClick} />
          ))}
        </div>
      )}
    </>
  );
}
