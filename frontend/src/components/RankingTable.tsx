import type { DriverRanking } from '../types';
import { resolveTeamColour } from '../lib/teamColours';

interface Props {
  title: string;
  subtitle?: string;
  rankings: DriverRanking[];
  showRaces?: boolean;
  onDriverClick?: (driverNumber: number) => void;
}

export default function RankingTable({ title, subtitle, rankings, showRaces, onDriverClick }: Props) {
  if (!rankings.length) return null;

  return (
    <div
      style={{
        background: 'var(--surface)',
        border: '1px solid var(--border)',
        borderRadius: 12,
        overflow: 'hidden',
      }}
    >
      <div style={{ padding: '16px 20px', borderBottom: '1px solid var(--border)' }}>
        <div
          style={{
            fontFamily: "'Fira Code', monospace",
            fontSize: 11,
            fontWeight: 600,
            color: 'var(--accent)',
            textTransform: 'uppercase',
            letterSpacing: 1.5,
          }}
        >
          {title}
        </div>
        {subtitle && (
          <div style={{ fontSize: 13, color: 'var(--text-muted)', marginTop: 4 }}>
            {subtitle}
          </div>
        )}
      </div>
      <div style={{ overflowX: 'auto', WebkitOverflowScrolling: 'touch' }}>
      <table style={{ width: '100%', borderCollapse: 'collapse', minWidth: showRaces ? 500 : 400 }}>
        <thead>
          <tr
            style={{
              fontSize: 12,
              color: 'var(--text-muted)',
              textTransform: 'uppercase',
              letterSpacing: 0.5,
            }}
          >
            <th style={{ padding: '10px 16px', textAlign: 'left', width: 40 }}>#</th>
            <th style={{ padding: '10px 16px', textAlign: 'left' }}>Driver</th>
            <th style={{ padding: '10px 16px', textAlign: 'center' }}>Wins</th>
            <th style={{ padding: '10px 16px', textAlign: 'center' }}>Podiums</th>
            {showRaces && <th style={{ padding: '10px 16px', textAlign: 'center' }}>Races</th>}
            <th style={{ padding: '10px 16px', textAlign: 'right' }}>Points</th>
          </tr>
        </thead>
        <tbody>
          {rankings.map((r, i) => {
            const teamColor = resolveTeamColour(r.team_name, r.team_colour);
            return (
              <tr
                key={r.driver_number}
                style={{
                  background: i % 2 === 0 ? 'transparent' : 'var(--surface-alt)',
                  transition: 'background 150ms',
                  cursor: onDriverClick ? 'pointer' : 'default',
                }}
                onClick={() => onDriverClick?.(r.driver_number)}
                onMouseEnter={(e) => {
                  e.currentTarget.style.background = 'var(--card-hover)';
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.background = i % 2 === 0 ? 'transparent' : 'var(--surface-alt)';
                }}
              >
                <td
                  style={{
                    padding: '12px 16px',
                    fontFamily: "'Fira Code', monospace",
                    fontWeight: 700,
                    color: i < 3 ? 'var(--accent)' : 'var(--text-muted)',
                  }}
                >
                  {r.position || i + 1}
                </td>
                <td style={{ padding: '12px 16px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                    <div style={{ width: 3, height: 24, borderRadius: 2, background: teamColor }} />
                    <div>
                      <div style={{ fontWeight: 600, fontSize: 14 }}>{r.name_acronym}</div>
                      <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>{r.team_name}</div>
                    </div>
                  </div>
                </td>
                <td
                  style={{
                    padding: '12px 16px',
                    textAlign: 'center',
                    fontFamily: "'Fira Code', monospace",
                    fontWeight: 600,
                  }}
                >
                  {r.wins}
                </td>
                <td
                  style={{
                    padding: '12px 16px',
                    textAlign: 'center',
                    fontFamily: "'Fira Code', monospace",
                  }}
                >
                  {r.podiums}
                </td>
                {showRaces && (
                  <td
                    style={{
                      padding: '12px 16px',
                      textAlign: 'center',
                      fontFamily: "'Fira Code', monospace",
                      color: 'var(--text-muted)',
                    }}
                  >
                    {r.races}
                  </td>
                )}
                <td
                  style={{
                    padding: '12px 16px',
                    textAlign: 'right',
                    fontFamily: "'Fira Code', monospace",
                    fontWeight: 700,
                    color: 'var(--accent)',
                  }}
                >
                  {r.totalPoints}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
      </div>
    </div>
  );
}
