# Mtprototest

Backend and web application for collecting, validating, ranking, and distributing public Telegram MTProto proxies.

Current product direction: `B2C website`.

The system imports proxies from external sources, checks their availability, performs deeper MTProto validation, stores historical results, calculates a reliability score, and exposes the best candidates through a public website and REST API.

## Product Status

The project is already beyond a raw prototype.

Implemented today:

- proxy import from external sources
- normalization and deduplication on import
- scheduled proxy lifecycle checks
- quick connectivity checks
- deep MTProto probe for stronger verification
- scoring based on recent checks and feedback
- feedback collection from users
- public proxy API
- simple web UI for desktop and mobile usage
- Liquibase-based schema management
- automated tests for core flows

Current focus:

- finish the first working public web version
- improve data quality with multiple sources
- harden the API for real users
- improve the B2C website UX
- add minimal admin controls and production observability

Detailed execution plan lives in [BACKLOG.md](./BACKLOG.md).

## Problem

Public MTProto proxies are unreliable:

- many die quickly
- some respond on TCP but do not work correctly with Telegram
- some degrade after a short period
- source quality is inconsistent

This project turns unstable raw proxy feeds into a ranked catalog of better candidates.

## What the System Does

High-level flow:

1. Fetch raw proxy links from configured external sources.
2. Normalize and deduplicate entries.
3. Store new proxies in PostgreSQL.
4. Run lifecycle checks on new, alive, and dead proxies.
5. Use quick connectivity checks and deeper MTProto probes.
6. Persist check history and update proxy status.
7. Recalculate score using status, latency, freshness, and feedback.
8. Expose best proxies through REST API and website.

## Architecture

Main domains:

- `parser`: ingestion from external proxy sources
- `checker`: connectivity checks and MTProto verification
- `proxy`: public API, listing, stats, feedback
- `scoring`: reliability score calculation
- `common.metrics`: operational metrics
- `bootstrap`: startup import and initial checking

Layering:

- controllers expose HTTP endpoints
- services hold business logic
- repositories handle persistence
- Liquibase manages database schema
- static frontend consumes public API

## Current Tech Stack

- Java 21
- Spring Boot
- Spring Web MVC
- Spring Data JPA
- Liquibase
- PostgreSQL
- Micrometer / Actuator
- Gradle
- Docker Compose
- JUnit / Mockito / Spring Boot Test

## Current Public API

### Proxy endpoints

- `GET /api/proxies`
  Paginated proxy list with filters and sorting.

- `GET /api/proxies/best`
  Best currently available proxies for public consumption.

- `GET /api/proxies/{proxyId}`
  Single proxy details.

- `GET /api/proxies/stats`
  Aggregate stats and recent check summary.

- `POST /api/proxies/{proxyId}/feedback`
  Submit user feedback for a proxy.

### Operations endpoints

- `POST /api/import/proxies`
  Trigger proxy import manually.

- `POST /api/check/proxies`
  Trigger lifecycle checks manually.

## Verification Model

The system distinguishes between:

- `VERIFIED`: passed deeper MTProto verification
- `QUICK_OK`: reachable by quick check, but not fully verified
- `UNVERIFIED`: no reliable confirmation yet

This matters because a TCP-successful proxy is not automatically a working MTProto proxy.

## Scoring Model

The current score uses:

- verification confidence
- recent success rate
- latency
- freshness of recent success
- failure streak
- recent user feedback

Score is intended to rank better candidates, not to guarantee that a proxy will work for every user in every network environment.

## Running the Project

### Prerequisites

- Java 21
- PostgreSQL
- Docker Desktop if using Docker Compose

### Database

Default local configuration:

- database: `mtproto_db`
- user: `postgres`
- password: `postgres`

Configured in [`application.yaml`](./src/main/resources/application.yaml).

### Local Run

```bash
./gradlew bootRun
```

On Windows:

```powershell
.\gradlew.bat bootRun
```

### Tests

```bash
./gradlew test
```

## Docker

The repository includes `docker-compose.yml` for local environment setup.

Start locally:

```bash
docker-compose up --build
```

## Scheduling

The application runs background jobs for:

- proxy import
- lifecycle checking of new proxies
- recheck of alive proxies
- retry of dead proxies

On startup, the application can also bootstrap import and initial checking.

Relevant settings are configured under `app.*` in [`application.yaml`](./src/main/resources/application.yaml).

## Current Limitations

These are known gaps before `v1` launch:

- only one external source is configured right now
- public API still needs hardening and rate limiting
- website UX is still closer to a functional demo than a finished product
- admin surface is minimal
- retention policy for large historical datasets is not finalized
- deployment/monitoring flow still needs production shaping

## Product Roadmap

Near-term priorities:

- add multi-source ingestion
- improve source quality visibility
- harden public API and feedback abuse protection
- turn the web UI into a polished B2C website
- add admin controls for moderation and manual recheck
- complete observability and launch preparation

Full task plan and moderation points are maintained in [BACKLOG.md](./BACKLOG.md).

## Repository Pointers

- [`BACKLOG.md`](./BACKLOG.md): product backlog and release plan
- [`build.gradle.kts`](./build.gradle.kts): project dependencies and build setup
- [`src/main/resources/application.yaml`](./src/main/resources/application.yaml): runtime configuration
- [`src/main/resources/static/index.html`](./src/main/resources/static/index.html): current website
- [`src/main/resources/db/changelog`](./src/main/resources/db/changelog): database migrations

## Next Product Milestone

Target: first working B2C web release by `2026-05-01`.
