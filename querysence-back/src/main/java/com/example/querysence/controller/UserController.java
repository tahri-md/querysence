package com.example.querysence.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.querysence.model.dto.AuthRequest;
import com.example.querysence.model.dto.AuthResponse;
import com.example.querysence.model.dto.ChangePasswordRequest;
import com.example.querysence.model.dto.UpdateEmailRequest;
import com.example.querysence.model.dto.UpdateProfileRequest;
import com.example.querysence.model.dto.UserDto;
import com.example.querysence.model.dto.UserRegister;
import com.example.querysence.service.JwtService;
import com.example.querysence.service.UserService;

@RestController
@RequestMapping("/auth")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@RequestBody UserRegister registredUser) {
        return ResponseEntity.ok(userService.register(registredUser));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest authRequest) {
        return ResponseEntity.ok(userService.login(authRequest));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getMyInfos(Authentication authentication) {
        return ResponseEntity.ok(userService.getMyInfos(authentication));
    }

    @PutMapping("/me")
    public ResponseEntity<UserDto> updateMyInfos(
            Authentication authentication,
            @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateMyInfos(authentication, request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refresh(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing or invalid Authorization header"));
            }

            String refreshToken = authHeader.substring(7);

            if (jwtService.isTokenBlacklisted(refreshToken)) {
                return ResponseEntity.status(401).body(Map.of("error", "Token has been revoked"));
            }

            if (jwtService.isTokenExpired(refreshToken)) {
                return ResponseEntity.status(401).body(Map.of("error", "Refresh token expired"));
            }

            String tokenType = jwtService.getTokenType(refreshToken);
            if (!"refresh".equals(tokenType)) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid token type"));
            }

            String email = jwtService.extractUsername(refreshToken);
            String newAccessToken = jwtService.generateAccessToken(email);
            String newRefreshToken = jwtService.generateRefreshToken(email);

            return ResponseEntity.ok(Map.of(
                    "accessToken", newAccessToken,
                    "refreshToken", newRefreshToken));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid token: " + e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing or invalid Authorization header"));
            }

            String token = authHeader.substring(7);
            jwtService.blacklistToken(token);

            return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Logout failed: " + e.getMessage()));
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            Authentication authentication,
            @RequestBody ChangePasswordRequest request) {
        try {
            userService.changePassword(authentication, request);
            return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/email")
    public ResponseEntity<Map<String, Object>> updateEmail(
            Authentication authentication,
            @RequestBody UpdateEmailRequest request) {
        try {
            UserDto updatedUser = userService.updateEmail(authentication, request);
            return ResponseEntity.ok(Map.of(
                    "message", "Email updated successfully",
                    "user", updatedUser));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/me")
    public ResponseEntity<Map<String, String>> deleteAccount(Authentication authentication) {
        try {
            userService.deleteAccount(authentication);
            return ResponseEntity.ok(Map.of("message", "Account deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
