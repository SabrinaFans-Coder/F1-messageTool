import { useState } from 'react';
import type { CircuitYearResult, RaceResultEntry } from '../types';

interface Props {
  years: CircuitYearResult[];
  sprintResults?: RaceResultEntry[];
}

export default function SessionResults({ years, sprintResults }: Props) {
  const [selectedYear, setSelectedYear] = useState<number | null>(
    years.length > 0 ? years[0].year : null
  );

  if (!years.length) return null;

  const currentYear = years.find((y) => y.year === selectedYear);

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
            marginBottom: 12,
          }}
        >
          Circuit History
        </div>
        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
          {years.map((y) => (
            <button
              key={y.year}
              onClick={() => setSelectedYear(y.year)}
              style={{
                fontFamily: "'Fira Code', monospace",
                fontSize: 13,
                fontWeight: 600,
                padding: '6px 14px',
                borderRadius: 6,
                border: '1px solid',
                borderColor: selectedYear === y.year ? 'var(--accent)' : 'var(--border)',
                background: selectedYear === y.year ? 'var(--accent)' : 'transparent',
                color: selectedYear === y.year ? '#000' : 'var(--text)',
                cursor: 'pointer',
                transition: 'all 150ms',
              }}
            >
              {y.year}
            </button>
          ))}
        </div>
      </div>

      {currentYear && (
        <div style={{ padding: '0 20px 16px' }}>
          <div
            style={{
              fontSize: 12,
              color: 'var(--text-muted)',
              padding: '10px 0',
              borderBottom: '1px solid var(--border)',
            }}
          >
            Race &middot;{' '}
            {new Date(currentYear.date_start).toLocaleDateString('en-US', {
              month: 'long',
              day: 'numeric',
              year: 'numeric',
            })}
          </div>
          <div style={{ overflowX: 'auto', WebkitOverflowScrolling: 'touch' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', minWidth: 400 }}>
            <tbody>
              {currentYear.results.map((entry: RaceResultEntry) => {
                const teamColor = entry.team_colour ? `#${entry.team_colour}` : 'var(--border)';
                const isPodium = entry.position <= 3;
                return (
                  <tr
                    key={entry.driver_number}
                    style={{
                      background: 'transparent',
                      transition: 'background 150ms',
                    }}
                    onMouseEnter={(e) => {
                      e.currentTarget.style.background = 'var(--card-hover)';
                    }}
                    onMouseLeave={(e) => {
                      e.currentTarget.style.background = 'transparent';
                    }}
                  >
                    <td
                      style={{
                        padding: '10px 0',
                        width: 36,
                        fontFamily: "'Fira Code', monospace",
                        fontWeight: 700,
                        color: isPodium ? 'var(--accent)' : 'var(--text-muted)',
                      }}
                    >
                      P{entry.position}
                    </td>
                    <td style={{ padding: '10px 8px' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                        <div style={{ width: 3, height: 20, borderRadius: 2, background: teamColor }} />
                        <span style={{ fontWeight: 600, fontSize: 14 }}>{entry.name_acronym}</span>
                      </div>
                    </td>
                    <td style={{ padding: '10px 8px', fontSize: 13, color: 'var(--text-muted)' }}>
                      {entry.driver_name}
                    </td>
                    <td
                      style={{
                        padding: '10px 0',
                        textAlign: 'right',
                        fontSize: 12,
                        color: 'var(--text-muted)',
                      }}
                    >
                      {entry.team_name}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
          </div>
        </div>
      )}

      {sprintResults && sprintResults.length > 0 && (
        <div style={{ padding: '0 20px 16px', borderTop: '1px solid var(--border)' }}>
          <div
            style={{
              fontFamily: "'Fira Code', monospace",
              fontSize: 11,
              fontWeight: 600,
              color: '#F59E0B',
              textTransform: 'uppercase',
              letterSpacing: 1.5,
              padding: '10px 0',
              borderBottom: '1px solid var(--border)',
            }}
          >
            Sprint
          </div>
          <div style={{ overflowX: 'auto', WebkitOverflowScrolling: 'touch' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', minWidth: 400 }}>
            <tbody>
              {sprintResults.map((entry: RaceResultEntry) => {
                const teamColor = entry.team_colour ? `#${entry.team_colour}` : 'var(--border)';
                const isPodium = entry.position <= 3;
                return (
                  <tr
                    key={entry.driver_number}
                    style={{
                      background: 'transparent',
                      transition: 'background 150ms',
                    }}
                    onMouseEnter={(e) => {
                      e.currentTarget.style.background = 'var(--card-hover)';
                    }}
                    onMouseLeave={(e) => {
                      e.currentTarget.style.background = 'transparent';
                    }}
                  >
                    <td
                      style={{
                        padding: '10px 0',
                        width: 36,
                        fontFamily: "'Fira Code', monospace",
                        fontWeight: 700,
                        color: isPodium ? '#F59E0B' : 'var(--text-muted)',
                      }}
                    >
                      P{entry.position}
                    </td>
                    <td style={{ padding: '10px 8px' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                        <div style={{ width: 3, height: 20, borderRadius: 2, background: teamColor }} />
                        <span style={{ fontWeight: 600, fontSize: 14 }}>{entry.name_acronym}</span>
                      </div>
                    </td>
                    <td style={{ padding: '10px 8px', fontSize: 13, color: 'var(--text-muted)' }}>
                      {entry.driver_name}
                    </td>
                    <td
                      style={{
                        padding: '10px 0',
                        textAlign: 'right',
                        fontSize: 12,
                        color: 'var(--text-muted)',
                      }}
                    >
                      {entry.team_name}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
          </div>
        </div>
      )}
    </div>
  );
}
