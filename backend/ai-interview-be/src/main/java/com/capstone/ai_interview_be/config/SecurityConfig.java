package com.capstone.ai_interview_be.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Disable CSRF for testing
            .authorizeHttpRequests(authz -> authz
                .anyRequest().permitAll() // Allow all requests without authentication
            )
            .headers(headers -> headers
                .frameOptions().disable() // For H2 console if needed
            );
        
        return http.build();
    }
}