#!/bin/bash

echo "Setting up Keycloak realm and client with email verification..."

# Wait for Keycloak to be ready
echo "Waiting for Keycloak to be ready..."
until curl -s http://localhost:8080/realms/master > /dev/null; do
  echo "Waiting for Keycloak..."
  sleep 5
done

echo "Keycloak is ready. Setting up realm..."

# Login to admin CLI
docker exec keycloak /opt/keycloak/bin/kcadm.sh config credentials \
  --server http://localhost:8080 \
  --realm master \
  --user admin \
  --password admin

# Create realm with email verification enabled
echo "Creating realm 'myrealm' with email verification..."
docker exec keycloak /opt/keycloak/bin/kcadm.sh create realms \
  -s realm=myrealm \
  -s enabled=true \
  -s verifyEmail=true \
  -s loginWithEmailAllowed=true \
  -s registrationAllowed=true \
  -s registrationEmailAsUsername=false \
  -s resetPasswordAllowed=true

# Configure email settings (using a mock SMTP for development)
echo "Configuring email settings..."
docker exec keycloak /opt/keycloak/bin/kcadm.sh update realms/myrealm \
  -s 'smtpServer.host=localhost' \
  -s 'smtpServer.port=1025' \
  -s 'smtpServer.from=noreply@yourapp.com' \
  -s 'smtpServer.fromDisplayName=Your App' \
  -s 'smtpServer.ssl=false' \
  -s 'smtpServer.starttls=false'

# Create client with direct access grants enabled and proper configuration
echo "Creating client 'auth-service'..."
docker exec keycloak /opt/keycloak/bin/kcadm.sh create clients -r myrealm \
  -s clientId=auth-service \
  -s enabled=true \
  -s publicClient=false \
  -s serviceAccountsEnabled=true \
  -s directAccessGrantsEnabled=true \
  -s standardFlowEnabled=true \
  -s implicitFlowEnabled=false \
  -s 'redirectUris=["*"]' \
  -s 'webOrigins=["*"]' \
  -s 'attributes."access.token.lifespan"="3600"' \
  -s 'attributes."client_credentials.use_refresh_token"="false"'

# Get client UUID and secret
echo "Getting client secret..."
CLIENT_UUID=$(docker exec keycloak /opt/keycloak/bin/kcadm.sh get clients -r myrealm -q clientId=auth-service | grep -o '"id":"[^"]*"' | sed 's/"id":"\([^"]*\)"/\1/' | head -1)
CLIENT_SECRET=$(docker exec keycloak /opt/keycloak/bin/kcadm.sh get clients/$CLIENT_UUID/client-secret -r myrealm | grep -o '"value":"[^"]*"' | sed 's/"value":"\([^"]*\)"/\1/')

echo "Client Secret: $CLIENT_SECRET"

# Create roles
echo "Creating roles..."
docker exec keycloak /opt/keycloak/bin/kcadm.sh create roles -r myrealm -s name=ROLE_USER
docker exec keycloak /opt/keycloak/bin/kcadm.sh create roles -r myrealm -s name=ROLE_ADMIN

# Create protocol mapper to include roles in token
echo "Creating roles mapper..."
docker exec keycloak /opt/keycloak/bin/kcadm.sh create clients/$CLIENT_UUID/protocol-mappers/models -r myrealm \
  -s name=realm-roles \
  -s protocol=openid-connect \
  -s protocolMapper=oidc-usermodel-realm-role-mapper \
  -s 'config."claim.name"=roles' \
  -s 'config."multivalued"=true' \
  -s 'config."id.token.claim"=true' \
  -s 'config."access.token.claim"=true' \
  -s 'config."userinfo.token.claim"=true'

# Configure login flow to require email verification
echo "Configuring authentication flow for email verification..."
docker exec keycloak /opt/keycloak/bin/kcadm.sh update realms/myrealm \
  -s 'attributes."bruteForceProtected"=true' \
  -s 'attributes."failureFactor"=5' \
  -s 'attributes."maxDeltaTime"=43200' \
  -s 'attributes."maxFailureWaitTime"=900'

echo ""
echo "========================================="
echo "KEYCLOAK SETUP COMPLETE WITH EMAIL VERIFICATION!"
echo "========================================="
echo "Realm: myrealm"
echo "Client ID: auth-service"
echo "Client Secret: $CLIENT_SECRET"
echo "Email Verification: ENABLED"
echo "Admin URL: http://localhost:8080/admin"
echo "Admin credentials: admin/admin"
echo ""
echo "Update your application.yml with the client secret:"
echo ""
echo "keycloak:"
echo "  server-url: http://localhost:8080"
echo "  realm: myrealm"
echo "  client-id: auth-service"
echo "  client-secret: $CLIENT_SECRET"
echo "  admin-username: admin"
echo "  admin-password: admin"
echo ""
echo "IMPORTANT NOTES:"
echo "1. Email verification is now REQUIRED for all new users"
echo "2. Users must verify their email before they can login"
echo "3. SMTP is configured for localhost:1025 (development)"
echo "4. For production, configure proper SMTP settings in Keycloak admin console"
echo ""
echo "To test email verification in development:"
echo "1. Install MailHog: docker run -d -p 1025:1025 -p 8025:8025 mailhog/mailhog"
echo "2. View emails at: http://localhost:8025"
echo ""
echo "New API endpoints available:"
echo "- POST /api/auth/resend-verification-email?username=<username>"
echo "- GET /api/auth/email-verification-status?username=<username>"