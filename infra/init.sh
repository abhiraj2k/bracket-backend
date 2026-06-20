#!/bin/bash
set -e

# Install Docker
curl -fsSL https://get.docker.com | sh
systemctl enable docker
systemctl start docker

# Install Docker Compose plugin
apt-get install -y docker-compose-plugin

# Clone repo (replace with your actual repo URL)
# git clone https://github.com/YOUR_USERNAME/expense-tracker.git /app
# cd /app

echo ""
echo "Docker installed. Next steps:"
echo "  1. Clone your repo or copy files to this VM"
echo "  2. Create /app/.env with DB_PASSWORD, JWT_SECRET, ALLOWED_ORIGINS"
echo "  3. cd /app && docker compose -f docker-compose.prod.yml up -d"
echo "  4. (Optional) Install Certbot: apt-get install -y certbot python3-certbot-nginx"
echo "              then: certbot --nginx -d yourdomain.com"
