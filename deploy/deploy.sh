#!/usr/bin/env bash
set -euo pipefail

cd /opt/mtproxycheck

git fetch origin
git reset --hard origin/master

docker compose -f compose.mtproxy.yml up -d --build

for i in {1..20}; do
  if curl -fsS http://mtproxycheck.ru/actuator/health > /dev/null; then
    echo "Backend is healthy"
    exit 0
  fi
  sleep 5
done

echo "Backend did not become healthy"
docker compose -f compose.mtproxy.yml logs --tail=200 mtproxy-backend
exit 1
