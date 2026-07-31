# Module Service

Repair-and-shipment tracking app for an electronics service (LED display modules).
Clients send packages of modules to the service; technicians scan, repair and
catalog them; packages are shipped back and confirmed. Includes internal
(Sport360) packages, statistics, audit, and a public module-history lookup.

The full specification lives in [`docs/`](docs/).

## Stack

- **Backend** — Spring Boot 3 on JVM 21 (Java + Kotlin), Spring Data JPA,
  Flyway, PostgreSQL 16.
- **Frontend** — React 18 + Vite + TypeScript + Tailwind CSS.

## Prerequisites

- JDK 21
- Node.js 20+
- Docker (for PostgreSQL 16)

## Run locally

```bash
# 1. Start PostgreSQL 16
docker compose up -d db

# 2. Backend (http://localhost:8080) — Flyway applies V1 on startup
cd backend && ./mvnw spring-boot:run

# 3. Frontend (http://localhost:5173) — proxies /api to the backend
cd frontend && npm install && npm run dev
```

Health check: <http://localhost:8080/api/v1/health> →
`{"status":"ok","time":"…"}`.

## Verify the backend

```bash
cd backend && ./mvnw verify   # boots a Testcontainers PostgreSQL, runs Flyway, checks the seed
```

## Configuration

The backend reads everything sensitive from environment variables. Dev defaults
point at the Docker Compose database:

| Variable | Dev default | Purpose |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/service_app` | JDBC URL |
| `DB_USERNAME` | `service_app` | DB user |
| `DB_PASSWORD` | `service_app` | DB password |
| `SPRING_PROFILES_ACTIVE` | `dev` | Active profile (`dev` / `prod`) |

The single admin account is created on first start from
`APP_BOOTSTRAP_ADMIN_EMAIL` / `APP_BOOTSTRAP_ADMIN_PASSWORD`.

## Troubleshooting

- **`password authentication failed for user "service_app"` on startup** — a
  *native* PostgreSQL is already listening on `localhost:5432` and shadows the
  Docker container (`localhost` resolves to it first). Stop the native instance,
  or change the host port mapping in `docker-compose.yml` (e.g. `5433:5432`) and
  set `DB_URL=jdbc:postgresql://localhost:5433/service_app`.
- **`mvn verify` — "Could not find a valid Docker environment" / HTTP 400** —
  Docker Desktop must be running. Very new Docker Engine (29+, MinAPI 1.44)
  needs Testcontainers ≥ 1.21 (already pinned in `pom.xml`).
