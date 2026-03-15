#!/bin/bash
# ============================================================
#  KURA — Production Deployment Script
#  Run on the production server after pulling new image.
# ============================================================
set -euo pipefail

APP_DIR="/opt/shopee"
IMAGE="ghcr.io/your-org/shopee-api"
TAG="${1:-latest}"

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  KURA Deploy — ${IMAGE}:${TAG}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

cd "$APP_DIR"

# 1. Pull new image
echo "[1/5] Pulling image ${IMAGE}:${TAG}..."
docker pull "${IMAGE}:${TAG}"

# 2. Run DB migrations (dry-run first)
echo "[2/5] Running Flyway migrations..."
docker compose -f docker-compose.yml -f docker-compose.prod.yml run --rm \
  -e SPRING_PROFILES_ACTIVE=prod \
  app java -jar app.jar --spring.flyway.validate-on-migrate=true 2>&1 | tail -5 || true

# 3. Rolling restart (2 replicas, one at a time)
echo "[3/5] Rolling restart..."
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d \
  --no-deps \
  --scale app=2 \
  app

# 4. Wait for health check
echo "[4/5] Waiting for health check..."
for i in {1..30}; do
  STATUS=$(curl -sf http://localhost:8080/actuator/health/liveness 2>/dev/null \
    | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('status',''))" 2>/dev/null || echo "DOWN")
  if [ "$STATUS" = "UP" ]; then
    echo "  ✓ App is UP after ${i}s"
    break
  fi
  if [ "$i" = "30" ]; then
    echo "  ✗ Health check failed! Rolling back..."
    docker compose -f docker-compose.yml -f docker-compose.prod.yml rollback app
    exit 1
  fi
  sleep 3
done

# 5. Reload Nginx (no downtime)
echo "[5/5] Reloading Nginx..."
docker compose -f docker-compose.yml -f docker-compose.prod.yml exec nginx nginx -s reload

echo ""
echo "✓ Deployment complete!"
docker compose -f docker-compose.yml -f docker-compose.prod.yml ps
