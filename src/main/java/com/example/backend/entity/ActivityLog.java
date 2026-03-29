package com.example.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "activity_logs")
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String entityType;

    @Column
    private Long entityId;

    @Column(nullable = false)
    private String username;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public ActivityLog() {
    }

    public ActivityLog(String action, String entityType, Long entityId, String username, String description) {
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.username = username;
        this.description = description;
        this.createdAt = LocalDateTime.now();
    }
}