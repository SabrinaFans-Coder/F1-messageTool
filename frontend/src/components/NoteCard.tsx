import type { Note } from '../types';

interface Props {
  note: Note;
  onClick: (note: Note) => void;
}

export default function NoteCard({ note, onClick }: Props) {
  const date = new Date(note.createdAt).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });

  return (
    <div
      style={{
        background: 'var(--surface)',
        border: '1px solid var(--border)',
        borderRadius: 12,
        padding: 20,
        cursor: 'pointer',
        transition: 'all 200ms',
      }}
      onClick={() => onClick(note)}
      onMouseEnter={(e) => {
        e.currentTarget.style.borderColor = 'var(--accent)';
        e.currentTarget.style.background = 'var(--card-hover)';
      }}
      onMouseLeave={(e) => {
        e.currentTarget.style.borderColor = 'var(--border)';
        e.currentTarget.style.background = 'var(--surface)';
      }}
    >
      <span
        style={{
          fontSize: 11,
          fontWeight: 600,
          textTransform: 'uppercase',
          letterSpacing: 0.5,
          color: 'var(--accent)',
          background: 'var(--accent-bg)',
          padding: '2px 8px',
          borderRadius: 4,
          display: 'inline-block',
          marginBottom: 8,
        }}
      >
        {note.tag}
      </span>
      <div style={{ fontSize: 15, fontWeight: 600, marginBottom: 6 }}>{note.title}</div>
      <div
        style={{
          fontSize: 13,
          color: 'var(--text-muted)',
          display: '-webkit-box',
          WebkitLineClamp: 2,
          WebkitBoxOrient: 'vertical',
          overflow: 'hidden',
          marginBottom: 12,
        }}
      >
        {note.content}
      </div>
      <div style={{ fontSize: 12, color: 'var(--text-muted)', display: 'flex', justifyContent: 'space-between' }}>
        <span>{date}</span>
        <span>{note.raceName || note.driverName || ''}</span>
      </div>
    </div>
  );
}
