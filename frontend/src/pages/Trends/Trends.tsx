import { useState } from 'react';
import { LineChart, Line, XAxis, YAxis, Tooltip, Legend, ResponsiveContainer, CartesianGrid } from 'recharts';
import { usePointsTrend } from '../../hooks/usePointsTrend';
import LoadingState from '../../components/LoadingState';
import ErrorState from '../../components/ErrorState';
import styles from './Trends.module.css';

const TOP_OPTIONS = [5, 10, 20];

function normalizeColour(colour?: string): string {
  if (!colour) return '#94A3B8';
  return colour.startsWith('#') ? colour : `#${colour}`;
}

export default function Trends() {
  const [top, setTop] = useState(10);
  const { data, isPending, isError, error } = usePointsTrend(2026, top);

  if (isPending) return <LoadingState label="Standings Trends" context="standings" />;
  if (isError || !data || !data.drivers.length) {
    return <ErrorState message={error instanceof Error ? error.message : 'No trend data available'} />;
  }

  // Recharts 数据形状：每轮一行 { round, 'NOR': 43, 'VER': 39, ... }
  const chartData = data.rounds.map((_, i) => {
    const row: Record<string, string | number> = { round: `R${i + 1}` };
    for (const driver of data.drivers) {
      row[driver.name_acronym] = driver.points[i] ?? 0;
    }
    return row;
  });

  return (
    <>
      <div className={styles.header}>
        <div>
          <div className={styles.title}>Standings Trends</div>
          <div className={styles.subtitle}>
            Cumulative points per round &middot; {data.year} season
          </div>
        </div>
        <div className={styles.topTabs}>
          {TOP_OPTIONS.map((option) => (
            <button
              key={option}
              className={`${styles.topTab} ${top === option ? styles.topTabActive : ''}`}
              onClick={() => setTop(option)}
            >
              Top {option}
            </button>
          ))}
        </div>
      </div>

      <div className={styles.notice}>Estimated from race positions — sprint points not included</div>

      <div className={styles.chartCard}>
        <ResponsiveContainer width="100%" height={480}>
          <LineChart data={chartData} margin={{ top: 16, right: 24, bottom: 8, left: 0 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
            <XAxis dataKey="round" tick={{ fontSize: 12, fill: 'var(--text-muted)' }} stroke="var(--border)" />
            <YAxis tick={{ fontSize: 12, fill: 'var(--text-muted)' }} stroke="var(--border)" />
            <Tooltip
              contentStyle={{
                background: 'var(--surface)',
                border: '1px solid var(--border)',
                borderRadius: 8,
                fontSize: 13,
              }}
              labelStyle={{ color: 'var(--text)', fontWeight: 600 }}
            />
            <Legend wrapperStyle={{ fontSize: 13 }} />
            {data.drivers.map((driver) => (
              <Line
                key={driver.driver_number}
                type="monotone"
                dataKey={driver.name_acronym}
                stroke={normalizeColour(driver.team_colour)}
                strokeWidth={2}
                dot={false}
                activeDot={{ r: 4 }}
              />
            ))}
          </LineChart>
        </ResponsiveContainer>
      </div>
    </>
  );
}
