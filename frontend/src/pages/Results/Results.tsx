import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useSeasonStandings } from '../../hooks/useSeasonStandings';
import { useResults } from '../../hooks/useResults';
import { useDrivers } from '../../hooks/useDrivers';
import ResultCard from '../../components/ResultCard';
import RankingTable from '../../components/RankingTable';
import LoadingState from '../../components/LoadingState';
import ErrorState from '../../components/ErrorState';
import type { Driver, DriverRanking } from '../../types';
import styles from './Results.module.css';

const YEARS = [2026, 2025, 2024, 2023];

export default function Results() {
  const navigate = useNavigate();
  const [selectedYear, setSelectedYear] = useState(2026);

  const { positions, latestSession, loading: resultsLoading, error: resultsError } = useResults();
  const { drivers } = useDrivers(latestSession?.session_key ?? null);
  const { standings, loading: standingsLoading, error: standingsError } = useSeasonStandings(selectedYear);

  const driverMap = new Map<number, Driver>();
  drivers.forEach((d) => driverMap.set(d.driver_number, d));

  const top3 = positions.slice(0, 3);

  const driverStandings: DriverRanking[] = standings?.driverRankings ?? [];

  const constructorStandings: DriverRanking[] = (standings?.constructorRankings ?? []).map((r) => ({
    driver_number: 0,
    driver_name: r.team_name,
    name_acronym: r.team_name.substring(0, 3).toUpperCase(),
    team_name: r.team_name,
    team_colour: r.team_colour,
    totalPoints: r.totalPoints,
    wins: r.wins,
    podiums: r.podiums,
    races: r.races,
    position: r.position,
  }));

  if (resultsLoading) return <LoadingState label="Results" context="results" />;
  if (resultsError) return <ErrorState message={resultsError} />;

  return (
    <>
      <div className={styles.header}>
        <div className={styles.title}>Standings</div>
        <div className={styles.subtitle}>
          {latestSession
            ? `Latest: ${latestSession.session_name} · ${new Date(latestSession.date_start).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })}`
            : 'Season standings'}
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

      {top3.length > 0 && selectedYear === 2026 && (
        <>
          <div className={styles.sectionLabel}>Latest Race Podium</div>
          <div className={styles.topGrid}>
            {top3.map((pos) => (
              <ResultCard
                key={pos.driver_number}
                position={pos}
                driver={driverMap.get(pos.driver_number)}
              />
            ))}
          </div>
        </>
      )}

      {standingsError ? (
        <ErrorState message={standingsError} />
      ) : !standingsLoading && (
        <div className={styles.standingsGrid}>
          <RankingTable
            title="Driver Standings"
            subtitle={`${selectedYear} Season`}
            rankings={driverStandings}
            showRaces
            onDriverClick={(driverNumber) => navigate(`/drivers/${driverNumber}`)}
          />
          <RankingTable
            title="Constructor Standings"
            subtitle={`${selectedYear} Season`}
            rankings={constructorStandings}
            showRaces
          />
        </div>
      )}
    </>
  );
}
