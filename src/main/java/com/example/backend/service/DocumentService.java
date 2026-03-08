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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@Service
@RequiredArgsConstructor
public class DocumentService implements DocumentServiceContract {

    private final DocumentRepository documentRepository;
    private final SocieteRepository societeRepository;
    private final FileStorageService fileStorageService;
    private final FileValidatorContract fileValidator;
    private final AuditLogService auditLogService;

    @Transactional
    public DocumentResponseDTO uploadDocument(DocumentUploadDTO dto, MultipartFile file, String societyId,
            User uploadedBy) {
        // Validate file
        fileValidator.validate(file);

        // Check if numero piece already exists
        documentRepository.findByNumeroPiece(dto.getNumeroPiece()).ifPresent(doc -> {
            throw new BusinessException("DUPLICATE_DOCUMENT",
                    String.format("Un document avec le numéro de pièce '%s' existe déjà", dto.getNumeroPiece()));
        });

        // Find societe
        Societe societe = societeRepository.findById(Long.parseLong(societyId))
                .orElseThrow(() -> new ResourceNotFoundException("Société", societyId));

        // Save file
        String savedFilePath = fileStorageService.save(
                file,
                societe.getId(),
                dto.getExerciceComptable());

        // Create document — uploadedBy is the authenticated caller
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
                .uploadedBy(uploadedBy)
                .exerciceComptable(dto.getExerciceComptable())
                .build();

        try {
            Document saved = documentRepository.save(document);
            auditLogService.logUpload(saved, uploadedBy);
            return mapToDTO(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(
                    "DUPLICATE_DOCUMENT",
                    String.format("Un document avec le numéro '%s' existe déjà", dto.getNumeroPiece()));
        }
    }

    public List<DocumentResponseDTO> getDocumentsBySocieteAndExercice(Long accoutantId, Integer exercice) {
        List<Societe> societes = societeRepository.findByAccountantId(accoutantId);
        if (societes.isEmpty()) {
            throw new ResourceNotFoundException("Société", accoutantId.toString());
        }

        List<DocumentResponseDTO> documents = new ArrayList<>();
        societes.forEach(s -> {
            documents.addAll(documentRepository.findBySocieteAndExerciceComptable(s, exercice)
                    .stream()
                    .map(this::mapToDTO)
                    .collect(Collectors.toList()));
        });
        return documents;
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
    public DocumentResponseDTO validateDocument(Long documentId, DocumentValidationDTO validation, User admin) {
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
        document.setValidatedBy(admin);

        Document updated = documentRepository.save(document);

        if (validation.getAction() == DocumentValidationDTO.Action.VALIDER) {
            auditLogService.logValidation(updated, admin);
        } else {
            auditLogService.logRejection(updated, admin, validation.getCommentaire());
        }

        return mapToDTO(updated);
    }

    public DocumentResponseDTO getDocumentById(Long id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", id.toString()));
        return mapToDTO(document);
    }

    public List<DocumentResponseDTO> getDocumentsBySociete(Long accoutantId) {
        List<Societe> societes = societeRepository.findByAccountantId(accoutantId);
        if (societes.isEmpty()) {
            throw new ResourceNotFoundException("Société", accoutantId.toString());
        }

        List<DocumentResponseDTO> documents = new ArrayList<>();
        societes.forEach(s -> {
            documents.addAll(documentRepository.findBySociete(s)
                    .stream()
                    .map(this::mapToDTO)
                    .collect(Collectors.toList()));
        });
        return documents;
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

    @Override
    public List<DocumentResponseDTO> getPendingDocumentsForCurrentUser(User user) {
        if (user.getRole() == User.Role.ADMIN) {
            return documentRepository.findByStatut(Document.StatutDocument.EN_ATTENTE)
                    .stream()
                    .map(this::mapToDTO)
                    .collect(Collectors.toList());
        }

        List<Societe> societes = societeRepository.findByAccountant(user);
        if (societes.isEmpty()) {
            throw new BusinessException("NO_SOCIETE", "L'utilisateur n'est associé à aucune société");
        }
        List<DocumentResponseDTO> documents = new ArrayList<>();
        societes.forEach(s -> {
            documents.addAll(documentRepository.findBySocieteAndStatut(s, Document.StatutDocument.EN_ATTENTE)
                    .stream().map(this::mapToDTO)
                    .collect(Collectors.toList()));
        });
        return documents;
    }

    @Override
    public Page<DocumentResponseDTO> getDocumentsBySocietePaginatedFiltered(
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
            LocalDate datePieceTo) {

        List<Societe> societes = societeRepository.findByAccountantId(accountantId);
        if (societes.isEmpty()) {
            throw new ResourceNotFoundException("Société", accountantId.toString());
        }

        Sort sort = "asc".equalsIgnoreCase(sortDir)
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Document> spec = (root, query, cb) -> root.get("societe").in(societes);

        if (statut != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("statut"), statut));
        }
        if (typeDocument != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("typeDocument"), typeDocument));
        }
        if (exerciceComptable != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("exerciceComptable"), exerciceComptable));
        }
        if (numeroPiece != null && !numeroPiece.trim().isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("numeroPiece")),
                    "%" + numeroPiece.trim().toLowerCase() + "%"));
        }
        if (fournisseur != null && !fournisseur.trim().isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("fournisseur")),
                    "%" + fournisseur.trim().toLowerCase() + "%"));
        }
        if (datePieceFrom != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("datePiece"), datePieceFrom));
        }
        if (datePieceTo != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("datePiece"), datePieceTo));
        }

        return documentRepository.findAll(spec, pageable).map(this::mapToDTO);
    }
}
