#!/bin/bash

echo "Getting client secret from Keycloak..."

# Check if Keycloak container is running
if ! docker ps | grep -q "keycloak"; then
    echo "Error: Keycloak container is not running. Please start it first with ./start-keycloak.sh"
    exit 1
fi

# Login to admin CLI
echo "Logging into Keycloak admin CLI..."
docker exec keycloak /opt/keycloak/bin/kcadm.sh config credentials \
  --server http://localhost:8080 \
  --realm master \
  --user admin \
  --password admin

# Get client UUID for auth-service using grep and sed instead of jq
echo "Finding auth-service client..."
CLIENT_UUID=$(docker exec keycloak /opt/keycloak/bin/kcadm.sh get clients -r myrealm -q clientId=auth-service 2>/dev/null | grep -o '"id":"[^"]*"' | sed 's/"id":"\([^"]*\)"/\1/' | head -1)

if [ -z "$CLIENT_UUID" ]; then
    echo "Error: Client 'auth-service' not found in realm 'myrealm'"
    echo "Available clients in myrealm:"
    docker exec keycloak /opt/keycloak/bin/kcadm.sh get clients -r myrealm --fields clientId 2>/dev/null | grep -o '"clientId":"[^"]*"' | sed 's/"clientId":"\([^"]*\)"/\1/' 2>/dev/null || echo "No clients found or realm doesn't exist"
    echo ""
    echo "Please run ./setup-keycloak.sh to create the realm and client first"
    exit 1
fi

# Get client secret using grep and sed instead of jq
echo "Getting client secret..."
CLIENT_SECRET=$(docker exec keycloak /opt/keycloak/bin/kcadm.sh get clients/$CLIENT_UUID/client-secret -r myrealm 2>/dev/null | grep -o '"value":"[^"]*"' | sed 's/"value":"\([^"]*\)"/\1/')

if [ -z "$CLIENT_SECRET" ]; then
    echo "Error: Could not retrieve client secret"
    exit 1
fi

echo ""
echo "========================================="
echo "CLIENT SECRET FOUND:"
echo "========================================="
echo "Client ID: auth-service"
echo "Client Secret: $CLIENT_SECRET"
echo "========================================="
echo ""
echo "Update your application.yml with this secret:"
echo ""
echo "keycloak:"
echo "  server-url: http://localhost:8080"
echo "  realm: myrealm"
echo "  client-id: auth-service"
echo "  client-secret: $CLIENT_SECRET"
echo "  admin-username: admin"
echo "  admin-password: admin"
echo ""
echo "You can also access the Keycloak Admin Console at:"
echo "http://localhost:8080/admin"
echo "Username: admin"
echo "Password: admin"
echo ""
echo "To view the client secret in the UI:"
echo "1. Go to http://localhost:8080/admin"
echo "2. Login with admin/admin"
echo "3. Select 'myrealm' realm"
echo "4. Go to Clients → auth-service"
echo "5. Go to Credentials tab"
echo "6. Click 'Regenerate Secret' if needed"
