package com.example.backend.security;

import java.util.Optional;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;
import com.example.backend.util.AppLogger;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        AppLogger.debug(String.format("Loading user by email: %s", email));

        Optional<User> user = userRepository.findByEmail(email);

        if (user.isEmpty()) {
            AppLogger.warn(String.format("User not found with email: %s", email));
            throw new UsernameNotFoundException(String.format("Utilisateur avec l'email '%s' introuvable", email));
        }

        User foundUser = user.get();
        
        if (!foundUser.isActive()) {
            AppLogger.warn(String.format("Inactive user attempted to login: %s", email));
            throw new UsernameNotFoundException("Compte utilisateur désactivé");
        }

        AppLogger.debug(String.format("User loaded successfully: %s (Role: %s)", email, foundUser.getRole()));

        return new CustomUserDetails(foundUser);
    }
}
