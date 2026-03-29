package com.example.backend.dto;

public record BackupTaskResult(
        String taskName,
        boolean success,
        String message,
        long durationMs) {
}