package org.example.auth.service;

import org.example.auth.config.KeycloakProperties;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.ws.rs.core.Response;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class KeycloakClient {

    private static final Logger logger = LoggerFactory.getLogger(KeycloakClient.class);

    private final Keycloak keycloakAdminClient;
    private final KeycloakProperties keycloakProperties;

    @Autowired
    public KeycloakClient(Keycloak keycloakAdminClient, KeycloakProperties keycloakProperties) {
        this.keycloakAdminClient = keycloakAdminClient;
        this.keycloakProperties = keycloakProperties;
    }

    public String createUser(String username, String email, String password, String firstName, String lastName, OffsetDateTime dateOfBirth, Set<String> roles) {
        try {
            RealmResource realmResource = keycloakAdminClient.realm(keycloakProperties.getRealm());
            UsersResource usersResource = realmResource.users();

            // Check if user already exists
            List<UserRepresentation> existingUsers = usersResource.search(username, true);
            for (UserRepresentation existing : existingUsers) {
                if (username.equals(existing.getUsername())) {
                    logger.warn("User {} already exists in Keycloak", username);
                    throw new RuntimeException("User already exists");
                }
            }

            // Create user representation with all required fields
            UserRepresentation user = new UserRepresentation();
            user.setUsername(username);
            user.setEmail(email);
            user.setEnabled(true);
            user.setEmailVerified(true); // Important: Set to false to require email verification
            user.setFirstName(firstName);
            user.setLastName(lastName);

            // Set date of birth as user attribute (store as ISO string to preserve timezone)
            Map<String, List<String>> attributes = new HashMap<>();
            attributes.put("dateOfBirth", List.of(dateOfBirth.toString()));
            user.setAttributes(attributes);

            // Create user
            Response response = usersResource.create(user);
            if (response.getStatus() == 409) {
                throw new RuntimeException("User already exists");
            }
            if (response.getStatus() != 201) {
                throw new RuntimeException("Failed to create user in Keycloak: " + response.getStatusInfo());
            }

            // Get user ID from location header
            String userId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");
            logger.info("Created user in Keycloak with ID: {}", userId);

            // Set password as non-temporary
            UserResource userResource = usersResource.get(userId);
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(password);
            credential.setTemporary(false);
            userResource.resetPassword(credential);

            // Assign roles
            assignRolesToUser(userId, roles);

            response.close();
            return userId;
        } catch (Exception e) {
            logger.error("Error creating user in Keycloak", e);
            if (e.getMessage() != null && e.getMessage().contains("already exists")) {
                throw new RuntimeException("Username already exists", e);
            }
            throw new RuntimeException("Failed to create user in Keycloak", e);
        }
    }

    public void assignRolesToUser(String userId, Set<String> roleNames) {
        try {
            RealmResource realmResource = keycloakAdminClient.realm(keycloakProperties.getRealm());
            UserResource userResource = realmResource.users().get(userId);

            List<RoleRepresentation> rolesToAssign = roleNames.stream()
                    .map(roleName -> {
                        try {
                            return realmResource.roles().get(roleName).toRepresentation();
                        } catch (Exception e) {
                            logger.warn("Role {} not found, skipping", roleName);
                            return null;
                        }
                    })
                    .filter(role -> role != null)
                    .toList();

            if (!rolesToAssign.isEmpty()) {
                userResource.roles().realmLevel().add(rolesToAssign);
                logger.info("Assigned roles {} to user {}", roleNames, userId);
            }
        } catch (Exception e) {
            logger.error("Error assigning roles to user", e);
            throw new RuntimeException("Failed to assign roles", e);
        }
    }

    public UserRepresentation getUserByUsername(String username) {
        try {
            RealmResource realmResource = keycloakAdminClient.realm(keycloakProperties.getRealm());
            List<UserRepresentation> users = realmResource.users().search(username, true);

            return users.stream()
                    .filter(user -> username.equals(user.getUsername()))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            logger.error("Error getting user by username", e);
            return null;
        }
    }

    public List<String> getUserRoles(String userId) {
        try {
            RealmResource realmResource = keycloakAdminClient.realm(keycloakProperties.getRealm());
            UserResource userResource = realmResource.users().get(userId);

            return userResource.roles().realmLevel().listAll().stream()
                    .map(RoleRepresentation::getName)
                    .toList();
        } catch (Exception e) {
            logger.error("Error getting user roles", e);
            return Collections.emptyList();
        }
    }

    public String getAccessToken(String username, String password) {
        try {
            logger.info("Attempting to get access token for user: {}", username);
            logger.info("Using server URL: {}", keycloakProperties.getServerUrl());
            logger.info("Using realm: {}", keycloakProperties.getRealm());
            logger.info("Using client ID: {}", keycloakProperties.getClientId());

            Keycloak userKeycloak = KeycloakBuilder.builder()
                    .serverUrl(keycloakProperties.getServerUrl())
                    .realm(keycloakProperties.getRealm())
                    .grantType("password")
                    .clientId(keycloakProperties.getClientId())
                    .clientSecret(keycloakProperties.getClientSecret())
                    .username(username)
                    .password(password)
                    .build();

            String token = userKeycloak.tokenManager().getAccessTokenString();
            userKeycloak.close();
            logger.info("Successfully obtained access token for user: {}", username);
            return token;
        } catch (jakarta.ws.rs.BadRequestException e) {
            logger.error("HTTP 400 Bad Request - Client configuration issue. Check if Direct Access Grants is enabled for client: {}", keycloakProperties.getClientId());
            logger.error("Also verify the client secret is correct");
            throw new RuntimeException("Authentication failed - client configuration error", e);
        } catch (jakarta.ws.rs.NotAuthorizedException e) {
            logger.error("HTTP 401 Unauthorized - Invalid credentials for user: {}", username);
            throw new RuntimeException("Invalid username or password", e);
        } catch (Exception e) {
            logger.error("Error getting access token for user: {}", username, e);
            throw new RuntimeException("Authentication failed: " + e.getMessage(), e);
        }
    }

    public boolean isEmailVerified(String username) {
        try {
            UserRepresentation user = getUserByUsername(username);
            return user != null && user.isEmailVerified();
        } catch (Exception e) {
            logger.error("Error checking email verification status for user: {}", username, e);
            return false;
        }
    }

    public void resendVerificationEmail(String username) {
        try {
            UserRepresentation user = getUserByUsername(username);
            if (user == null) {
                throw new RuntimeException("User not found");
            }

            if (user.isEmailVerified()) {
                throw new RuntimeException("Email already verified");
            }

            RealmResource realmResource = keycloakAdminClient.realm(keycloakProperties.getRealm());
            UserResource userResource = realmResource.users().get(user.getId());

            userResource.executeActionsEmail(List.of("VERIFY_EMAIL"));
            logger.info("Verification email resent to user: {}", username);
        } catch (Exception e) {
            logger.error("Error resending verification email for user: {}", username, e);
            throw new RuntimeException("Failed to resend verification email", e);
        }
    }
}
