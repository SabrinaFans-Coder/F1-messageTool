import { useNavigate } from 'react-router-dom';
import { resolveTeamColour } from '../../lib/teamColours';
import { useSeasonStandings } from '../../hooks/useSeasonStandings';
import TeamLogo from '../../components/TeamLogo/TeamLogo';
import LoadingState from '../../components/LoadingState';
import ErrorState from '../../components/ErrorState';
import styles from './Teams.module.css';



export default function Teams() {
  const navigate = useNavigate();
  const { standings, loading, error } = useSeasonStandings(2026);

  if (loading) return <LoadingState label="Teams" context="standings" />;
  if (error) return <ErrorState message={error} />;
  if (!standings?.constructorRankings?.length) {
    return <ErrorState message="No team data available" />;
  }

  return (
    <>
      <div className={styles.header}>
        <div className={styles.title}>Constructors</div>
        <div className={styles.subtitle}>{standings.constructorRankings.length} teams &middot; {standings.year} season</div>
      </div>

      <div className={styles.grid}>
        {standings.constructorRankings.map((team) => {
          const colour = resolveTeamColour(team.team_name, team.team_colour);
          return (
            <div
              key={team.team_name}
              className={styles.card}
              style={{ ['--teamColor' as string]: colour }}
              onClick={() => navigate(`/teams/${encodeURIComponent(team.team_name)}`)}
              role="button"
              tabIndex={0}
              onKeyDown={(e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                  e.preventDefault();
                  navigate(`/teams/${encodeURIComponent(team.team_name)}`);
                }
              }}
            >
              <span className={styles.accentBar} style={{ background: colour }} />
              <span className={styles.position}>P{team.position}</span>
              <TeamLogo teamName={team.team_name} colour={colour} size={52} />
              <div className={styles.info}>
                <div className={styles.name}>{team.team_name}</div>
                <div className={styles.statsLine}>
                  {team.wins} wins &middot; {team.podiums ?? 0} podiums &middot; {team.races ?? '-'} races
                </div>
              </div>
              <div className={styles.points}>
                <div className={styles.pointsValue} style={{ color: colour }}>{team.totalPoints}</div>
                <div className={styles.pointsLabel}>PTS</div>
              </div>
            </div>
          );
        })}
      </div>
    </>
  );
}
