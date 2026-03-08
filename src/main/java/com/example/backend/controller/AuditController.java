package com.example.backend.controller;

import com.example.backend.dto.AuditLogDTO;
import com.example.backend.entity.AuditLog;
import com.example.backend.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {

    private final AuditLogService auditLogService;

    /**
     * GET /api/v1/audit-logs
     *
     * Returns a paginated, filterable list of all audit log entries.
     * ADMIN only.
     *
     * Query params:
     * action – filter by AuditAction enum value
     * documentId – filter by document ID
     * performedById – filter by user ID who performed the action
     * from – filter entries on or after this datetime (ISO format)
     * to – filter entries on or before this datetime (ISO format)
     * page – zero-based page index (default 0)
     * size – page size (default 20)
     * sortBy – field to sort by (default: performedAt)
     * sortDir – asc or desc (default: desc)
     */
    @GetMapping
    public ResponseEntity<Page<AuditLogDTO>> getAuditLogs(
            @RequestParam(required = false) AuditLog.AuditAction action,
            @RequestParam(required = false) Long documentId,
            @RequestParam(required = false) Long performedById,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "performedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Page<AuditLogDTO> result = auditLogService.getAuditLogsPaginated(
                action, documentId, performedById, from, to, page, size, sortBy, sortDir);

        return ResponseEntity.ok(result);
    }
}
