export interface Result<T> {
  code: number;
  message: string;
  data: T;
}

export interface Meeting {
  meeting_key: number;
  meeting_name: string;
  meeting_official_name: string;
  location: string;
  country_code: string;
  country_name: string;
  country_flag: string;
  circuit_short_name: string;
  circuit_image: string;
  date_start: string;
  date_end: string;
  year: number;
  is_cancelled: boolean;
}

export interface Session {
  session_key: number;
  session_type: string;
  session_name: string;
  date_start: string;
  date_end: string;
  meeting_key: number;
  circuit_short_name: string;
  country_name: string;
  year: number;
}

export interface Driver {
  driver_number: number;
  full_name: string;
  name_acronym: string;
  team_name: string;
  team_colour: string;
  headshot_url: string;
}

export interface Position {
  driver_number: number;
  position: number;
  date: string;
  session_key: number;
}

export interface Note {
  id: number;
  title: string;
  content: string;
  tag: string;
  raceName: string | null;
  driverName: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface NoteCreateRequest {
  title: string;
  content: string;
  tag: string;
  raceName?: string;
  driverName?: string;
}

export interface NoteUpdateRequest {
  title?: string;
  content?: string;
  tag?: string;
  raceName?: string;
  driverName?: string;
}

export interface FavoriteDriver {
  id: number;
  driverNumber: number;
  driverName: string;
  teamName: string;
}

export interface CircuitHistory {
  circuit_short_name: string;
  country_name: string;
  circuit_image: string;
  years: CircuitYearResult[];
  sprintResults: RaceResultEntry[];
  circuitRanking: DriverRanking[];
}

export interface CircuitYearResult {
  year: number;
  session_key: number;
  date_start: string;
  results: RaceResultEntry[];
}

export interface RaceResultEntry {
  position: number;
  driver_number: number;
  driver_name: string;
  name_acronym: string;
  team_name: string;
  team_colour: string;
}

export interface SeasonRankings {
  year: number;
  racesCount: number;
  rankings: DriverRanking[];
}

export interface DriverRanking {
  driver_number: number;
  driver_name: string;
  name_acronym: string;
  team_name: string;
  team_colour?: string;
  totalPoints: number;
  wins: number;
  podiums: number;
  races?: number;
  position?: number;
}

export interface DriverDetail {
  driver_number: number;
  full_name: string;
  name_acronym: string;
  team_name: string;
  team_colour: string;
  headshot_url: string;
  country_code: string | null;
  seasonStats: SeasonStats;
  raceResults: DriverRaceResult[];
}

export interface SeasonStats {
  totalPoints: number;
  wins: number;
  podiums: number;
  races: number;
  rank: number;
}

export interface DriverRaceResult {
  meeting_name: string;
  circuit_short_name: string;
  position: number;
  session_key: number;
}

export interface ConstructorRanking {
  position: number;
  team_name: string;
  team_colour: string;
  totalPoints: number;
  wins: number;
  podiums: number;
  races: number;
}

export interface ConstructorRankings {
  year: number;
  rankings: ConstructorRanking[];
}

export interface SeasonStandings {
  year: number;
  racesCount: number;
  driverRankings: DriverRanking[];
  constructorRankings: ConstructorRanking[];
}
