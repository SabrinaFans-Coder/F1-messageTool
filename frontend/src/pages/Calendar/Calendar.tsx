import { useMeetings } from '../../hooks/useMeetings';
import RaceCard from '../../components/RaceCard';
import Countdown from '../../components/Countdown';
import LoadingState from '../../components/LoadingState';
import ErrorState from '../../components/ErrorState';
import styles from './Calendar.module.css';

export default function Calendar() {
  const { meetings, loading, error } = useMeetings(2026);

  if (loading) return <LoadingState label="Race Calendar" context="calendar" />;
  if (error) return <ErrorState message={error} />;

  const now = new Date();
  const nextRace = meetings.find((m) => new Date(m.date_start) > now);

  return (
    <>
      {nextRace && (
        <>
          <div className={styles.sectionLabel}>Next Race</div>
          <div className={styles.countdownCard}>
            <div className={styles.countdownHeader}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
                {nextRace.circuit_image && (
                  <img
                    src={nextRace.circuit_image}
                    alt={nextRace.circuit_short_name}
                    style={{
                      width: 80,
                      height: 60,
                      objectFit: 'contain',
                      opacity: 0.8,
                      borderRadius: 8,
                      background: 'var(--surface-alt)',
                      padding: 4,
                    }}
                  />
                )}
                <div>
                  <div
                    style={{
                      fontFamily: "'Fira Code', monospace",
                      fontSize: 16,
                      fontWeight: 600,
                      marginBottom: 4,
                    }}
                  >
                    {nextRace.meeting_name}
                  </div>
                  <div style={{ fontSize: 13, color: 'var(--text-muted)' }}>
                    {nextRace.circuit_short_name} &middot; {nextRace.location}, {nextRace.country_name}
                  </div>
                </div>
              </div>
              <span className={styles.upcomingBadge}>
                <span className={styles.dot} />
                UPCOMING
              </span>
            </div>
            <Countdown targetDate={nextRace.date_start} />
          </div>
        </>
      )}

      <div className={styles.sectionLabel}>2026 Race Calendar</div>
      <div className={styles.header}>
        <div className={styles.title}>2026 Race Calendar</div>
        <div className={styles.subtitle}>{meetings.length} rounds across the globe</div>
      </div>
      <div className={styles.grid}>
        {meetings.map((meeting) => (
          <RaceCard key={meeting.meeting_key} meeting={meeting} />
        ))}
      </div>
    </>
  );
}
