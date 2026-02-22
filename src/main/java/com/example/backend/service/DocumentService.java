package com.example.backend.service;

import lombok.RequiredArgsConstructor;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.backend.contract.DocumentServiceContract;
import com.example.backend.contract.FileStorageService;
import com.example.backend.contract.FileValidatorContract;
import com.example.backend.dto.DocumentResponseDTO;
import com.example.backend.dto.DocumentUploadDTO;
import com.example.backend.dto.DocumentValidationDTO;
import com.example.backend.entity.Document;
import com.example.backend.entity.Societe;
import com.example.backend.entity.User;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.DocumentRepository;
import com.example.backend.repository.SocieteRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentService implements DocumentServiceContract {

    private final DocumentRepository documentRepository;
    private final SocieteRepository societeRepository;
    private final FileStorageService fileStorageService;
    private final FileValidatorContract fileValidator;

    @Transactional
    public DocumentResponseDTO uploadDocument(DocumentUploadDTO dto, MultipartFile file, User user) {
        // Validate file
        fileValidator.validate(file);

        // Check if numero piece already exists
        documentRepository.findByNumeroPiece(dto.getNumeroPiece()).ifPresent(doc -> {
            throw new BusinessException("DUPLICATE_DOCUMENT",
                    String.format("Un document avec le numéro de pièce '%s' existe déjà", dto.getNumeroPiece()));
        });

        // Get societe
        Societe societe = user.getSociete();
        if (societe == null) {
            throw new BusinessException("NO_SOCIETE",
                    "L'utilisateur n'est associé à aucune société");
        }

        // Save file
        String savedFilePath = fileStorageService.save(
                file,
                societe.getId(),
                dto.getExerciceComptable());

        // Create document
        Document document = Document.builder()
                .numeroPiece(dto.getNumeroPiece())
                .typeDocument(dto.getTypeDocument())
                .categorieComptable(dto.getCategorieComptable())
                .datePiece(dto.getDatePiece())
                .montant(dto.getMontant())
                .fournisseur(dto.getFournisseur())
                .cheminFichier(savedFilePath)
                .nomFichierOriginal(file.getOriginalFilename())
                .statut(Document.StatutDocument.EN_ATTENTE)
                .societe(societe)
                .uploadedBy(user)
                .exerciceComptable(dto.getExerciceComptable())
                .build();

        try {
            Document saved = documentRepository.save(document);

            return mapToDTO(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(
                    "DUPLICATE_DOCUMENT",
                    String.format("Un document avec le numéro '%s' existe déjà", dto.getNumeroPiece()));
        }
    }

    public List<DocumentResponseDTO> getDocumentsBySocieteAndExercice(Long societeId, Integer exercice) {
        Societe societe = societeRepository.findById(societeId)
                .orElseThrow(() -> new ResourceNotFoundException("Société", societeId.toString()));

        return documentRepository.findBySocieteAndExerciceComptable(societe, exercice)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<DocumentResponseDTO> getAllPendingDocuments() {
        return documentRepository.findByStatut(Document.StatutDocument.EN_ATTENTE)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<DocumentResponseDTO> getPendingDocumentsByExercice(Integer exercice) {
        return documentRepository.findByStatutAndExerciceComptable(Document.StatutDocument.EN_ATTENTE, exercice)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public DocumentResponseDTO validateDocument(Long documentId, DocumentValidationDTO validation, User comptable) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", documentId.toString()));

        if (document.getStatut() != Document.StatutDocument.EN_ATTENTE) {
            throw new BusinessException("ALREADY_PROCESSED",
                    String.format("Le document '%s' a déjà été traité avec le statut: %s",
                            document.getNumeroPiece(), document.getStatut()));
        }

        if (validation.getAction() == DocumentValidationDTO.Action.VALIDER) {
            document.setStatut(Document.StatutDocument.VALIDE);
            document.setCommentaireComptable(validation.getCommentaire());
        } else {
            if (validation.getCommentaire() == null || validation.getCommentaire().trim().isEmpty()) {
                throw new BusinessException("REJECTION_REASON_REQUIRED",
                        "Le motif de rejet est obligatoire pour rejeter un document");
            }
            document.setStatut(Document.StatutDocument.REJETE);
            document.setCommentaireComptable(validation.getCommentaire());
        }

        document.setDateValidation(LocalDateTime.now());
        document.setValidatedBy(comptable);

        Document updated = documentRepository.save(document);
        return mapToDTO(updated);
    }

    public DocumentResponseDTO getDocumentById(Long id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", id.toString()));
        return mapToDTO(document);
    }

    public List<DocumentResponseDTO> getDocumentsBySociete(Long societeId) {
        Societe societe = societeRepository.findById(societeId)
                .orElseThrow(() -> new ResourceNotFoundException("Société", societeId.toString()));

        return documentRepository.findBySociete(societe)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public byte[] downloadDocument(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", documentId.toString()));

        return fileStorageService.read(document.getCheminFichier());
    }

    private DocumentResponseDTO mapToDTO(Document document) {
        return DocumentResponseDTO.builder()
                .id(document.getId())
                .numeroPiece(document.getNumeroPiece())
                .typeDocument(document.getTypeDocument())
                .categorieComptable(document.getCategorieComptable())
                .datePiece(document.getDatePiece())
                .montant(document.getMontant())
                .fournisseur(document.getFournisseur())
                .nomFichierOriginal(document.getNomFichierOriginal())
                .statut(document.getStatut())
                .dateValidation(document.getDateValidation())
                .commentaireComptable(document.getCommentaireComptable())
                .societeRaisonSociale(document.getSociete().getRaisonSociale())
                .uploadedByName(document.getUploadedBy().getFullName())
                .validatedByName(document.getValidatedBy() != null ? document.getValidatedBy().getFullName() : null)
                .exerciceComptable(document.getExerciceComptable())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .cheminFichier(document.getCheminFichier())
                .build();
    }
}
