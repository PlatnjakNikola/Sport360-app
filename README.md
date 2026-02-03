# Module Service

Repair-and-shipment tracking app for an electronics service (LED display modules).
Clients send packages of modules to the service; technicians repair and catalog
them; packages are shipped back and confirmed.

The specification lives in [`docs/`](docs/).

## Stack

- **Backend** — Spring Boot 3 on JVM 21 (Java + Kotlin), Spring Data JPA,
  Flyway, PostgreSQL 16.
- **Frontend** — React 18 + Vite + TypeScript + Tailwind CSS.

## Prerequisites

- JDK 21
- Node.js 20+
- Docker (for PostgreSQL 16)

## Development database

```bash
docker compose up -d db
```

The database runs on `localhost:5432` (`service_app` / `service_app`).
