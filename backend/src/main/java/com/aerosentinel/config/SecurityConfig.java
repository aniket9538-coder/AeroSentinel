package com.aerosentinel.config;

import com.aerosentinel.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> {})
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/v1/health",
                    "/api/v1/cities/**",
                    "/api/v1/stations/**",
                    "/api/v1/sensors/**",
                    "/api/v1/air/**",
                    "/api/v1/weather/**",
                    "/api/v1/fires/**",
                    "/api/v1/satellite/**",
                    "/api/v1/grid",
                    "/api/v1/grid/**",
                    "/api/v1/hotspots/**",
                    "/api/v1/forecast/**",
                    "/api/v1/citizen/**",
                    "/api/v1/alerts/**",
                    "/api/v1/inspections/**",
                    "/api/v1/actions/**",
                    "/api/v1/monitoring/**",
                    "/api/v1/federated/**",
                    "/api/v1/auth/**",
                    "/actuator/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
