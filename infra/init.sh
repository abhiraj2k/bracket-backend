#!/bin/bash
set -e

# Install Docker
curl -fsSL https://get.docker.com | sh
systemctl enable docker
systemctl start docker

# Install Docker Compose plugin
apt-get install -y docker-compose-plugin

git clone https://github.com/abhiraj2k/bracket-backend.git /app
cd /app

echo ""
echo "Docker installed and repo cloned to /app. Next steps:"
echo "  1. Create /app/.env with:"
echo "       DB_NAME=expense_tracker"
echo "       DB_USER=expense_user"
echo "       DB_PASSWORD=<strong-password>"
echo "       JWT_SECRET=\$(openssl rand -base64 32)"
echo "       ALLOWED_ORIGINS=https://YOUR-APP.vercel.app"
echo "  2. docker compose -f docker-compose.prod.yml up -d"
echo "  3. docker compose -f docker-compose.prod.yml ps   # verify all 4 healthy"
echo "  4. curl http://localhost/actuator/health          # should return {\"status\":\"UP\"}"
