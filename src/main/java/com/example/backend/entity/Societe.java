package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "societes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Societe {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String raisonSociale;

    @Column(nullable = false, unique = true, length = 15)
    private String ice;

    @Column(nullable = false, length = 500)
    private String adresse;

    @Column(nullable = false, length = 20)
    private String telephone;

    @Column(nullable = false)
    private String emailContact;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
