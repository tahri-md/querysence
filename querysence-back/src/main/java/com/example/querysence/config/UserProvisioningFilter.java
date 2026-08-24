package com.example.querysence.config;


import com.example.querysence.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class UserProvisioningFilter extends OncePerRequestFilter {

    private final UserService userService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        var authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {

            String keycloakUserId =
                    jwtAuthentication
                            .getToken()
                            .getSubject();
            String username = jwtAuthentication.getToken().getClaimAsString("preferred_username");
            String email = jwtAuthentication.getToken().getClaimAsString("email");

            userService.getOrCreateUser(keycloakUserId, username, email);
        }

        filterChain.doFilter(request, response);
    }
}