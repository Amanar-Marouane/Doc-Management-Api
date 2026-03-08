package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditAction action;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by_user_id", nullable = false)
    private User performedBy;

    @Column(nullable = false)
    private LocalDateTime performedAt;

    @Column(length = 1000)
    private String details;

    @PrePersist
    protected void onCreate() {
        performedAt = LocalDateTime.now();
    }

    public enum AuditAction {
        DOCUMENT_UPLOADED,
        DOCUMENT_VALIDATED,
        DOCUMENT_REJECTED,
        DOCUMENT_DELETED
    }
}
