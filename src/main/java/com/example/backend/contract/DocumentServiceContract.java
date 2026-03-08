package com.example.backend.contract;

import com.example.backend.dto.*;
import com.example.backend.entity.Document;
import com.example.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public interface DocumentServiceContract {
    DocumentResponseDTO uploadDocument(DocumentUploadDTO dto, MultipartFile file, String societyId, User uploadedBy);

    List<DocumentResponseDTO> getDocumentsBySocieteAndExercice(Long societeId, Integer exercice);

    List<DocumentResponseDTO> getAllPendingDocuments();

    List<DocumentResponseDTO> getPendingDocumentsByExercice(Integer exercice);

    List<DocumentResponseDTO> getPendingDocumentsForCurrentUser(User user);

    DocumentResponseDTO validateDocument(Long documentId, DocumentValidationDTO validation, User comptable);

    DocumentResponseDTO getDocumentById(Long id);

    List<DocumentResponseDTO> getDocumentsBySociete(Long societeId);

    byte[] downloadDocument(Long documentId);

    Page<DocumentResponseDTO> getDocumentsBySocietePaginatedFiltered(
            Long accountantId,
            Integer page,
            Integer size,
            String sortBy,
            String sortDir,
            Document.StatutDocument statut,
            Document.TypeDocument typeDocument,
            Integer exerciceComptable,
            String numeroPiece,
            String fournisseur,
            LocalDate datePieceFrom,
            LocalDate datePieceTo);
}
