#!/usr/bin/env bash
set -euo pipefail

cd /opt/streamcut

git fetch origin
git reset --hard origin/main

docker compose -f compose.streamcut.yml up -d --build

for i in {1..20}; do
  if curl -fsS http://localhost:8080/health > /dev/null; then
    echo "Backend is healthy"
    exit 0
  fi
  sleep 5
done

echo "Backend did not become healthy"
docker compose -f compose.streamcut.yml logs --tail=200 backend
exit 1
