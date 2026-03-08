package com.example.backend.controller;

import com.example.backend.dto.MissingFileWarningDTO;
import com.example.backend.entity.MissingFileWarning;
import com.example.backend.repository.MissingFileWarningRepository;
import com.example.backend.scheduler.MissingFileCheckJob;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/warnings/missing-files")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class MissingFileWarningController {

    private final MissingFileWarningRepository warningRepository;
    private final MissingFileCheckJob missingFileCheckJob;

    /**
     * GET /api/v1/warnings/missing-files
     * Returns all recorded missing-file warnings (admin only).
     */
    @GetMapping
    public ResponseEntity<List<MissingFileWarningDTO>> getMissingFileWarnings() {
        List<MissingFileWarningDTO> dtos = warningRepository.findAllWithDocumentAndSociete()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * POST /api/v1/warnings/missing-files/run
     * Manually triggers the check outside the nightly schedule (admin only).
     */
    @PostMapping("/run")
    public ResponseEntity<List<MissingFileWarningDTO>> triggerCheck() {
        missingFileCheckJob.checkMissingFiles();
        return getMissingFileWarnings();
    }

    private MissingFileWarningDTO toDTO(MissingFileWarning w) {
        return MissingFileWarningDTO.builder()
                .id(w.getId())
                .documentId(w.getDocument().getId())
                .numeroPiece(w.getDocument().getNumeroPiece())
                .typeDocument(w.getDocument().getTypeDocument().name())
                .statut(w.getDocument().getStatut().name())
                .exerciceComptable(w.getDocument().getExerciceComptable())
                .societeId(w.getDocument().getSociete().getId())
                .raisonSociale(w.getDocument().getSociete().getRaisonSociale())
                .expectedPath(w.getExpectedPath())
                .detectedAt(w.getDetectedAt())
                .build();
    }
}
