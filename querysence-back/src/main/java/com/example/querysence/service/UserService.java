package com.example.querysence.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.querysence.model.User;
import com.example.querysence.model.UserRole;
import com.example.querysence.model.dto.AuthRequest;
import com.example.querysence.model.dto.AuthResponse;
import com.example.querysence.model.dto.ChangePasswordRequest;
import com.example.querysence.model.dto.UpdateEmailRequest;
import com.example.querysence.model.dto.UpdateProfileRequest;
import com.example.querysence.model.dto.UserDto;
import com.example.querysence.model.dto.UserRegister;
import com.example.querysence.repository.UserRepository;

@Service
public class UserService {
    @Autowired
    UserRepository userRepository;
    @Autowired
    AuthenticationManager authenticationManager;
    @Autowired
    JwtService jwtService;

    public AuthResponse login(AuthRequest authRequest) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authRequest.getEmail(),
                            authRequest.getPassword()));

            UserDto user = mapUserToDto(userRepository.findByEmail(authRequest.getEmail()).orElseThrow());
            String accessToken = jwtService.generateAccessToken(authRequest.getEmail());
            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .user(user)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Login failed: " + e.getMessage());
        }
    }

    public UserDto getMyInfos(Authentication authentication) {
        String email = authentication.getName();
        return mapUserToDto(userRepository.findByEmail(email)
                .orElseThrow());
    }

    public UserDto register(UserRegister registredUser) {

        User user = User.builder()
                .fullName(registredUser.getFullName())
                .email(registredUser.getEmail())
                .password(new BCryptPasswordEncoder().encode(registredUser.getPassword()))
                .isActive(true)
                .role(UserRole.VIEWER)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        User saved = userRepository.save(user);
        return mapUserToDto(saved);
    }

    public UserDto updateMyInfos(Authentication authentication, UpdateProfileRequest request) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            user.setFullName(request.getFullName().trim());
        }

        user.setUpdatedAt(LocalDateTime.now());
        User updatedUser = userRepository.save(user);
        return mapUserToDto(updatedUser);
    }

    public void changePassword(Authentication authentication, ChangePasswordRequest request) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        if (!encoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        if (request.getNewPassword() == null || request.getNewPassword().trim().isEmpty()) {
            throw new RuntimeException("New password cannot be empty");
        }

        if (request.getNewPassword().length() < 6) {
            throw new RuntimeException("Password must be at least 6 characters");
        }

        user.setPassword(encoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    public UserDto updateEmail(Authentication authentication, UpdateEmailRequest request) {
        String currentEmail = authentication.getName();
        User user = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getNewEmail() == null || !request.getNewEmail().contains("@")) {
            throw new RuntimeException("Invalid email address");
        }

        String newEmail = request.getNewEmail().trim();

        if (newEmail.equals(currentEmail)) {
            throw new RuntimeException("New email must be different from current email");
        }

        if (userRepository.existsByEmail(newEmail)) {
            throw new RuntimeException("Email already in use");
        }

        user.setEmail(newEmail);
        user.setUpdatedAt(LocalDateTime.now());
        User updatedUser = userRepository.save(user);
        return mapUserToDto(updatedUser);
    }

    public void deleteAccount(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        userRepository.delete(user);
    }

    public UserDto mapUserToDto(User user) {
        return UserDto.builder()
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .is_active(user.getIsActive())
                .build();
    }
}
