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

### Important Runtime Settings

Before running outside local development, review these settings in [`application.yaml`](./src/main/resources/application.yaml):

- `spring.datasource.*`: PostgreSQL connection
- `app.sources.entries`: enabled proxy sources
- `app.admin.enabled`, `app.admin.header-name`, `app.admin.key`: admin API protection
- `app.startup.bootstrap-enabled`: startup import + startup check cycle
- `app.startup.check-batch-size`, `app.startup.deep-probe-limit`: bounded startup workload
- `app.checker.batch-size`, `app.checker.deep-probe-limit`: lifecycle scheduler budget
- `app.checker.executor.*`: effective checker concurrency and submission window
- `app.rate-limit.*`: public API and feedback limits
- `app.feedback.*`: abuse protection thresholds
- `app.analytics.posthog.*`: public frontend product analytics configuration

Production overrides live in [`application-prod.yaml`](./src/main/resources/application-prod.yaml).
An example env file lives in [`.env.example`](./.env.example).

### Local Run

```bash
./gradlew bootRun
```

On Windows:

```powershell
.\gradlew.bat bootRun
```

Production-style profile run:

```powershell
.\gradlew.bat bootRun "--args=--spring.profiles.active=prod"
```

### Local Smoke Run Without Startup Bootstrap

Useful for a quick local API/health check without doing a full startup import/check pass.
This uses the normal runtime stack and still expects PostgreSQL; it is not the test profile:

```powershell
.\gradlew.bat bootRun "--args=--spring.profiles.active=smoke --server.port=18081 --app.admin.enabled=true --app.admin.key=test-admin-key"
```

Then verify:

- `GET /actuator/prometheus`
- `GET /docs`
- `GET /api/v1/proxies/stats`
- `GET /api/v1/frontend-config`

Notes:

- if `app.startup.bootstrap-enabled=false`, the application can start correctly while `/actuator/health` still reports `DOWN/503`
- this is expected until at least one successful import cycle produces source snapshots for the custom `proxyImports` health contributor
- the `smoke` profile disables startup bootstrap and pushes parser/checker schedules far enough out to keep the run quiet

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

The compose file is intended for local PostgreSQL only and now supports:

- env-driven database credentials and port mapping
- persistent named volume
- PostgreSQL healthcheck

## Scheduling

The application runs background jobs for:

- proxy import
- lifecycle checking of new proxies
- recheck of alive proxies
- retry of dead proxies

On startup, the application can also bootstrap import and initial checking.

Relevant settings are configured under `app.*` in [`application.yaml`](./src/main/resources/application.yaml).

## Operations and Observability

Currently exposed operational endpoints:

- `/actuator/health`
- `/actuator/info`
- `/actuator/metrics`
- `/actuator/prometheus`
- `/api/v1/frontend-config`
- `/docs`
- `/v3/api-docs`

Prometheus scrape notes:

- scrape `/actuator/prometheus`
- use `/actuator/metrics` when you want to inspect meter names one by one
- for a quick local review of custom meters, search the scrape output for `proxy_`

System metrics already exported to Prometheus:

- JVM memory, GC, threads, classes, and buffer pools
- process uptime and process CPU usage
- system CPU and load metrics
- HTTP server request metrics such as `http_server_requests_seconds_*`
- logging metrics such as `logback_events_total`

Custom proxy metrics already exported:

- `proxy_state_count`
- `proxy_verification_count`
- `proxy_imported_total`
- `proxy_imported_by_source_total`
- `proxy_import_skipped_by_source_total`
- `proxy_import_rejected_by_source_total`
- `proxy_import_rejected_by_source_reason_total`
- `proxy_import_failure_by_source_total`
- `proxy_import_duration_seconds_*`
- `proxy_check_success_total`
- `proxy_check_failure_total`
- `proxy_check_cycle_duration_seconds_*`
- `proxy_check_cycle_skipped_total`
- `proxy_deep_probe_total`
- `proxy_feedback_submitted_total`
- custom health for proxy imports and checker freshness

Current gaps worth knowing about:

- there is still no dedicated backend metric for frontend-only actions like copy/open-link because those are now tracked in PostHog instead
- there is no separate Prometheus dashboarding stack in this repository; only the export endpoint is provided

Admin and manual operations:

- public website uses `/api/v1/proxies/*`
- manual import and check endpoints require the configured admin header/key
- admin overview, moderation, diagnostics, and recheck endpoints also require admin access

Recommended production reverse-proxy behavior:

- keep `/api/v1/proxies/*` public
- restrict `/api/v1/admin/*`, `/api/v1/check/*`, and `/api/v1/import/*`
- preserve real client IP headers consistently, because rate limiting and feedback controls depend on client identity
- expect `/actuator/health` to remain `DOWN` until imports and checker cycles have produced valid operational snapshots

