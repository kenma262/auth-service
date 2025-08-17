package org.example.auth.service;

import org.example.auth.dto.UserLoginRequest;
import org.example.auth.dto.UserRegisterRequest;
import org.example.auth.dto.UserResponse;
import org.example.auth.dto.AuthResponse;

public interface UserService {
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    UserResponse register(UserRegisterRequest request);
    AuthResponse login(UserLoginRequest request);
    UserResponse getCurrentUser();
    void resendVerificationEmail(String username);
}
