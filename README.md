# CRM Tool — Loan Operations Platform

Internal CRM for **Trillion Loans / BP Capital** collections and loan operations teams. Agents, team leads, and admins use one workspace to manage customer follow-ups, view loan and profile data, handle support tickets, and track IVR call history.

This repo contains the **CRM application** (`crm-service` + `crm-frontend`) and reference copies of related services (LOS, LMS, customer portal) used for local integration during development.

---

## What it does

| Role | Route | Capabilities |
|------|-------|--------------|
| **Agent** | `/agent` | Personal queue, customer 360°, Freshdesk support, IVR call history, workflow actions |
| **Lead** | `/lead` | Team queue, bucket stats, lead reassignment, resolution rates |
| **Admin** | `/admin` | Ops overview, ticket explorer, user management, integrations health |

### Key features

- **Customer 360°** — profile, loans, tickets, calls, and agent notes in one view
- **CRM lead queue** — ingest, assign, status updates, round-robin assignment
- **LOS / LMS integration** — live customer and loan data via REST (with mock fallback)
- **Freshdesk** — agent ticket buckets, replies, status updates (when configured)
- **IVR / Exotel** — call history from GreyLabs bot and Exotel sync; masked mobile numbers
- **PII controls** — field reveal with audit trail
- **Admin ops dashboard** — org/team/agent metrics, ticket explorer, health checks

### Architecture (summary)

```
crm-frontend (:3001)  →  crm-service (:8092)  →  PostgreSQL (crm)
                              ↓
                    LOS (:8090) · LMS (:8091) · Exotel · Freshdesk
```

Detailed design: [`docs/CRM_ARCHITECTURE.md`](docs/CRM_ARCHITECTURE.md)

---

## Tech stack

| Layer | Technology |
|-------|------------|
| Frontend | Next.js 14, React 18, TypeScript, Axios |
| Backend | Java 17, Spring Boot 3.3, Spring MVC, JDBC |
| Database | PostgreSQL 16 |
| Integrations | RestClient → LOS, LMS, Exotel, Freshdesk |

---

## Prerequisites

Install these before running locally:

| Tool | Version | Notes |
|------|---------|-------|
| **Java** | 17 | Required for `crm-service` |
| **Maven** | 3.8+ | Or use `./los-service-main/mvnw` bundled in repo |
| **Node.js** | 18+ | Required for `crm-frontend` |
| **Docker** | Latest | For local PostgreSQL (`crm-service/docker-compose.yml`) |
| **LOS / LMS** | Optional | Needed only for live customer/loan data |

---

## Local setup

### 1. Clone the repository

```bash
git clone https://github.com/tl-tanishasinghai/CRM-Tool.git
cd CRM-Tool
```

### 2. Start PostgreSQL

From the `crm-service` directory:

```bash
cd crm-service
docker compose up -d
```

This starts Postgres on **port 5433** with database `crm`, user `postgres`, password `postgres`.

> **Without Docker:** create a database named `crm` on your local Postgres and run `crm-service/src/main/resources/schema.sql` and `db/seed_data.sql` manually.

### 3. Start the backend

```bash
cd crm-service
SPRING_PROFILES_ACTIVE=local ../los-service-main/mvnw spring-boot:run
```

The `local` profile auto-applies schema and demo seed data on startup.

- API base URL: **http://localhost:8092**
- Health: **http://localhost:8092/actuator/health**

**Optional environment variables:**

| Variable | Default | Purpose |
|----------|---------|---------|
| `CRM_DB_URL` | `jdbc:postgresql://localhost:5432/crm` | Override JDBC URL (local profile uses 5433) |
| `LOS_SERVICE_BASE_URL` | `http://localhost:8090` | LOS integration |
| `LMS_SERVICE_BASE_URL` | `http://localhost:8091` | LMS integration |
| `CRM_USE_LIVE_DATA` | `true` | Set `false` to always use mock loan/profile data |
| `EXOTEL_*` | — | Exotel call sync credentials |
| `FRESHDESK_*` | — | Freshdesk API credentials |

### 4. Start the frontend

In a new terminal:

```bash
cd crm-frontend
npm install
npm run dev
```

- UI: **http://localhost:3001**

Optional: set `NEXT_PUBLIC_CRM_API_BASE_URL=http://localhost:8092` if the API is not on the default host.

### 5. Log in (demo accounts)

Open **http://localhost:3001/login** and use **Open demo** on any role card, or sign in manually:

| Role | Email | Password |
|------|-------|----------|
| Agent | `agent1@trillionloans.com` | `password` |
| Lead | `lead@trillionloans.com` | `password` |
| Admin | `admin@trillionloans.com` | `password` |

---

## Project structure

```
CRM-Tool/
├── crm-frontend/          Next.js UI (port 3001)
├── crm-service/           Spring Boot BFF (port 8092)
│   ├── docker-compose.yml Local Postgres
│   └── src/main/resources/
│       ├── schema.sql       Database DDL (copy for production)
│       └── db/seed_data.sql Demo users & sample data
├── docs/
│   └── CRM_ARCHITECTURE.md  HLD / LLD / tech comparison
├── los-service-main/        Loan Origination Service (reference)
├── lms-service-main/        Loan Management Service (reference)
└── customer-portal-*/       Customer portal (reference)
```

---

## Running with live LOS / LMS data

1. Start **LOS** on port **8090** and **LMS** on port **8091** (see their respective READMEs in this repo).
2. Ensure `CRM_USE_LIVE_DATA=true` (default).
3. Restart `crm-service`.

If LOS/LMS are unavailable, the CRM falls back to seeded mock profile and loan data so the UI remains usable.

---

## Production deployment notes

1. Create a dedicated PostgreSQL database (e.g. `crm`).
2. Apply `crm-service/src/main/resources/schema.sql` — **do not** run `seed_data.sql` in production.
3. Set `CRM_DB_URL`, `CRM_DB_USER`, `CRM_DB_PASSWORD` — **do not** use `SPRING_PROFILES_ACTIVE=local`.
4. Configure Exotel and Freshdesk env vars if integrations are required.
5. Build frontend: `cd crm-frontend && npm run build && npm start`.

---

## Troubleshooting

| Issue | Fix |
|-------|-----|
| Port 8092 already in use | `lsof -ti:8092 \| xargs kill -9` then restart backend |
| Database connection refused | Ensure Docker is running: `docker compose up -d` in `crm-service/` |
| Login fails after restart | Re-login — sessions are stored in PostgreSQL and persist across restarts |
| Empty customer/loan data | Start LOS/LMS or set `CRM_USE_LIVE_DATA=false` for mocks |
| Frontend can't reach API | Confirm backend is on 8092 and CORS allows `http://localhost:3001` |

---

## License

Internal use — Trillion Loans / BP Capital.

