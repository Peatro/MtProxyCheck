# DEPLOYMENT DECISIONS

## Purpose

This file captures the remaining launch decisions that should be confirmed before the first public `v1` release.

The goal is to avoid silent assumptions around deployment, operations, and incident handling.

## Recommended Defaults

### Deployment Target

Recommended:

- one Linux VM or VPS
- one application instance
- one PostgreSQL instance with persistent storage
- reverse proxy in front of the app

Reason:

- enough for first public beta
- simplest rollback and debugging path
- no need for orchestration complexity yet

### Application Profile

Recommended:

- run with `prod` profile
- keep `SPRINGDOC_SWAGGER_UI_ENABLED=false`
- keep `SPRINGDOC_API_DOCS_ENABLED=false`
- keep `APP_ADMIN_ENABLED=true`

### Startup Policy

Recommended:

- keep `APP_STARTUP_BOOTSTRAP_ENABLED=true` in production
- keep bounded startup settings conservative
- do not switch back to full-catalog startup checks

Suggested starting values:

- `APP_STARTUP_CHECK_BATCH_SIZE=120`
- `APP_STARTUP_DEEP_PROBE_LIMIT=10`
- `APP_CHECKER_BATCH_SIZE=200`
- `APP_CHECKER_DEEP_PROBE_LIMIT=20`

### Reverse Proxy

Recommended:

- expose public website and public API only
- restrict admin/manual endpoints by network and header key
- preserve real client IP headers consistently

At minimum, protect:

- `/api/v1/admin/*`
- `/api/v1/check/*`
- `/api/v1/import/*`
- `/actuator/*`

### Monitoring

Recommended:

- scrape `/actuator/prometheus`
- poll `/actuator/health`
- alert on:
- app down
- health down for sustained period
- import health down
- checker health down

### Backup Strategy

Recommended:

- daily PostgreSQL dump
- retain at least `7` days
- store backups outside the app host if possible

### Logs

Recommended:

- capture stdout/stderr from the app process
- rotate logs at the process manager or host level
- keep enough history for at least the last `3-7` days

## Decisions Requiring Moderation

These are the remaining points that should be explicitly confirmed by the product owner.

### 1. Hosting

Need confirmation:

- where `v1` will run

Recommended answer:

- one VPS/Linux VM

### 2. Domain and TLS

Need confirmation:

- final domain name
- who provisions TLS

Recommended answer:

- custom domain with standard reverse proxy TLS termination

### 3. Admin Access Model

Need confirmation:

- whether admin endpoints should be reachable only from allowlisted IPs or from the public internet with header key protection

Recommended answer:

- allowlisted IPs plus admin key

### 4. Swagger Exposure

Need confirmation:

- keep `/docs` and `/v3/api-docs` public in production or disable them

Recommended answer:

- disable in `prod`

### 5. Actuator Exposure

Need confirmation:

- whether `/actuator/prometheus` is exposed directly or only through private network / reverse proxy restriction

Recommended answer:

- not public; restrict to monitoring path/network

### 6. Backup Owner

Need confirmation:

- who is responsible for database backups and restore verification

Confirmed answer:

- product owner: `Peatr`

### 7. Monitoring Owner

Need confirmation:

- who receives alerts

Confirmed answer:

- product owner: `Peatr`

### 8. Launch Traffic Expectation

Need confirmation:

- expected first-week traffic level

Recommended answer:

- low-volume public beta

## Suggested Go/No-Go Rule

Proceed with public `v1` only if all of the following are true:

- deployment target is chosen
- admin key is configured
- reverse proxy is configured
- backups are defined
- monitoring is defined
- release checklist is reviewed
