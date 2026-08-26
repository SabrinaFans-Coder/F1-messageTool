# F1 MessageTool

A personal Formula 1 companion web app — a single-page F1 information hub for race calendars, live results, driver & team profiles, standings, and latest news.

> 定位：Personal F1 Dashboard / Information Center。不做社区、不发帖，只做信息获取。

## Features

- **Home** — Next race countdown, latest race top 10, driver/constructor standings, latest news
- **News** — Auto-synced F1 headlines from Autosport / Motorsport.com / BBC Sport (RSS), category filter, source links
- **Race Calendar** — Full season schedule with circuit maps and per-race hub
- **Live Results** — Latest session results and season standings (Driver & Constructor)
- **Driver Profiles** — Season stats, headshots, race-by-race results (newest first)
- **Team Pages** — Team-colour themed pages with stats and driver lineup
- **Dark / Light Theme**

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Spring Boot 3.5, Java 17, JPA, WebClient, Spring Scheduling |
| Frontend | React 18, TypeScript, Vite, CSS Modules, self-hosted fonts |
| Database | PostgreSQL 16 (pgvector-ready) |
| Deployment | Docker Compose, Nginx |

## Data Sources

| Source | Usage |
|--------|-------|
| [OpenF1 API](https://openf1.org) | Meetings, sessions, circuit images, driver headshots, per-race positions |
| [Jolpica API](https://api.jolpi.ca) | Official standings, wins, points (Ergast successor) |
| RSS Feeds | Autosport F1 / Motorsport.com F1 / BBC Sport F1 → local `news` table |

News syncs at startup and every 30 minutes (deduplicated by source + URL). Podium counts are computed from OpenF1 per-race data since official standings don't provide them.

## Quick Start

### Prerequisites

- Docker & Docker Compose

### Deploy

```bash
# Clone
git clone https://github.com/SabrinaFans-Coder/F1-messageTool.git
cd F1-messageTool

# Set database credentials (.env in project root)
echo "DB_PASSWORD=your_password" > .env

# Build and start
docker-compose up -d --build
```

Frontend: `http://localhost:3000`
Backend API: `http://localhost:8180/api/f1/` · `/api/news`

> PostgreSQL is exposed on host port **5433** (container-internal 5432) to avoid clashing with a local instance.

### Update

```bash
git pull
docker-compose up -d --build
```

### Local Development (without Docker for apps)

```bash
# 1. Start only the database
docker-compose up -d postgres

# 2. Backend (needs DB_PASSWORD env var)
cd backend && ./mvnw spring-boot:run   # Windows: .\mvnw.cmd spring-boot:run

# 3. Frontend (dev server proxies /api -> localhost:8180)
cd frontend && npm install && npm run dev
```

## Project Structure

```
F1-messageTool/
├── backend/
│   └── src/main/java/com/springboot/backend/
│       ├── controller/    # REST endpoints (F1 proxy, news, favorites)
│       ├── service/       # OpenF1 / Jolpica integration, news query
│       ├── sync/          # NewsProvider (RSS), sync scheduler, stats warmup
│       ├── entity/        # JPA entities (news, favorite_drivers)
│       └── repository/    # Data access
├── frontend/
│   └── src/
│       ├── pages/         # Home, Calendar, News, Results, Drivers, Teams
│       ├── components/    # Shared UI components
│       ├── hooks/         # Data fetching hooks
│       └── assets/teams/  # Self-hosted team logos (fallback: monogram)
├── docs/                  # Product spec & AI coding rules
└── docker-compose.yml
```

## API Endpoints

| Endpoint | Description |
|----------|-------------|
| `GET /api/f1/meetings?year=` | Season calendar |
| `GET /api/f1/sessions/latest` | Latest session |
| `GET /api/f1/season-standings?year=` | Driver & constructor standings (official points/wins) |
| `GET /api/f1/season-rankings?year=&limit=` | Per-race aggregated stats incl. podiums |
| `GET /api/f1/drivers-list?year=` | All drivers with headshots |
| `GET /api/f1/driver-detail-v2?driverNumber=&year=` | Driver profile & race-by-race results |
| `GET /api/f1/circuit-history?meetingKey=` | Circuit historical results |
| `GET /api/news?category=&page=&size=` | Synced news (paged, category filter) |
| `GET /api/favorites` | Favorite drivers |

## License

MIT
