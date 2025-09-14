package org.example.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.auth.dto.*;
import org.example.auth.security.SecurityConfig;
import org.example.auth.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    @DisplayName("register: success")
    void register_success() throws Exception {
        UserRegisterRequest req = UserRegisterRequest.builder()
                .username("john123")
                .email("john@example.com")
                .password("secretPW1")
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(OffsetDateTime.now().minusYears(20))
                .roles(Set.of("ROLE_USER"))
                .build();

        UserResponse resp = UserResponse.builder()
                .id("kc-id-1")
                .username("john123")
                .email("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(req.getDateOfBirth())
                .roles(Set.of("ROLE_USER"))
                .build();

        when(userService.register(any(UserRegisterRequest.class))).thenReturn(resp);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("kc-id-1"))
                .andExpect(jsonPath("$.username").value("john123"))
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    @DisplayName("register: validation failure returns 400")
    void register_validationFailure() throws Exception {
        // Missing required fields (username blank, invalid email, short password)
        String invalidJson = "{" +
                "\"username\":\"\"," +
                "\"email\":\"bad\"," +
                "\"password\":\"123\"," +
                "\"firstName\":\"\"," +
                "\"lastName\":\"\"," +
                "\"dateOfBirth\":\"2020-01-01T00:00:00Z\"}"; // roles omitted

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());

        Mockito.verify(userService, Mockito.never()).register(any());
    }

    @Test
    @DisplayName("login: success")
    void login_success() throws Exception {
        UserLoginRequest loginReq = UserLoginRequest.builder()
                .username("john123")
                .password("secretPW1")
                .build();

        AuthResponse authResp = AuthResponse.builder()
                .token("jwt-token")
                .id("kc-id-1")
                .username("john123")
                .email("john@example.com")
                .roles(Set.of("ROLE_USER"))
                .build();

        when(userService.login(any(UserLoginRequest.class))).thenReturn(authResp);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.username").value("john123"));
    }

    @Test
    @DisplayName("/me: requires authentication and role")
    @WithMockUser(username = "john123", roles = {"USER"})
    void me_success() throws Exception {
        UserResponse resp = UserResponse.builder()
                .id("kc-id-1")
                .username("john123")
                .email("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(OffsetDateTime.now().minusYears(20))
                .roles(Set.of("ROLE_USER"))
                .build();
        when(userService.getCurrentUser()).thenReturn(resp);

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("john123"));
    }

    @Test
    @DisplayName("/me: unauthorized when no auth")
    void me_unauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}
