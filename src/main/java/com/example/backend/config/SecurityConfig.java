package com.example.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.example.backend.constants.Routes;
import com.example.backend.contract.JwtContract;
import com.example.backend.filter.JwtFilter;
import com.example.backend.filter.LoginFilter;
import com.example.backend.filter.LogoutAuthFilter;
import com.example.backend.security.CustomUserDetailsService;
import com.example.backend.service.JwtBlacklistService;

import lombok.AllArgsConstructor;

@Configuration
@AllArgsConstructor
public class SecurityConfig {

    JwtContract jwtService;
    CustomUserDetailsService userDetailsService;
    JwtBlacklistService jwtBlacklistService;
    JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain basicAuthSecurityFilterChain(HttpSecurity http,
            AuthenticationConfiguration authConfig) throws Exception {

        // Create LoginFilter
        LoginFilter loginFilter = new LoginFilter(authenticationManager(authConfig), jwtService);

        // Create LogoutFilter
        LogoutAuthFilter logoutFilter = new LogoutAuthFilter(jwtBlacklistService);

        return http
                .securityMatcher("/api/**")
                .authorizeHttpRequests(request -> {
                    Routes.open_routes.forEach(pr -> request.requestMatchers(pr).permitAll());
                    request.anyRequest().authenticated();
                })
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(loginFilter, UsernamePasswordAuthenticationFilter.class) // Login filter
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class) // JWT filter
                .addFilterBefore(logoutFilter, UsernamePasswordAuthenticationFilter.class) // Logout filter
                .build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
