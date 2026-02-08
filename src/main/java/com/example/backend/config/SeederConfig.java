package com.example.backend.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.backend.repository.UserRepository;
import com.example.backend.seeder.AdminSeeder;

import lombok.AllArgsConstructor;

@Configuration
@AllArgsConstructor
public class SeederConfig {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public AdminSeeder adminSeeder() {
        return new AdminSeeder(userRepository, passwordEncoder);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seedAdmin() {
        adminSeeder().seedAdmin();
    }
}