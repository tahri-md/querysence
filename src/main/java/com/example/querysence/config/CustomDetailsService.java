package com.example.querysence.config;

import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.querysence.model.User;
import com.example.querysence.model.UserRole;
import com.example.querysence.model.dto.CustomUserDetails;
import com.example.querysence.repository.UserRepository;

@Service
public class CustomDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user =  userRepository.findByFullName(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Collection<? extends GrantedAuthority> authorities = getRoles(user.getRole());

        return new CustomUserDetails(
                user.getFullName(),
                user.getEmail(),
                user.getPassword(),
                authorities
        );
    }

    private Collection<? extends GrantedAuthority> getRoles(UserRole role) {
        return List.of(new SimpleGrantedAuthority("ROLE_"+role.name()));
    }
}
