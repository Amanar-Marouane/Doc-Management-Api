package com.example.backend.controller;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.backend.contract.DocumentServiceContract;
import com.example.backend.dto.DocumentResponseDTO;
import com.example.backend.dto.DocumentUploadDTO;
import com.example.backend.entity.User;
import com.example.backend.security.CustomUserDetails;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {
    private final DocumentServiceContract documentService;

    @GetMapping
    public ResponseEntity<List<DocumentResponseDTO>> getAllMyDocuments(
            @AuthenticationPrincipal UserDetails userDetails) {

        CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;
        User user = customUserDetails.getUser();
        if (user.getSociete() == null) {
            return ResponseEntity.badRequest().build();
        }

        List<DocumentResponseDTO> documents = documentService.getDocumentsBySociete(
                user.getSociete().getId());

        return ResponseEntity.ok(documents);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponseDTO> uploadDocument(
            @Valid @ModelAttribute DocumentUploadDTO dto,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {

        CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;
        User user = customUserDetails.getUser();
        DocumentResponseDTO response = documentService.uploadDocument(dto, file, user);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/exercice/{exercice}")
    public ResponseEntity<List<DocumentResponseDTO>> getDocumentsByExercice(
            @PathVariable Integer exercice,
            @AuthenticationPrincipal UserDetails userDetails) {

        CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;
        User user = customUserDetails.getUser();
        if (user.getSociete() == null) {
            return ResponseEntity.badRequest().build();
        }

        List<DocumentResponseDTO> documents = documentService.getDocumentsBySocieteAndExercice(
                user.getSociete().getId(), exercice);

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
}
