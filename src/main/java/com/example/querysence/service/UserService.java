package com.example.querysence.service;

import java.security.AuthProvider;
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
        Authentication auth = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                authRequest.getUsername(),
                authRequest.getPassword()
            )
        );

        UserDto user =  mapUserToDto(userRepository.findByFullName(authRequest.getUsername()).orElseThrow());
        String jwt = jwtService.generateToken(authRequest.getUsername());
        return AuthResponse.builder()
                            .accessToken(jwt)
                            .user(user)
                            .build();
    } catch (Exception e) {
        throw new RuntimeException("Login failed: " + e.getMessage());
    }
}
public UserDto getMyInfos(Authentication authentication) {
    String username = authentication.getName();
    return mapUserToDto(userRepository.findByFullName(username)
                                        .orElseThrow());
}


   public UserDto register(UserRegister registredUser) {
    User user = new User();
    user.setFullName(registredUser.getUsername());
    user.setEmail(registredUser.getEmail());
    user.setPassword(new BCryptPasswordEncoder().encode(registredUser.getPassword()));
    user.setIsActive(true);
    user.setRole(UserRole.VIEWER);
    user.setCreatedAt(LocalDateTime.now());
    user.setUpdatedAt(LocalDateTime.now());

    User saved = userRepository.save(user);
    return mapUserToDto(saved);
}

    public UserDto mapUserToDto(User user){
        return UserDto.builder()
                        .email(user.getEmail())
                        .username(user.getFullName())
                        .is_active(user.getIsActive())
                        .build();
    }
}
