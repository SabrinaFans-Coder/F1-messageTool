import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import { useMeetings } from '../../hooks/useMeetings';
import { useCircuitHistory } from '../../hooks/useCircuitHistory';
import { useSeasonRankings } from '../../hooks/useSeasonRankings';
import SessionResults from '../../components/SessionResults';
import RankingTable from '../../components/RankingTable';
import LoadingState from '../../components/LoadingState';
import ErrorState from '../../components/ErrorState';
import styles from './RaceReport.module.css';

export default function RaceReport() {
  const { meetingKey } = useParams<{ meetingKey: string }>();
  const navigate = useNavigate();
  const { meetings, loading: meetingsLoading } = useMeetings(2026);

  const meeting = meetings.find((m) => m.meeting_key === Number(meetingKey));

  const { history, loading: historyLoading, error: historyError } = useCircuitHistory(
    meeting?.meeting_key ?? null
  );
  const { rankings: seasonRankings, loading: seasonLoading } = useSeasonRankings(2026, 5);

  if (meetingsLoading || historyLoading) {
    return <LoadingState label="Race Report" context="raceReport" />;
  }

  if (historyError) {
    return <ErrorState message={historyError} />;
  }

  if (!meeting) {
    return <ErrorState message="Meeting not found" />;
  }

  return (
    <div className={styles.container}>
      <button className={styles.backLink} onClick={() => navigate('/calendar')}>
        <ArrowLeft size={16} />
        Back to Calendar
      </button>

      <div className={styles.header}>
        {meeting.circuit_image && (
          <div className={styles.circuitImageWrap}>
            <img
              src={meeting.circuit_image}
              alt={meeting.circuit_short_name}
              className={styles.circuitImage}
            />
          </div>
        )}
        <div className={styles.meetingName}>{meeting.meeting_name}</div>
        <div className={styles.meetingInfo}>
          {meeting.circuit_short_name} &middot; {meeting.location}, {meeting.country_name}
        </div>
      </div>

      {history && (
        <div className={styles.section}>
          <SessionResults years={history.years} sprintResults={history.sprintResults} />
        </div>
      )}

      {history && history.circuitRanking.length > 0 && (
        <div className={styles.section}>
          <RankingTable
            title="Circuit Ranking"
            subtitle={`${history.circuit_short_name} · 2023-2026`}
            rankings={history.circuitRanking}
            showRaces
          />
        </div>
      )}

      {!seasonLoading && seasonRankings && (
        <div className={styles.section}>
          <RankingTable
            title="Season Recent Rankings"
            subtitle={`Last ${seasonRankings.racesCount} races · ${seasonRankings.year}`}
            rankings={seasonRankings.rankings}
          />
        </div>
      )}
    </div>
  );
}
