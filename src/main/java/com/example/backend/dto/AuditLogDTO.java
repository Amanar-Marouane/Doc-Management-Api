package com.example.backend.dto;

import com.example.backend.entity.AuditLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogDTO {

    private Long id;
    private AuditLog.AuditAction action;
    private Long documentId;
    private String documentNumeroPiece;
    private Long performedById;
    private String performedByName;
    private LocalDateTime performedAt;
    private String details;
}
