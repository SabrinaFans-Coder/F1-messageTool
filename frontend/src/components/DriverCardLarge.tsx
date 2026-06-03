import { useNavigate } from 'react-router-dom';
import type { Driver } from '../types';

interface Props {
  driver: Driver;
}

export default function DriverCardLarge({ driver }: Props) {
  const navigate = useNavigate();
  const teamColor = driver.team_colour ? `#${driver.team_colour}` : 'var(--border)';

  return (
    <div
      onClick={() => navigate(`/drivers/${driver.driver_number}`)}
      style={{
        background: 'var(--surface)',
        border: '1px solid var(--border)',
        borderRadius: 12,
        padding: 20,
        cursor: 'pointer',
        transition: 'border-color 150ms',
        display: 'flex',
        alignItems: 'center',
        gap: 16,
      }}
      onMouseEnter={(e) => {
        e.currentTarget.style.borderColor = teamColor;
      }}
      onMouseLeave={(e) => {
        e.currentTarget.style.borderColor = 'var(--border)';
      }}
    >
      <div style={{ width: 3, height: 60, borderRadius: 2, background: teamColor }} />
      {driver.headshot_url ? (
        <img
          src={driver.headshot_url}
          alt={driver.full_name}
          style={{
            width: 80,
            height: 80,
            borderRadius: 12,
            objectFit: 'cover',
            background: 'var(--surface-alt)',
          }}
        />
      ) : (
        <div
          style={{
            width: 80,
            height: 80,
            borderRadius: 12,
            background: 'var(--surface-alt)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontFamily: "'Fira Code', monospace",
            fontSize: 24,
            fontWeight: 700,
            color: teamColor,
          }}
        >
          #{driver.driver_number}
        </div>
      )}
      <div>
        <div
          style={{
            fontFamily: "'Fira Code', monospace",
            fontSize: 20,
            fontWeight: 700,
          }}
        >
          {driver.name_acronym}
        </div>
        <div style={{ fontSize: 14, color: 'var(--text-muted)', marginTop: 2 }}>
          {driver.full_name}
        </div>
        <div
          style={{
            fontFamily: "'Fira Code', monospace",
            fontSize: 12,
            color: teamColor,
            marginTop: 4,
          }}
        >
          #{driver.driver_number}
        </div>
      </div>
    </div>
  );
}
