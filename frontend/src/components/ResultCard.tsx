import type { Position, Driver } from '../types';

interface Props {
  position: Position;
  driver?: Driver;
}

export default function ResultCard({ position, driver }: Props) {
  const posColor = position.position === 1
    ? 'var(--accent)'
    : position.position === 2
    ? 'var(--text)'
    : 'var(--warning)';

  return (
    <div
      style={{
        background: 'var(--surface)',
        border: '1px solid var(--border)',
        borderRadius: 12,
        padding: 20,
        transition: 'all 200ms',
        cursor: 'pointer',
      }}
      onMouseEnter={(e) => {
        e.currentTarget.style.borderColor = 'var(--accent)';
      }}
      onMouseLeave={(e) => {
        e.currentTarget.style.borderColor = 'var(--border)';
      }}
    >
      <div
        style={{
          fontFamily: "'Fira Code', monospace",
          fontSize: 32,
          fontWeight: 700,
          color: posColor,
          lineHeight: 1,
          marginBottom: 8,
        }}
      >
        P{position.position}
      </div>
      <div style={{ fontSize: 16, fontWeight: 600, marginBottom: 2 }}>
        {driver?.full_name || `Driver #${position.driver_number}`}
      </div>
      <div style={{ fontSize: 13, color: 'var(--text-muted)' }}>
        {driver?.team_name || 'Unknown Team'}
      </div>
    </div>
  );
}