Quick local Prometheus verification:

```powershell
Invoke-WebRequest http://localhost:18081/actuator/prometheus | Select-Object -ExpandProperty Content
```

Useful checks in the scrape output:

- `jvm_memory_used_bytes`
- `process_uptime_seconds`
- `http_server_requests_seconds_count`
- `proxy_state_count`
- `proxy_feedback_submitted_total`

## Product Analytics

The public landing page lives in [`src/main/resources/static/index.html`](./src/main/resources/static/index.html).
PostHog is integrated there as product analytics and is configured at runtime through the public backend endpoint:

- `GET /api/v1/frontend-config`

Environment variables:

- `APP_ANALYTICS_POSTHOG_ENABLED`
- `APP_ANALYTICS_POSTHOG_API_KEY`
- `APP_ANALYTICS_POSTHOG_HOST`

Behavior:

- if PostHog is disabled or not configured, the website still works normally
- if the PostHog script fails to load, the website still works normally
- the frontend sends only explicit product events; it does not rely on broad click-tracking for this integration

Tracked PostHog events:

- `page_view`
- `import_started`
- `import_finished`
- `best_proxy_shown`
- `copy_proxy`
- `open_telegram_link`
- `feedback_sent`

Event notes:

- on the current static landing page, `import_started` and `import_finished` are attached to the catalog load cycle because there is no separate user-facing import UI in this repository
- `best_proxy_shown` fires when the desktop hero card shows a new best candidate
- `copy_proxy`, `open_telegram_link`, and `feedback_sent` reuse the existing UI hooks and do not change the page behavior

How to verify PostHog locally:

1. Set `APP_ANALYTICS_POSTHOG_ENABLED=true`
2. Set `APP_ANALYTICS_POSTHOG_API_KEY` to the public PostHog project key
3. Set `APP_ANALYTICS_POSTHOG_HOST`, for example `https://us.i.posthog.com`
4. Open the landing page and trigger copy/open/feedback actions
5. Confirm `GET /api/v1/frontend-config` returns the PostHog config
6. Confirm events appear in PostHog live events or in the browser network log

## Deployment Notes

Minimum deployment assumptions for `v1`:

- one PostgreSQL instance with persistent storage
- one app instance is enough for first launch
- admin key must not stay empty outside local development
- production should run with profile `prod`
- `app.startup.bootstrap-enabled` can stay on for small catalogs, but bounded startup settings should remain conservative
- monitor `/actuator/health` and `/actuator/prometheus` from day one

For this repository, reverse proxy and TLS termination are treated as external infrastructure.
The application only assumes that the external reverse proxy:

- forwards the real client IP consistently
- keeps `/api/v1/proxies/*` and the public website open
- restricts `/api/v1/admin/*`, `/api/v1/check/*`, `/api/v1/import/*`, and `/actuator/*` at the network or edge layer

Application-side VPS deployment artifacts:

- [`deploy/DEPLOY_TO_VPS.md`](./deploy/DEPLOY_TO_VPS.md)
- [`deploy/systemd/mtprototest.service`](./deploy/systemd/mtprototest.service)
- [`deploy/env/mtprototest.env.example`](./deploy/env/mtprototest.env.example)

Release and launch checks are tracked in [RELEASE_CHECKLIST.md](./RELEASE_CHECKLIST.md).

## Current Limitations

These are known gaps before `v1` launch:

- current source mix still needs final VPS-side observation and confirmation after deployment
- edge restrictions and public-surface validation still need final VPS-side verification
- website UX is still closer to a functional demo than a finished product
- admin surface is minimal
- retention policy for large historical datasets is not finalized
- deployment/monitoring flow still needs production shaping

## Product Roadmap

Near-term priorities:

- stabilize multi-source ingestion quality
- improve source quality visibility
- finish public API hardening and abuse validation on the real public path
- turn the web UI into a polished B2C website
- add admin controls for moderation and manual recheck
- complete observability and launch preparation

Full task plan and moderation points are maintained in [BACKLOG.md](./BACKLOG.md).

## Repository Pointers

- [`BACKLOG.md`](./BACKLOG.md): product backlog and release plan
- [`DEPLOYMENT_DECISIONS.md`](./DEPLOYMENT_DECISIONS.md): open deployment and operations decisions for launch
- [`RELEASE_CHECKLIST.md`](./RELEASE_CHECKLIST.md): pre-launch and release checklist
- [`build.gradle.kts`](./build.gradle.kts): project dependencies and build setup
- [`src/main/resources/application.yaml`](./src/main/resources/application.yaml): runtime configuration
- [`src/main/resources/static/index.html`](./src/main/resources/static/index.html): current website
- [`src/main/resources/db/changelog`](./src/main/resources/db/changelog): database migrations

## Next Product Milestone

Target: first working B2C web release by `2026-05-01`.
