import type { Driver } from '../types';

interface Props {
  driver: Driver;
}

export default function DriverCard({ driver }: Props) {
  const teamColor = driver.team_colour ? `#${driver.team_colour}` : 'var(--border)';

  return (
    <div
      style={{
        background: 'var(--surface)',
        border: '1px solid var(--border)',
        borderRadius: 12,
        padding: 20,
        cursor: 'pointer',
        transition: 'all 200ms',
        display: 'flex',
        alignItems: 'center',
        gap: 16,
      }}
      onMouseEnter={(e) => {
        e.currentTarget.style.borderColor = 'var(--accent)';
      }}
      onMouseLeave={(e) => {
        e.currentTarget.style.borderColor = 'var(--border)';
      }}
    >
      <div style={{ width: 4, height: 36, borderRadius: 2, background: teamColor }} />
      <div
        style={{
          fontFamily: "'Fira Code', monospace",
          fontSize: 28,
          fontWeight: 700,
          color: 'var(--accent)',
          minWidth: 48,
        }}
      >
        {driver.driver_number}
      </div>
      <div style={{ flex: 1 }}>
        <div style={{ fontSize: 15, fontWeight: 600 }}>{driver.full_name}</div>
        <div style={{ fontSize: 13, color: 'var(--text-muted)' }}>{driver.team_name}</div>
      </div>
    </div>
  );
}
