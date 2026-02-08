package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "societe_id")
    private Societe societe; // Null when role = COMPTABLE

    @Column(nullable = false)
    private final boolean active = true;

    private final LocalDateTime createdAt = LocalDateTime.now();

    public enum Role {
        SOCIETE,
        COMPTABLE,
        ADMIN
    }

    public Collection<String> getAuthorities() {
        return switch (role) {
            case SOCIETE -> List.of("ROLE_SOCIETE");
            case COMPTABLE -> List.of("ROLE_COMPTABLE");
            case ADMIN -> List.of("ROLE_ADMIN");
        };
    }
}
