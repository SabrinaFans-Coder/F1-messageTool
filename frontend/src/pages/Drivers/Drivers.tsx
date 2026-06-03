import { useDriversList } from '../../hooks/useDriversList';
import DriverCardLarge from '../../components/DriverCardLarge';
import LoadingState from '../../components/LoadingState';
import ErrorState from '../../components/ErrorState';
import type { Driver } from '../../types';
import styles from './Drivers.module.css';

export default function Drivers() {
  const { drivers, loading, error } = useDriversList(2026);

  if (loading) return <LoadingState label="All Drivers" context="drivers" />;
  if (error) return <ErrorState message={error} />;

  // Group drivers by team
  const teams = new Map<string, Driver[]>();
  for (const driver of drivers) {
    const list = teams.get(driver.team_name) ?? [];
    list.push(driver);
    teams.set(driver.team_name, list);
  }

  const sortedTeams = [...teams.entries()].sort(([a], [b]) => a.localeCompare(b));

  return (
    <>
      <div className={styles.header}>
        <div className={styles.title}>All Drivers</div>
        <div className={styles.subtitle}>{drivers.length} drivers on the grid</div>
      </div>

      {sortedTeams.map(([teamName, teamDrivers]) => {
        const teamColour = teamDrivers[0]?.team_colour
          ? `#${teamDrivers[0].team_colour}`
          : 'var(--border)';
        return (
          <div key={teamName} className={styles.teamSection}>
            <div className={styles.teamHeader}>
              <div className={styles.teamColorBar} style={{ background: teamColour }} />
              <div className={styles.teamName}>{teamName}</div>
            </div>
            <div className={styles.driverGrid}>
              {teamDrivers.map((driver) => (
                <DriverCardLarge key={driver.driver_number} driver={driver} />
              ))}
            </div>
          </div>
        );
      })}
    </>
  );
}
