import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import { useDriverDetailV2 } from '../../hooks/useDriverDetailV2';
import LoadingState from '../../components/LoadingState';
import ErrorState from '../../components/ErrorState';
import styles from './DriverDetail.module.css';

const YEARS = [2026, 2025, 2024, 2023];

export default function DriverDetail() {
  const { driverNumber } = useParams<{ driverNumber: string }>();
  const navigate = useNavigate();
  const [selectedYear, setSelectedYear] = useState(2026);

  const num = driverNumber ? Number(driverNumber) : null;
  const { detail, loading, error } = useDriverDetailV2(num, selectedYear);

  if (loading) return <LoadingState label="Driver Profile" context="drivers" />;
  if (error) return <ErrorState message={error} />;
  if (!detail) return <ErrorState message="Driver not found" />;

  const teamColor = detail.team_colour ? `#${detail.team_colour}` : 'var(--border)';

  return (
    <>
      <button className={styles.backLink} onClick={() => navigate(-1)}>
        <ArrowLeft size={16} />
        Back
      </button>

      <div className={styles.profile}>
        {detail.headshot_url ? (
          <img
            src={detail.headshot_url}
            alt={detail.full_name}
            className={styles.headshot}
          />
        ) : (
          <div className={styles.headshotPlaceholder} style={{ color: teamColor }}>
            #{detail.driver_number}
          </div>
        )}
        <div>
          <div className={styles.driverName}>
            {detail.name_acronym}
          </div>
          <div className={styles.driverInfo}>
            {detail.full_name} · {detail.team_name}
          </div>
          <div
            style={{
              fontFamily: "'Fira Code', monospace",
              fontSize: 14,
              color: teamColor,
              marginTop: 4,
            }}
          >
            #{detail.driver_number}
          </div>
        </div>
      </div>

      <div className={styles.yearTabs}>
        {YEARS.map((y) => (
          <button
            key={y}
            className={`${styles.yearTab} ${selectedYear === y ? styles.yearTabActive : ''}`}
            onClick={() => setSelectedYear(y)}
          >
            {y}
          </button>
        ))}
      </div>

      <div className={styles.sectionLabel}>Season Stats</div>
      <div className={styles.statsGrid}>
        <div className={styles.statCard}>
          <div className={styles.statValue}>{detail.seasonStats.totalPoints}</div>
          <div className={styles.statLabel}>Points</div>
        </div>
        <div className={styles.statCard}>
          <div className={styles.statValue}>{detail.seasonStats.wins}</div>
          <div className={styles.statLabel}>Wins</div>
        </div>
        <div className={styles.statCard}>
          <div className={styles.statValue}>{detail.seasonStats.podiums}</div>
          <div className={styles.statLabel}>Podiums</div>
        </div>
        <div className={styles.statCard}>
          <div className={styles.statValue}>{detail.seasonStats.races}</div>
          <div className={styles.statLabel}>Races</div>
        </div>
        <div className={styles.statCard}>
          <div className={styles.statValue}>#{detail.seasonStats.rank}</div>
          <div className={styles.statLabel}>Rank</div>
        </div>
      </div>

      <div className={styles.sectionLabel}>Race Results</div>
      <div className={styles.raceTable}>
        {detail.raceResults.map((result) => (
          <div key={result.meeting_name} className={styles.raceRow}>
            <div
              className={`${styles.racePos} ${result.position <= 3 ? styles.racePosPodium : ''}`}
            >
              P{result.position}
            </div>
            <div className={styles.raceName}>{result.meeting_name}</div>
          </div>
        ))}
        {detail.raceResults.length === 0 && (
          <div style={{ padding: '20px', textAlign: 'center', color: 'var(--text-muted)' }}>
            No race results for this season
          </div>
        )}
      </div>
    </>
  );
}
