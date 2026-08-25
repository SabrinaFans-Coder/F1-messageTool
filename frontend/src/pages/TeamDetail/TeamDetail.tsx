import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import { useSeasonStandings } from '../../hooks/useSeasonStandings';
import { useSeasonRankings } from '../../hooks/useSeasonRankings';
import { useResults } from '../../hooks/useResults';
import { useDrivers } from '../../hooks/useDrivers';
import TeamLogo from '../../components/TeamLogo/TeamLogo';
import { resolveTeamColour } from '../../lib/teamColours';
import LoadingState from '../../components/LoadingState';
import ErrorState from '../../components/ErrorState';
import styles from './TeamDetail.module.css';



export default function TeamDetail() {
  const navigate = useNavigate();
  const { teamName } = useParams<{ teamName: string }>();
  const decodedName = decodeURIComponent(teamName ?? '');

  const { standings, loading, error } = useSeasonStandings(2026);
  const { rankings } = useSeasonRankings(2026, 30);
  const { latestSession } = useResults();
  const { drivers } = useDrivers(latestSession?.session_key ?? null);

  if (loading) return <LoadingState label="Team" context="standings" />;
  if (error) return <ErrorState message={error} />;
  if (!standings) return <ErrorState message="No standings data available" />;

  const team = standings.constructorRankings?.find((t) => t.team_name === decodedName);
  if (!team) return <ErrorState message={`Team not found: ${decodedName}`} />;

  const colour = resolveTeamColour(team.team_name, team.team_colour);
  const teamDrivers = (standings.driverRankings ?? []).filter((d) => d.team_name === decodedName);
  const podiumMap = new Map((rankings?.rankings ?? []).map((r) => [r.driver_number, r.podiums]));
  const teamPodiums = teamDrivers.reduce((sum, d) => sum + (podiumMap.get(d.driver_number) ?? 0), 0);

  const stats = [
    { label: 'Position', value: `P${team.position}` },
    { label: 'Points', value: team.totalPoints },
    { label: 'Wins', value: team.wins },
    { label: 'Podiums', value: teamPodiums },
  ];

  return (
    <div className={styles.page}>
      <button className={styles.backBtn} onClick={() => navigate('/teams')}>
        <ArrowLeft size={16} />
        All Teams
      </button>

      <div className={styles.hero} style={{ borderColor: `${colour}55`, background: `linear-gradient(135deg, ${colour}1A 0%, transparent 60%)` }}>
        <TeamLogo teamName={team.team_name} colour={colour} size={72} className={styles.monogram} />
        <div>
          <h1 className={styles.name}>{team.team_name}</h1>
          <div className={styles.sub}>{team.races ?? '-'} races &middot; {standings.year} season</div>
        </div>
      </div>

      <div className={styles.statsRow}>
        {stats.map((s) => (
          <div key={s.label} className={styles.statCard}>
            <div className={styles.statValue} style={s.label === 'Points' || s.label === 'Position' ? { color: colour } : undefined}>
              {s.value}
            </div>
            <div className={styles.statLabel}>{s.label}</div>
          </div>
        ))}
      </div>

      <div className={styles.sectionLabel}>Driver Lineup</div>
      <div className={styles.driverGrid}>
        {teamDrivers.map((d) => {
          const info = drivers.find((x) => x.driver_number === d.driver_number);
          return (
            <div
              key={d.driver_number}
              className={styles.driverCard}
              style={{ borderColor: d.driver_number ? `${colour}44` : undefined }}
              onClick={() => navigate(`/drivers/${d.driver_number}`)}
              role="button"
              tabIndex={0}
              onKeyDown={(e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                  e.preventDefault();
                  navigate(`/drivers/${d.driver_number}`);
                }
              }}
            >
              {info?.headshot_url && (
                <img className={styles.headshot} src={info.headshot_url} alt={info.full_name} loading="lazy" />
              )}
              <div className={styles.driverInfo}>
                <div className={styles.driverNumber} style={{ color: colour }}>#{d.driver_number}</div>
                <div className={styles.driverName}>{d.driver_name}</div>
                <div className={styles.driverStats}>
                  {d.totalPoints} PTS &middot; {d.wins} wins &middot; {podiumMap.get(d.driver_number) ?? 0} podiums
                </div>
              </div>
            </div>
          );
        })}
        {teamDrivers.length === 0 && (
          <div className={styles.noDrivers}>No driver data available for this team</div>
        )}
      </div>
    </div>
  );
}
