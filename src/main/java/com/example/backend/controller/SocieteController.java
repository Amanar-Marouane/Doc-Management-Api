package com.example.backend.controller;

import com.example.backend.contract.SocieteServiceContract;
import com.example.backend.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/societes")
@RequiredArgsConstructor
public class SocieteController {

    private final SocieteServiceContract societeService;

    /** ADMIN only — create a new societe */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SocieteDTO> createSociete(@RequestBody SocieteRequestDTO request) {
        SocieteDTO created = societeService.createSociete(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** ADMIN only — list all societes */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SocieteDTO>> getAllSocietes() {
        List<SocieteDTO> societes = societeService.getAllSocietes();
        return ResponseEntity.ok(societes);
    }

    /** ADMIN, COMPTABLE, CLIENT — get a societe by ID */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPTABLE') or hasRole('CLIENT')")
    public ResponseEntity<SocieteDTO> getSocieteById(@PathVariable Long id) {
        SocieteDTO societe = societeService.getSocieteById(id);
        return ResponseEntity.ok(societe);
    }

    /** ADMIN, COMPTABLE, CLIENT — get compliance overview for a societe */
    @GetMapping("/{id}/compliance")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPTABLE') or hasRole('CLIENT')")
    public ResponseEntity<SocietComplianceOverviewDTO> getSocietComplianceOverview(@PathVariable Long id) {
        SocietComplianceOverviewDTO overview = societeService.getSocietComplianceOverview(id);
        return ResponseEntity.ok(overview);
    }

    /** ADMIN, COMPTABLE, CLIENT — generate PDF compliance report for a societe */
    @GetMapping("/{id}/compliance/report")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPTABLE') or hasRole('CLIENT')")
    public ResponseEntity<byte[]> generateCompliancePdfReport(@PathVariable Long id) {
        byte[] pdfReport = societeService.generateCompliancePdfReport(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "compliance_report_" + id + ".pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfReport);
    }

    /** ADMIN only — assign an accountant to a societe */
    @PostMapping("/{societeId}/accountant/{accountantId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SocieteDTO> assignAccountantToSociete(@PathVariable Long societeId,
            @PathVariable Long accountantId) {
        SocieteDTO updated = societeService.assignAccountantToSociete(societeId, accountantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(updated);
    }

    /** ADMIN only — update the accountant assigned to a societe */
    @PutMapping("/{societeId}/accountant/{accountantId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SocieteDTO> updateAccountantForSociete(@PathVariable Long societeId,
            @PathVariable Long accountantId) {
        SocieteDTO updated = societeService.updateAccountantForSociete(societeId, accountantId);
        return ResponseEntity.ok(updated);
    }

    /** ADMIN only — remove the accountant from a societe */
    @DeleteMapping("/{societeId}/accountant")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> removeAccountantFromSociete(@PathVariable Long societeId) {
        societeService.removeAccountantFromSociete(societeId);
        return ResponseEntity.noContent().build();
    }

    /** ADMIN, COMPTABLE — get societes assigned to a given accountant */
    @GetMapping("/accountant/{accountantId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPTABLE')")
    public ResponseEntity<List<SocieteDTO>> getSocietiesByAccountant(@PathVariable Long accountantId) {
        List<SocieteDTO> societes = societeService.getSocietiesByAccountant(accountantId);
        return ResponseEntity.ok(societes);
    }

    /** ADMIN only — update societe details */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SocieteDTO> updateSociete(@PathVariable Long id, @RequestBody SocieteRequestDTO request) {
        SocieteDTO updated = societeService.updateSociete(id, request);
        return ResponseEntity.ok(updated);
    }

    /** ADMIN only — delete a societe */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteSociete(@PathVariable Long id) {
        societeService.deleteSociete(id);
        return ResponseEntity.noContent().build();
    }

    /* get all societes (simple view) */
    @GetMapping("/simple")
    public ResponseEntity<List<SimpleSocieteDTO>> getAllSocietesSimple() {
        List<SimpleSocieteDTO> societes = societeService.getAllSocietesSimple();
        return ResponseEntity.ok(societes);
    }
}
