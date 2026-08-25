import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useMeetings } from '../../hooks/useMeetings';
import { useResults } from '../../hooks/useResults';
import { useDrivers } from '../../hooks/useDrivers';
import { useSeasonStandings } from '../../hooks/useSeasonStandings';
import { useSeasonRankings } from '../../hooks/useSeasonRankings';
import Countdown from '../../components/Countdown';
import RankingTable from '../../components/RankingTable';
import type { NewsPage, DriverRanking } from '../../types';
import { fetchJson } from '../../api/client';
import styles from './Home.module.css';

const CURRENT_YEAR = 2026;
const FULL_SEASON_LIMIT = 30;

function formatDate(iso: string): string {
  return new Date(iso).toLocaleString(undefined, {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

function NextRaceSection() {
  const navigate = useNavigate();
  const { meetings } = useMeetings(CURRENT_YEAR);
  if (!meetings.length) return null;

  const nextRace = meetings.find((m) => new Date(m.date_start) > new Date());
  if (!nextRace) return null;

  return (
    <>
      <div className={styles.sectionLabel}>Next Grand Prix</div>
      <div className={styles.hero}>
        <div className={styles.heroInfo}>
          {nextRace.circuit_image && (
            <img className={styles.heroImage} src={nextRace.circuit_image} alt={nextRace.circuit_short_name} />
          )}
          <div>
            <div className={styles.heroName}>{nextRace.meeting_name}</div>
            <div className={styles.heroMeta}>
              {nextRace.circuit_short_name} &middot; {nextRace.location}, {nextRace.country_name}
              {' · '}
              {new Date(nextRace.date_start).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })}
              {' - '}
              {new Date(nextRace.date_end).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })}
            </div>
          </div>
        </div>
        <Countdown targetDate={nextRace.date_start} />
        <button className={styles.badge} onClick={() => navigate(`/calendar/${nextRace.meeting_key}`)}>
          <span className={styles.dot} />
          RACE HUB
        </button>
      </div>
    </>
  );
}

interface StatsProps {
  statsMap: Map<number, DriverRanking>;
}

function LatestRaceSection({ statsMap }: StatsProps) {
  const navigate = useNavigate();
  const { positions, latestSession } = useResults();
  const { drivers } = useDrivers(latestSession?.session_key ?? null);

  if (!positions.length) return null;
  const driverMap = new Map(drivers.map((d) => [d.driver_number, d]));

  return (
    <div>
      <div className={styles.sectionLabel}>Latest Race</div>
      <RankingTable
        title={latestSession ? latestSession.session_name : 'Latest Results'}
        subtitle="Wins / Podiums / Points are season totals"
        rankings={positions.slice(0, 10).map((pos) => {
          const driver = driverMap.get(pos.driver_number);
          const stats = statsMap.get(pos.driver_number);
          return {
            driver_number: pos.driver_number,
            driver_name: driver?.full_name ?? `Driver ${pos.driver_number}`,
            name_acronym: driver?.name_acronym ?? '---',
            team_name: driver?.team_name ?? '',
            team_colour: driver?.team_colour ?? '#666',
            totalPoints: stats?.totalPoints ?? 0,
            wins: stats?.wins ?? 0,
            podiums: stats?.podiums ?? 0,
            position: pos.position,
          };
        })}
        onDriverClick={(driverNumber) => navigate(`/drivers/${driverNumber}`)}
      />
    </div>
  );
}

function StandingsSection({ statsMap }: StatsProps) {
  const navigate = useNavigate();
  const { standings } = useSeasonStandings(CURRENT_YEAR);
  if (!standings) return null;

  const driversWithStats = (standings.driverRankings ?? []).map((r) => ({
    ...r,
    podiums: statsMap.get(r.driver_number)?.podiums ?? 0,
  }));
  const podiumsByTeam = new Map<string, number>();
  for (const r of driversWithStats) {
    podiumsByTeam.set(r.team_name, (podiumsByTeam.get(r.team_name) ?? 0) + r.podiums);
  }

  const constructors: DriverRanking[] = (standings.constructorRankings ?? []).slice(0, 5).map((r) => ({
    driver_number: 0,
    driver_name: r.team_name,
    name_acronym: r.team_name.substring(0, 3).toUpperCase(),
    team_name: r.team_name,
    team_colour: r.team_colour,
    totalPoints: r.totalPoints,
    wins: r.wins,
    podiums: podiumsByTeam.get(r.team_name) ?? 0,
    races: r.races,
    position: r.position,
  }));

  return (
    <div>
      <div className={styles.sectionLabel}>Championship</div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        <RankingTable
          title="Drivers Top 5"
          rankings={driversWithStats.slice(0, 5)}
          onDriverClick={(driverNumber) => navigate(`/drivers/${driverNumber}`)}
        />
        <RankingTable title="Constructors Top 5" rankings={constructors} />
      </div>
    </div>
  );
}

function NewsSection() {
  const [news, setNews] = useState<NewsPage | null>(null);

  useEffect(() => {
    let cancelled = false;
    fetchJson<NewsPage>('/news?size=4')
      .then((data) => {
        if (!cancelled) setNews(data);
      })
      .catch(() => {});
    return () => {
      cancelled = true;
    };
  }, []);

  if (!news || !news.content.length) return null;

  return (
    <>
      <div className={styles.sectionLabel}>Latest News</div>
      <div className={styles.newsList}>
        {news.content.map((entry) => (
          <article
            key={entry.id}
            className={styles.newsCard}
            onClick={() => window.open(entry.sourceUrl, '_blank', 'noopener,noreferrer')}
          >
            <div className={styles.newsMeta}>
              <span className={styles.newsCategory}>{entry.category}</span>
              <span>{entry.source}</span>
              <span>{formatDate(entry.publishedAt)}</span>
            </div>
            <h3 className={styles.newsTitle}>{entry.title}</h3>
          </article>
        ))}
      </div>
    </>
  );
}

export default function Home() {
  const { standings } = useSeasonStandings(CURRENT_YEAR);
  const { rankings } = useSeasonRankings(CURRENT_YEAR, FULL_SEASON_LIMIT);

  // 统一口径：Points / Wins 以官方 Jolpica 积分榜为准，
  // 仅 Podiums（官方不提供）取自 OpenF1 逐场统计
  const statsMap = new Map<number, DriverRanking>();
  for (const r of standings?.driverRankings ?? []) {
    statsMap.set(r.driver_number, { ...r });
  }
  for (const r of rankings?.rankings ?? []) {
    const existing = statsMap.get(r.driver_number);
    if (existing) {
      existing.podiums = r.podiums;
      existing.races = r.races || existing.races;
    } else {
      statsMap.set(r.driver_number, r);
    }
  }

  return (
    <>
      <NextRaceSection />
      <div className={styles.midGrid}>
        <LatestRaceSection statsMap={statsMap} />
        <StandingsSection statsMap={statsMap} />
      </div>
      <NewsSection />
    </>
  );
}
