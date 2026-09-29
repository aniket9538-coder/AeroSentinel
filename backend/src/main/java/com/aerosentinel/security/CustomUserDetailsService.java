package com.aerosentinel.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String role = "ROLE_CITIZEN";
        if (username != null) {
            String lower = username.toLowerCase();
            if (lower.contains("authority")) {
                role = "ROLE_AUTHORITY";
            } else if (lower.contains("admin")) {
                role = "ROLE_ADMIN";
            } else if (lower.contains("analyst")) {
                role = "ROLE_ANALYST";
            }
        }
        return new User(
                username,
                "{noop}password",
                Collections.singletonList(new SimpleGrantedAuthority(role))
        );
    }
}
