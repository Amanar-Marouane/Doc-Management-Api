package com.example.backend.contract;

import com.example.backend.dto.*;
import com.example.backend.entity.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentServiceContract {
    DocumentResponseDTO uploadDocument(DocumentUploadDTO dto, MultipartFile file, User user);

    List<DocumentResponseDTO> getDocumentsBySocieteAndExercice(Long societeId, Integer exercice);

    List<DocumentResponseDTO> getAllPendingDocuments();

    List<DocumentResponseDTO> getPendingDocumentsByExercice(Integer exercice);

    DocumentResponseDTO validateDocument(Long documentId, DocumentValidationDTO validation, User comptable);

    DocumentResponseDTO getDocumentById(Long id);

    List<DocumentResponseDTO> getDocumentsBySociete(Long societeId);

    byte[] downloadDocument(Long documentId);
}
