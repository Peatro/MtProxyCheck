# RELEASE CHECKLIST

## Fresh Start

- [ ] `./gradlew test` passes
- [ ] application starts locally with the intended `prod` or `smoke` runtime profile
- [ ] Liquibase migrations apply cleanly on an empty database
- [ ] `/actuator/health` returns `UP`
  Use the normal launch profile or allow at least one successful import/check cycle to complete first; with startup bootstrap disabled, custom operational health may stay `DOWN`
- [ ] `/actuator/prometheus` is reachable
- [ ] `/docs` and `/v3/api-docs` are reachable for local smoke runs
- [ ] Swagger remains disabled in production unless deliberately enabled for a bounded reason

## Configuration

- [ ] PostgreSQL connection values are set for the target environment
- [ ] `app.admin.enabled=true` outside local development
- [ ] `app.admin.key` is set and not empty
- [ ] enabled proxy sources are reviewed and reachable
- [ ] the configured `4` source feeds import successfully on the target VPS
- [ ] startup batch and deep probe limits are reviewed for the current catalog size
- [ ] checker batch size and executor concurrency are reviewed for the current catalog size
- [ ] public rate limits are reviewed
- [ ] feedback abuse thresholds are reviewed

## Operational Safety

- [ ] admin endpoints are not exposed without protection
  Confirmed model: allowlisted IPs plus admin key
- [ ] reverse proxy preserves real client IP headers consistently
- [ ] monitoring is configured against `/actuator/health` and `/actuator/prometheus`
- [ ] logs are collected from the application process
- [ ] database backup approach is defined
- [x] backup owner is assigned
  Owner: `Peatr`
- [x] monitoring owner is assigned
  Owner: `Peatr`

## Product Readiness

- [ ] public website loads on desktop
- [ ] public website loads on mobile width
- [ ] `/api/v1/proxies/best` returns usable candidates
- [ ] `/api/v1/proxies/stats` returns current catalog stats
- [ ] feedback submission works
- [ ] admin overview works
- [ ] manual admin recheck works
- [ ] deep-probe diagnostics endpoint works

## Launch Decision

- [ ] known launch risks are reviewed
- [ ] rollback path is understood
- [x] first-launch traffic expectations are documented
  Mode: `low-volume public beta`
- [ ] release timestamp and owner are recorded
