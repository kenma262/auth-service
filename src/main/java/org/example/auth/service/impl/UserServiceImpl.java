package org.example.auth.service.impl;

import org.example.auth.dto.UserLoginRequest;
import org.example.auth.dto.UserRegisterRequest;
import org.example.auth.dto.UserResponse;
import org.example.auth.dto.AuthResponse;
import org.example.auth.service.KeycloakClient;
import org.example.auth.service.UserService;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private final KeycloakClient keycloakClient;

    @Autowired
    public UserServiceImpl(KeycloakClient keycloakClient) {
        this.keycloakClient = keycloakClient;
    }

    @Override
    public boolean existsByUsername(String username) {
        UserRepresentation user = keycloakClient.getUserByUsername(username);
        return user != null;
    }

    @Override
    public boolean existsByEmail(String email) {
        // For simplicity, we'll check by username. In a real scenario, you might want to search by email
        return false; // Keycloak handles email uniqueness internally
    }

    @Override
    public UserResponse register(UserRegisterRequest request) {
        // Check if user already exists first
        if (existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        // Set default role if none provided
        Set<String> roles = (request.getRoles() == null || request.getRoles().isEmpty())
            ? Set.of("ROLE_USER") : request.getRoles();

        try {
            // Create user in Keycloak with new fields
            String userId = keycloakClient.createUser(
                request.getUsername(),
                request.getEmail(),
                request.getPassword(),
                request.getFirstName(),
                request.getLastName(),
                request.getDateOfBirth(),
                roles
            );

            logger.info("User registered: username={}, firstName={}, lastName={}, roles={}",
                request.getUsername(), request.getFirstName(), request.getLastName(), roles);

            return UserResponse.builder()
                    .id(userId)
                    .username(request.getUsername())
                    .email(request.getEmail())
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .dateOfBirth(request.getDateOfBirth())
                    .roles(roles)
                    .build();
        } catch (Exception e) {
            logger.error("Error registering user", e);
            if (e.getMessage() != null && e.getMessage().contains("already exists")) {
                throw new RuntimeException("Username already exists");
            }
            throw new RuntimeException("Failed to register user: " + e.getMessage());
        }
    }

    @Override
    public AuthResponse login(UserLoginRequest request) {
        try {
            // Get access token from Keycloak
            String token = keycloakClient.getAccessToken(request.getUsername(), request.getPassword());

            // Get user details
            UserRepresentation user = keycloakClient.getUserByUsername(request.getUsername());
            if (user == null) {
                throw new RuntimeException("User not found");
            }

            // Get user roles
            List<String> rolesList = keycloakClient.getUserRoles(user.getId());
            Set<String> roles = Set.copyOf(rolesList);

            logger.info("User login: username={}, roles={}", user.getUsername(), roles);

            return AuthResponse.builder()
                    .token(token)
                    .id(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .roles(roles)
                    .build();
        } catch (Exception e) {
            logger.error("Error logging in user", e);
            throw new RuntimeException("Invalid username or password");
        }
    }

    @Override
    public UserResponse getCurrentUser() {
        try {
            // Get JWT from security context
            Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            String username = jwt.getClaimAsString("preferred_username");

            if (username == null) {
                username = jwt.getClaimAsString("sub");
            }

            UserRepresentation user = keycloakClient.getUserByUsername(username);
            if (user == null) {
                throw new RuntimeException("User not found");
            }

            List<String> rolesList = keycloakClient.getUserRoles(user.getId());
            Set<String> roles = Set.copyOf(rolesList);

            // Extract date of birth from user attributes
            OffsetDateTime dateOfBirth = null;
            if (user.getAttributes() != null && user.getAttributes().get("dateOfBirth") != null) {
                List<String> dobValues = user.getAttributes().get("dateOfBirth");
                if (!dobValues.isEmpty()) {
                    dateOfBirth = OffsetDateTime.parse(dobValues.get(0));
                }
            }

            return UserResponse.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .dateOfBirth(dateOfBirth)
                    .roles(roles)
                    .build();
        } catch (Exception e) {
            logger.error("Error getting current user", e);
            throw new RuntimeException("Failed to get current user");
        }
    }


    @Override
    public void resendVerificationEmail(String username) {
        keycloakClient.resendVerificationEmail(username);
    }
}
