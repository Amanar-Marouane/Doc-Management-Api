package com.example.backend.service;

import com.example.backend.dto.*;
import com.example.backend.entity.Societe;
import com.example.backend.entity.User;
import com.example.backend.repository.SocieteRepository;
import com.example.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final SocieteRepository societeRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserDTO createComptable(CreateComptableDTO request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(User.Role.COMPTABLE)
                .societe(null)
                .build();

        User saved = userRepository.save(user);
        return toDTO(saved);
    }

    @Transactional
    public UserDTO createSocieteUser(CreateSocieteUserDTO request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        Societe societe = societeRepository.findById(request.getSocieteId())
                .orElseThrow(() -> new RuntimeException("Societe not found"));

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(User.Role.SOCIETE)
                .societe(societe)
                .build();

        User saved = userRepository.save(user);
        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<UserDTO> getAllComptables() {
        return userRepository.findByRole(User.Role.COMPTABLE).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserDTO> getAllSocieteUsers() {
        return userRepository.findByRole(User.Role.SOCIETE).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return toDTO(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (user.getRole() == User.Role.ADMIN) {
            throw new RuntimeException("Cannot delete admin user");
        }

        userRepository.deleteById(id);
    }

    private UserDTO toDTO(User user) {
        SocieteDTO societeDTO = null;
        if (user.getSociete() != null) {
            Societe s = user.getSociete();
            societeDTO = SocieteDTO.builder()
                    .id(s.getId())
                    .raisonSociale(s.getRaisonSociale())
                    .ice(s.getIce())
                    .adresse(s.getAdresse())
                    .telephone(s.getTelephone())
                    .emailContact(s.getEmailContact())
                    .createdAt(s.getCreatedAt())
                    .build();
        }

        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .societe(societeDTO)
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
