package com.example.backend.service;

import com.example.backend.contract.FileStorageService;
import com.example.backend.contract.FileValidatorContract;
import com.example.backend.dto.DocumentResponseDTO;
import com.example.backend.dto.DocumentValidationDTO;
import com.example.backend.entity.Document;
import com.example.backend.entity.Societe;
import com.example.backend.entity.User;
import com.example.backend.exception.BusinessException;
import com.example.backend.repository.DocumentRepository;
import com.example.backend.repository.SocieteRepository;
import com.example.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SocieteRepository societeRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private FileValidatorContract fileValidator;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private DocumentService documentService;

    @Test
    void validateDocument_ShouldApprove_WhenActionIsValidate() {
        // Arrange
        Long documentId = 1L;
        User validator = User.builder().id(2L).fullName("Comptable User").build();
        DocumentValidationDTO validation = new DocumentValidationDTO();
        validation.setAction(DocumentValidationDTO.Action.VALIDER);
        validation.setCommentaire("Looks good");

        Societe societe = Societe.builder().raisonSociale("Test Soc").build();
        User uploader = User.builder().fullName("Client User").build();

        Document document = Document.builder()
                .id(documentId)
                .statut(Document.StatutDocument.EN_ATTENTE)
                .numeroPiece("BILL-001")
                .societe(societe)
                .uploadedBy(uploader)
                .build();

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        DocumentResponseDTO result = documentService.validateDocument(documentId, validation, validator);

        // Assert
        assertEquals(Document.StatutDocument.VALIDE, result.getStatut());
        assertEquals("Looks good", result.getCommentaireComptable());
        assertNotNull(document.getRetentionExpiresAt());
        verify(auditLogService).logValidation(any(Document.class), eq(validator));
    }

    @Test
    void validateDocument_ShouldReject_WhenActionIsRejectAndCommentProvided() {
        // Arrange
        Long documentId = 1L;
        User validator = User.builder().id(2L).fullName("Comptable User").build();
        DocumentValidationDTO validation = new DocumentValidationDTO();
        validation.setAction(DocumentValidationDTO.Action.REJETER);
        validation.setCommentaire("Incomplete piece");

        Societe societe = Societe.builder().raisonSociale("Test Soc").build();
        User uploader = User.builder().fullName("Client User").build();

        Document document = Document.builder()
                .id(documentId)
                .statut(Document.StatutDocument.EN_ATTENTE)
                .societe(societe)
                .uploadedBy(uploader)
                .build();

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        DocumentResponseDTO result = documentService.validateDocument(documentId, validation, validator);

        // Assert
        assertEquals(Document.StatutDocument.REJETE, result.getStatut());
        assertEquals("Incomplete piece", result.getCommentaireComptable());
        verify(auditLogService).logRejection(any(Document.class), eq(validator), eq("Incomplete piece"));
    }

    @Test
    void validateDocument_ShouldThrowException_WhenAlreadyProcessed() {
        // Arrange
        Long documentId = 1L;
        Document document = Document.builder()
                .id(documentId)
                .statut(Document.StatutDocument.VALIDE)
                .build();

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        DocumentValidationDTO validation = new DocumentValidationDTO();

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            documentService.validateDocument(documentId, validation, null);
        });
        assertEquals("ALREADY_PROCESSED", exception.getCode());
    }

    @Test
    void deleteDocument_ShouldSucceed_WhenPending() {
        // Arrange
        Long documentId = 1L;
        User deletedBy = User.builder().role(User.Role.CLIENT).build();
        Document document = Document.builder()
                .id(documentId)
                .statut(Document.StatutDocument.EN_ATTENTE)
                .build();

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));

        // Act
        documentService.deleteDocument(documentId, deletedBy);

        // Assert
        assertEquals(Document.StatutDocument.SUPPRIME, document.getStatut());
        verify(documentRepository).save(document);
    }

    @Test
    void deleteDocument_ShouldThrowException_WhenValidated() {
        // Arrange
        Long documentId = 1L;
        User deletedBy = User.builder().role(User.Role.ADMIN).build();
        Document document = Document.builder()
                .id(documentId)
                .statut(Document.StatutDocument.VALIDE)
                .numeroPiece("PIECE-123")
                .build();

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            documentService.deleteDocument(documentId, deletedBy);
        });
        assertEquals("NOT_DELETABLE", exception.getCode());
    }
}
