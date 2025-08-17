#!/bin/bash

echo "Starting Keycloak server with persistent data..."

# Create a Docker network for Keycloak and PostgreSQL
docker network create keycloak-network 2>/dev/null || true

# Start PostgreSQL database
echo "Starting PostgreSQL database..."
docker run -d --name keycloak-postgres \
  --network keycloak-network \
  -e POSTGRES_DB=keycloak \
  -e POSTGRES_USER=keycloak \
  -e POSTGRES_PASSWORD=keycloak \
  -v keycloak-postgres-data:/var/lib/postgresql/data \
  postgres:15

# Wait for PostgreSQL to be ready
echo "Waiting for PostgreSQL to be ready..."
sleep 10

# Start Keycloak with PostgreSQL
echo "Starting Keycloak with persistent database..."
docker run --name keycloak \
  --network keycloak-network \
  -p 8080:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  -e KC_DB=postgres \
  -e KC_DB_URL=jdbc:postgresql://keycloak-postgres:5432/keycloak \
  -e KC_DB_USERNAME=keycloak \
  -e KC_DB_PASSWORD=keycloak \
  -e KC_HOSTNAME_STRICT=false \
  -e KC_HOSTNAME_STRICT_HTTPS=false \
  -e KC_HTTP_ENABLED=true \
  -v keycloak-data:/opt/keycloak/data \
  quay.io/keycloak/keycloak:26.0 start

echo "Keycloak started on http://localhost:8080"
echo "Admin credentials: admin/admin"
echo "Data will persist across restarts using PostgreSQL and Docker volumes"
