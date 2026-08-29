package com.example.querysence.service;

import com.example.querysence.model.User;

import jakarta.transaction.Transactional;

import lombok.RequiredArgsConstructor;

import com.example.querysence.repository.UserRepository;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public User getOrCreateUser(String keycloakUserId, String username, String email) {

        return userRepository
                .findByKeycloakUserId(keycloakUserId)
                .orElseGet(() -> {

                    User user = new User();
                    user.setKeycloakUserId(keycloakUserId);
                    user.setUsername(username);
                    user.setEmail(email);

                    return userRepository.save(user);
                });
    }
}