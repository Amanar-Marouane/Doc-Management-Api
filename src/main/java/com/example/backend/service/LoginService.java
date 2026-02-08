package com.example.backend.service;

import java.util.Map;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.backend.contract.LoginContract;
import com.example.backend.entity.User;
import com.example.backend.exception.InvalidCredentialsException;
import com.example.backend.repository.UserRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class LoginService implements LoginContract {

    UserRepository userRepo;
    PasswordEncoder passwordEncoder;
    JwtService jwtUtility;

    @Override
    public String Login(String email, String password) {
       Optional<User> user = userRepo.findByEmail(email);

        if (user.isEmpty() || !passwordEncoder.matches(password, user.get().getPassword())) {
            throw new InvalidCredentialsException();
        }

        return jwtUtility.generateToken(
                Map.of("role", user.get().getRole().toString()),
                email);
    }

    @Override
    public void Logout(String token) {
        jwtUtility.invalidate(token);
    }
}

