#!/bin/bash

echo "Stopping Keycloak server and PostgreSQL..."

# Stop and remove containers
docker stop keycloak 2>/dev/null || true
docker rm keycloak 2>/dev/null || true
docker stop keycloak-postgres 2>/dev/null || true
docker rm keycloak-postgres 2>/dev/null || true

echo "Keycloak and PostgreSQL stopped and removed"
echo "Data volumes are preserved for next restart"
echo ""
echo "To completely remove all data (including volumes), run:"
echo "docker volume rm keycloak-postgres-data keycloak-data"
