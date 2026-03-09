package com.example.backend.controller;

import com.example.backend.contract.DeadlineServiceContract;
import com.example.backend.dto.DeadlineRequestDTO;
import com.example.backend.dto.DeadlineResponseDTO;
import com.example.backend.dto.PageResponse;
import com.example.backend.entity.Deadline;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/deadlines")
@RequiredArgsConstructor
public class DeadlineController {

    private final DeadlineServiceContract deadlineService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPTABLE')")
    public ResponseEntity<DeadlineResponseDTO> createDeadline(
            @Valid @RequestBody DeadlineRequestDTO request) {
        DeadlineResponseDTO created = deadlineService.createDeadline(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeadlineResponseDTO> getDeadlineById(@PathVariable Long id) {
        return ResponseEntity.ok(deadlineService.getDeadlineById(id));
    }

    @GetMapping
    public ResponseEntity<PageResponse<DeadlineResponseDTO>> getDeadlines(
            @RequestParam(required = false) Long societeId,
            @RequestParam(required = false) Deadline.DeadlineStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "dueDate") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(deadlineService.getDeadlines(societeId, status, page, size, sortBy, sortDir));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPTABLE')")
    public ResponseEntity<DeadlineResponseDTO> updateDeadline(
            @PathVariable Long id,
            @Valid @RequestBody DeadlineRequestDTO request) {
        return ResponseEntity.ok(deadlineService.updateDeadline(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPTABLE')")
    public ResponseEntity<DeadlineResponseDTO> updateDeadlineStatus(
            @PathVariable Long id,
            @RequestParam Deadline.DeadlineStatus status) {
        return ResponseEntity.ok(deadlineService.updateDeadlineStatus(id, status));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPTABLE')")
    public ResponseEntity<Void> deleteDeadline(@PathVariable Long id) {
        deadlineService.deleteDeadline(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh-overdue")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPTABLE')")
    public ResponseEntity<Void> refreshOverdueStatuses() {
        deadlineService.refreshOverdueStatuses();
        return ResponseEntity.noContent().build();
    }
}
