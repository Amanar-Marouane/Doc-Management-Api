package com.example.backend.seeder;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.backend.entity.User;
import com.example.backend.entity.User.Role;
import com.example.backend.repository.UserRepository;
import com.example.backend.util.AppLogger;

public class AdminSeeder {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void seedAdmin() {
        String email = "admin@gmail.com";
        String rawPassword = "password";

        User existing = userRepository.findByEmail(email).orElse(null);
        if (existing == null) {
            User user = new User();
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(rawPassword));
            user.setFullName("Administrator");
            user.setRole(Role.ADMIN);
            user.setActive(true);
            userRepository.save(user);
            AppLogger.success("Seeded admin user: " + email);
            return;
        }

        if (!passwordEncoder.matches(rawPassword, existing.getPassword())) {
            existing.setPassword(passwordEncoder.encode(rawPassword));
            userRepository.save(existing);
            AppLogger.warn("Updated admin password hash: " + email);
        } else {
            AppLogger.info("Admin user already exists: " + email);
        }
    }
}