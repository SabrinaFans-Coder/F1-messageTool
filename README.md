# F1 MessageTool

A personal Formula 1 companion web app for tracking race calendars, standings, driver profiles, and personal notes.

## Features

- **Race Calendar** — View the full season schedule with circuit maps and countdown to next race
- **Live Results** — Latest race podium and season standings (Driver & Constructor)
- **Driver Profiles** — Detailed stats, headshots, and race results by year (2023-2026)
- **Circuit History** — Historical race results and circuit-specific rankings
- **Notes** — Personal race observations with tags (Race / Driver / General)
- **Dark / Light Theme** — Toggle between themes

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Spring Boot 3.5, Java 17, JPA, WebClient |
| Frontend | React 18, TypeScript, Vite, CSS Modules |
| Database | MySQL 8.0 |
| Deployment | Docker Compose, Nginx |

## Data Sources

| Source | Usage |
|--------|-------|
| [OpenF1 API](https://openf1.org) | Meetings, sessions, circuit images, driver headshots |
| [Jolpica API](https://api.jolpi.ca) | Standings, race results (Ergast successor) |

## Quick Start

### Prerequisites

- Docker & Docker Compose

### Deploy

```bash
# Clone
git clone https://github.com/SabrinaFans-Coder/F1-messageTool.git
cd F1-messageTool

# Set database password
echo "DB_PASSWORD=your_password" > .env

# Build and start
docker-compose up -d --build
```

Frontend: `http://localhost:3000`
Backend API: `http://localhost:8080/api/f1/`

### Update

```bash
git pull
docker-compose up -d --build
```

## Project Structure

```
F1-messageTool/
├── backend/
│   └── src/main/java/com/springboot/backend/
│       ├── controller/    # REST endpoints
│       ├── service/       # OpenF1 & Jolpica API integration
│       ├── entity/        # JPA entities
│       └── repository/    # Data access
├── frontend/
│   └── src/
│       ├── pages/         # Calendar, Results, Drivers, Notes
│       ├── components/    # Shared UI components
│       ├── hooks/         # Data fetching hooks
│       └── types/         # TypeScript interfaces
└── docker-compose.yml
```

## API Endpoints

| Endpoint | Description |
|----------|-------------|
| `GET /api/f1/meetings?year=` | Season calendar |
| `GET /api/f1/season-standings?year=` | Driver & constructor standings |
| `GET /api/f1/drivers-list?year=` | All drivers with headshots |
| `GET /api/f1/driver-detail-v2?driverNumber=&year=` | Driver profile & results |
| `GET /api/f1/circuit-history?meetingKey=` | Circuit historical results |
| `GET /api/f1/season-rankings?year=&limit=` | Recent race rankings |
| `GET /api/notes` | CRUD for personal notes |
| `GET /api/favorites` | Favorite drivers |

## License

MIT
