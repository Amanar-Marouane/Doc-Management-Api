package com.example.backend.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.backend.contract.DocumentServiceContract;
import com.example.backend.dto.AuditLogDTO;
import com.example.backend.dto.DocumentResponseDTO;
import com.example.backend.dto.DocumentUploadDTO;
import com.example.backend.dto.DocumentValidationDTO;
import com.example.backend.entity.Document;
import com.example.backend.entity.User;
import com.example.backend.security.CustomUserDetails;
import com.example.backend.service.AuditLogService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentServiceContract documentService;
    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<Page<DocumentResponseDTO>> getAllMyDocuments(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) Document.StatutDocument statut,
            @RequestParam(required = false) Document.TypeDocument typeDocument,
            @RequestParam(required = false) Integer exerciceComptable,
            @RequestParam(required = false) String numeroPiece,
            @RequestParam(required = false) String fournisseur,
            @RequestParam(required = false) LocalDate datePieceFrom,
            @RequestParam(required = false) LocalDate datePieceTo) {

        User user = extractUser(userDetails);

        Page<DocumentResponseDTO> documents = documentService.getDocumentsBySocietePaginatedFiltered(
                user.getId(),
                page,
                size,
                sortBy,
                sortDir,
                statut,
                typeDocument,
                exerciceComptable,
                numeroPiece,
                fournisseur,
                datePieceFrom,
                datePieceTo);

        return ResponseEntity.ok(documents);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponseDTO> uploadDocument(
            @Valid @ModelAttribute DocumentUploadDTO dto,
            @RequestParam("file") MultipartFile file,
            @RequestParam("societeId") String societeId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User uploadedBy = extractUser(userDetails);
        DocumentResponseDTO response = documentService.uploadDocument(dto, file, societeId, uploadedBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/exercice/{exercice}")
    public ResponseEntity<List<DocumentResponseDTO>> getDocumentsByExercice(
            @PathVariable Integer exercice,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = extractUser(userDetails);
        List<DocumentResponseDTO> documents = documentService.getDocumentsBySocieteAndExercice(
                user.getId(), exercice);
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponseDTO> getDocument(@PathVariable Long id) {
        DocumentResponseDTO document = documentService.getDocumentById(id);
        return ResponseEntity.ok(document);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable Long id) {
        byte[] fileContent = documentService.downloadDocument(id);
        DocumentResponseDTO document = documentService.getDocumentById(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", document.getNomFichierOriginal());

        return ResponseEntity.ok()
                .headers(headers)
                .body(fileContent);
    }

    @GetMapping("/pending")
    public ResponseEntity<List<DocumentResponseDTO>> getPendingDocuments(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = extractUser(userDetails);
        List<DocumentResponseDTO> documents = documentService.getPendingDocumentsForCurrentUser(user);
        return ResponseEntity.ok(documents);
    }

    /**
     * Validate or reject a document — ADMIN only.
     */
    @PostMapping("/{id}/validate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DocumentResponseDTO> validateDocument(
            @PathVariable Long id,
            @Valid @RequestBody DocumentValidationDTO validation,
            @AuthenticationPrincipal UserDetails userDetails) {

        User admin = extractUser(userDetails);
        DocumentResponseDTO response = documentService.validateDocument(id, validation, admin);
        return ResponseEntity.ok(response);
    }

    /**
     * Get audit trail for a document — ADMIN only.
     */
    @GetMapping("/{id}/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AuditLogDTO>> getAuditLogs(@PathVariable Long id) {
        List<AuditLogDTO> logs = auditLogService.getAuditLogsForDocument(id);
        return ResponseEntity.ok(logs);
    }

    /**
     * Soft-delete a document — marks it as SUPPRIME (file is NOT removed).
     * Rules enforced by the service:
     *   EN_ATTENTE → any authenticated user
     *   REJETE     → ADMIN only
     *   VALIDE     → NOT allowed (use purge after document reaches SUPPRIME status)
     *   SUPPRIME   → error (already deleted)
     * Returns 204 No Content on success.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteDocument(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = extractUser(userDetails);
        documentService.deleteDocument(id, user);
        return ResponseEntity.noContent().build();
    }

    /**
     * Permanently purge a document — ADMIN only.
     * Requires the document to be in SUPPRIME status.
     * Clears all audit logs, removes the physical file, then deletes the DB record.
     * Returns 204 No Content on success.
     */
    @DeleteMapping("/{id}/purge")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> purgeDocument(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        User admin = extractUser(userDetails);
        documentService.purgeDocument(id, admin);
        return ResponseEntity.noContent().build();
    }

    private User extractUser(UserDetails userDetails) {
        return ((CustomUserDetails) userDetails).getUser();
    }
}
