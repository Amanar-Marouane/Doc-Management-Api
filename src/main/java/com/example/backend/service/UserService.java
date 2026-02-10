package com.example.backend.service;

import com.example.backend.dto.*;
import com.example.backend.entity.Societe;
import com.example.backend.entity.User;
import com.example.backend.repository.SocieteRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
        if (!SecurityUtils.isAdmin()) {
            throw new RuntimeException("Only admins can create users");
        }

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
        if (!SecurityUtils.isAdmin()) {
            throw new RuntimeException("Only admins can create users");
        }

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

    @Transactional(readOnly = true)
    public UserDTO getCurrentUser() {
        String email = SecurityUtils.getCurrentUserEmail();
        if (email == null) {
            throw new RuntimeException("No authenticated user found");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return toDTO(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!SecurityUtils.isAdmin()) {
            throw new RuntimeException("Only admins can delete users");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() == User.Role.ADMIN) {
            throw new RuntimeException("Cannot delete admin user");
        }

        userRepository.deleteById(id);
    }

    @Transactional
    public UserDTO updateUser(Long id, UpdateUserDTO request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if current user can modify this user (self or admin)
        if (!SecurityUtils.canModifyUser(user.getEmail())) {
            throw new RuntimeException("You don't have permission to update this user");
        }

        // Check if email is being changed and already exists
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("Email already exists");
            }
            user.setEmail(request.getEmail());
        }

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        User updated = userRepository.save(user);
        return toDTO(updated);
    }

    @Transactional
    public UserDTO adminUpdateUser(Long id, AdminUpdateUserDTO request) {
        if (!SecurityUtils.isAdmin()) {
            throw new RuntimeException("Only admins can perform admin updates");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Prevent modifying admin role
        if (user.getRole() == User.Role.ADMIN && request.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("Cannot change admin user role");
        }

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("Email already exists");
            }
            user.setEmail(request.getEmail());
        }

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getRole() != null) {
            user.setRole(request.getRole());

            // Handle societe assignment based on role
            if (request.getRole() == User.Role.SOCIETE) {
                if (request.getSocieteId() != null) {
                    Societe societe = societeRepository.findById(request.getSocieteId())
                            .orElseThrow(() -> new RuntimeException("Societe not found"));
                    user.setSociete(societe);
                }
            } else {
                user.setSociete(null);
            }
        }

        if (request.getActive() != null) {
            if (user.getRole() == User.Role.ADMIN) {
                throw new RuntimeException("Cannot deactivate admin user");
            }
            user.setActive(request.getActive());
        }

        User updated = userRepository.save(user);
        return toDTO(updated);
    }

    @Transactional
    public UserDTO updateUserStatus(Long id, UpdateUserStatusDTO request) {
        if (!SecurityUtils.isAdmin()) {
            throw new RuntimeException("Only admins can change user status");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() == User.Role.ADMIN) {
            throw new RuntimeException("Cannot deactivate admin user");
        }

        user.setActive(request.isActive());
        User updated = userRepository.save(user);
        return toDTO(updated);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserDTO> getUsersWithFilters(User.Role role, Long societeId, Boolean active,
            String search, int page, int size, String sortBy) {
        Sort sort = Sort.by(Sort.Direction.DESC, sortBy != null ? sortBy : "createdAt");
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<User> userPage = userRepository.findByFilters(role, societeId, active, search, pageable);

        List<UserDTO> userDTOs = userPage.getContent().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return PageResponse.<UserDTO>builder()
                .content(userDTOs)
                .page(userPage.getNumber())
                .size(userPage.getSize())
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .last(userPage.isLast())
                .first(userPage.isFirst())
                .build();
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
