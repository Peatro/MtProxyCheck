# VPS Deploy Runbook

This repository assumes:

- one Linux VPS
- one PostgreSQL instance with persistent storage
- one app instance running from the built jar
- reverse proxy and TLS are managed outside this repository

## 1. Build and copy the app

Build locally or in CI:

```bash
./gradlew bootJar -x test
```

Copy the jar to the VPS:

- target path: `/opt/mtprototest/app.jar`

## 2. Prepare directories and user

Create the runtime user and directories:

```bash
sudo useradd --system --home /opt/mtprototest --shell /usr/sbin/nologin mtproto
sudo mkdir -p /opt/mtprototest
sudo mkdir -p /etc/mtprototest
sudo chown -R mtproto:mtproto /opt/mtprototest
```

## 3. Prepare environment file

Use [`deploy/env/mtprototest.env.example`](./env/mtprototest.env.example) as the template.

Recommended target path:

- `/etc/mtprototest/mtprototest.env`

Minimum production requirements:

- set real `DB_*` values
- set non-empty `APP_ADMIN_KEY`
- keep `SPRING_PROFILES_ACTIVE=prod`
- keep Swagger disabled in production unless explicitly needed

Restrict file permissions:

```bash
sudo chown root:mtproto /etc/mtprototest/mtprototest.env
sudo chmod 640 /etc/mtprototest/mtprototest.env
```

## 4. Install the systemd unit

Use [`deploy/systemd/mtprototest.service`](./systemd/mtprototest.service).

Recommended target path:

- `/etc/systemd/system/mtprototest.service`

Then enable and start:

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now mtprototest
```

## 5. Verify startup

Check the service:

```bash
sudo systemctl status mtprototest
sudo journalctl -u mtprototest -n 200 --no-pager
```

Check app endpoints from the VPS:

- `GET /api/v1/proxies/stats`
- `GET /actuator/health`
- `GET /actuator/prometheus`

Notes:

- with `app.startup.bootstrap-enabled=false`, custom health may stay `DOWN` until import snapshots exist
- with startup bootstrap enabled, allow the first import and checker cycle to finish before judging readiness

## 6. Reverse-proxy expectations

The external reverse proxy should:

- forward the real client IP consistently
- keep `/api/v1/proxies/*` and the public website open
- restrict `/api/v1/admin/*`, `/api/v1/check/*`, `/api/v1/import/*`, and `/actuator/*`

## 7. Restart and rollback

Restart after updating the jar or env file:

```bash
sudo systemctl restart mtprototest
```

Basic rollback:

1. restore the previous `app.jar`
2. restore the previous env file if config changed
3. restart `mtprototest`
4. re-check logs and `/actuator/health`
