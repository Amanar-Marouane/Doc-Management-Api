package com.example.backend.controller;

import com.example.backend.dto.*;
import com.example.backend.service.SocieteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/societes")
@RequiredArgsConstructor
public class SocieteController {

    private final SocieteService societeService;

    /**
     * Create a new Societe - ADMIN ONLY
     */
    @PostMapping
    public ResponseEntity<SocieteDTO> createSociete(@RequestBody SocieteRequestDTO request) {
        SocieteDTO created = societeService.createSociete(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Get all Societes - ADMIN ONLY
     */
    @GetMapping
    public ResponseEntity<List<SocieteDTO>> getAllSocietes() {
        List<SocieteDTO> societes = societeService.getAllSocietes();
        return ResponseEntity.ok(societes);
    }

    /**
     * Get Societe by ID - ADMIN ONLY
     */
    @GetMapping("/{id}")
    public ResponseEntity<SocieteDTO> getSocieteById(@PathVariable Long id) {
        SocieteDTO societe = societeService.getSocieteById(id);
        return ResponseEntity.ok(societe);
    }

    /**
     * Get Societe compliance overview
     */
    @GetMapping("/{id}/compliance")
    public ResponseEntity<SocietComplianceOverviewDTO> getSocietComplianceOverview(@PathVariable Long id) {
        SocietComplianceOverviewDTO overview = societeService.getSocietComplianceOverview(id);
        return ResponseEntity.ok(overview);
    }

    /**
     * Assign accountant to societe - ADMIN ONLY
     */
    @PostMapping("/{societeId}/accountant/{accountantId}")
    public ResponseEntity<SocieteDTO> assignAccountantToSociete(@PathVariable Long societeId,
            @PathVariable Long accountantId) {
        SocieteDTO updated = societeService.assignAccountantToSociete(societeId, accountantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(updated);
    }

    /**
     * Update accountant for societe - ADMIN ONLY
     */
    @PutMapping("/{societeId}/accountant/{accountantId}")
    public ResponseEntity<SocieteDTO> updateAccountantForSociete(@PathVariable Long societeId,
            @PathVariable Long accountantId) {
        SocieteDTO updated = societeService.updateAccountantForSociete(societeId, accountantId);
        return ResponseEntity.ok(updated);
    }

    /**
     * Remove accountant from societe - ADMIN ONLY
     */
    @DeleteMapping("/{societeId}/accountant")
    public ResponseEntity<Void> removeAccountantFromSociete(@PathVariable Long societeId) {
        societeService.removeAccountantFromSociete(societeId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get societies for an accountant
     */
    @GetMapping("/accountant/{accountantId}")
    public ResponseEntity<List<SocieteDTO>> getSocietiesByAccountant(@PathVariable Long accountantId) {
        List<SocieteDTO> societes = societeService.getSocietiesByAccountant(accountantId);
        return ResponseEntity.ok(societes);
    }

    /**
     * Update Societe - ADMIN ONLY
     */
    @PutMapping("/{id}")
    public ResponseEntity<SocieteDTO> updateSociete(@PathVariable Long id, @RequestBody SocieteRequestDTO request) {
        SocieteDTO updated = societeService.updateSociete(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete Societe - ADMIN ONLY
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSociete(@PathVariable Long id) {
        societeService.deleteSociete(id);
        return ResponseEntity.noContent().build();
    }
}
