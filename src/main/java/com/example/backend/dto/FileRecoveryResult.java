package com.example.backend.dto;

public record FileRecoveryResult(
                boolean success,
                String expectedPath,
                String recoveredFromBackup, // zip file name it was found in, null on failure
                String message) {
}
