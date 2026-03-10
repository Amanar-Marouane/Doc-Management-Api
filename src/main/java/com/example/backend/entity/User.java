package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

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

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // COMPTABLE users: list of societes assigned to manage
    @OneToMany(mappedBy = "accountant")
    private List<Societe> societes;

    // CLIENT users: the single societe they belong to
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_societe_id", nullable = true)
    private Societe clientSociete;

    public enum Role {
        COMPTABLE,
        ADMIN,
        CLIENT
    }

    public Collection<String> getAuthorities() {
        return switch (role) {
            case COMPTABLE -> List.of("ROLE_COMPTABLE");
            case ADMIN -> List.of("ROLE_ADMIN");
            case CLIENT -> List.of("ROLE_CLIENT");
        };
    }
}
