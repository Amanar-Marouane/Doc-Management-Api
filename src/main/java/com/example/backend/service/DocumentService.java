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
import com.example.backend.entity.User.Role;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.DocumentRepository;
import com.example.backend.repository.SocieteRepository;
import com.example.backend.repository.UserRepository;

import jakarta.persistence.criteria.Predicate;

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
    private final UserRepository userRepository;
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

        // CLIENT users can only upload to their own societe
        if (uploadedBy.getRole() == Role.CLIENT) {
            if (uploadedBy.getClientSociete() == null
                    || !uploadedBy.getClientSociete().getId().equals(societe.getId())) {
                throw new BusinessException("FORBIDDEN",
                        "Un client ne peut déposer des documents que pour sa propre société");
            }
        }

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

    public List<DocumentResponseDTO> getDocumentsBySocieteAndExercice(Long userId, Integer exercice) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", userId.toString()));

        List<Societe> societes = getSocietesForUser(user);

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
    public DocumentResponseDTO validateDocument(Long documentId, DocumentValidationDTO validation, User validator) {
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
            // Retention period: 10 years from validation year (Law N° 9-88)
            document.setRetentionExpiresAt(LocalDate.of(LocalDateTime.now().getYear() + 10, 12, 31));
        } else {
            if (validation.getCommentaire() == null || validation.getCommentaire().trim().isEmpty()) {
                throw new BusinessException("REJECTION_REASON_REQUIRED",
                        "Le motif de rejet est obligatoire pour rejeter un document");
            }
            document.setStatut(Document.StatutDocument.REJETE);
            document.setCommentaireComptable(validation.getCommentaire());
        }

        document.setDateValidation(LocalDateTime.now());
        document.setValidatedBy(validator);

        Document updated = documentRepository.save(document);

        if (validation.getAction() == DocumentValidationDTO.Action.VALIDER) {
            auditLogService.logValidation(updated, validator);
        } else {
            auditLogService.logRejection(updated, validator, validation.getCommentaire());
        }

        return mapToDTO(updated);
    }

    public DocumentResponseDTO getDocumentById(Long id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", id.toString()));
        return mapToDTO(document);
    }

    public List<DocumentResponseDTO> getDocumentsBySociete(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", userId.toString()));

        List<Societe> societes = getSocietesForUser(user);

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

    @Transactional
    public void deleteDocument(Long documentId, User deletedBy) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", documentId.toString()));

        switch (document.getStatut()) {

            case EN_ATTENTE:
                // Pending documents: any authenticated user can soft-delete.
                break;

            case REJETE:
                // Rejected documents: ADMIN only (CLIENT and COMPTABLE cannot delete rejected
                // docs).
                if (deletedBy.getRole() != User.Role.ADMIN) {
                    throw new BusinessException("FORBIDDEN",
                            "Seul un administrateur peut supprimer un document rejeté");
                }
                break;

            case VALIDE:
                // Validated documents cannot be soft-deleted at all.
                throw new BusinessException("NOT_DELETABLE",
                        String.format(
                                "Le document '%s' est validé et ne peut pas être supprimé. "
                                        + "Seule la suppression définitive (purge) est possible via DELETE /api/documents/{id}/purge, "
                                        + "et uniquement après avoir marqué le document comme SUPPRIME.",
                                document.getNumeroPiece()));

            case SUPPRIME:
                throw new BusinessException("ALREADY_DELETED",
                        String.format("Le document '%s' est déjà marqué comme supprimé.", document.getNumeroPiece()));
        }

        document.setStatut(Document.StatutDocument.SUPPRIME);
        auditLogService.logDeletion(document, deletedBy);
        documentRepository.save(document);
    }

    @Transactional
    public void purgeDocument(Long documentId, User deletedBy) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", documentId.toString()));

        if (deletedBy.getRole() != User.Role.ADMIN) {
            throw new BusinessException("FORBIDDEN",
                    "Seul un administrateur peut effectuer une suppression définitive (purge)");
        }

        if (document.getStatut() != Document.StatutDocument.SUPPRIME) {
            throw new BusinessException("PURGE_NOT_ALLOWED",
                    String.format(
                            "La purge du document '%s' n'est autorisée que si son statut est SUPPRIME (statut actuel: %s).",
                            document.getNumeroPiece(), document.getStatut()));
        }

        // Clear audit trail, then remove physical file and DB record.
        auditLogService.deleteForDocument(document);
        fileStorageService.delete(document.getCheminFichier());
        documentRepository.delete(document);
    }

    @Override
    public List<DocumentResponseDTO> getPendingDocumentsForCurrentUser(User user) {
        if (user.getRole() == User.Role.ADMIN) {
            return documentRepository.findByStatut(Document.StatutDocument.EN_ATTENTE)
                    .stream()
                    .map(this::mapToDTO)
                    .collect(Collectors.toList());
        }

        if (user.getRole() == User.Role.CLIENT) {
            throw new BusinessException("FORBIDDEN",
                    "Les clients n'ont pas accès à la file d'attente de validation");
        }

        // COMPTABLE: only their assigned societes
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
            Long userId,
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

        // 1. Setup Pagination
        Sort sort = "asc".equalsIgnoreCase(sortDir) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        // 2. Fetch User
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", userId.toString()));

        // 3. Initialize Specification with a base condition (where 1=1)
        Specification<Document> spec = Specification.where((root, query, cb) -> cb.conjunction());

        // 4. Role-based Filtering
        if (user.getRole() == Role.COMPTABLE) {
            List<Societe> societes = societeRepository.findByAccountantId(userId);
            if (societes.isEmpty()) {
                throw new ResourceNotFoundException("Société", userId.toString());
            }
            spec = spec.and((root, query, cb) -> root.get("societe").in(societes));
        } else if (user.getRole() == Role.CLIENT) {
            if (user.getClientSociete() == null) {
                throw new BusinessException("NO_SOCIETE", "Ce client n'est associé à aucune société");
            }
            Societe clientSociete = user.getClientSociete();
            spec = spec.and((root, query, cb) -> cb.equal(root.get("societe"), clientSociete));
        }
        // ADMIN: no additional filter — sees all documents

        // 5. Dynamic Filters
        spec = spec.and(buildFilters(statut, typeDocument, exerciceComptable, numeroPiece, fournisseur, datePieceFrom,
                datePieceTo));

        return documentRepository.findAll(spec, pageable).map(this::mapToDTO);
    }

    private Specification<Document> buildFilters(
            Document.StatutDocument statut,
            Document.TypeDocument typeDocument,
            Integer exerciceComptable,
            String numeroPiece,
            String fournisseur,
            LocalDate datePieceFrom,
            LocalDate datePieceTo) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (statut != null)
                predicates.add(cb.equal(root.get("statut"), statut));
            if (typeDocument != null)
                predicates.add(cb.equal(root.get("typeDocument"), typeDocument));
            if (exerciceComptable != null)
                predicates.add(cb.equal(root.get("exerciceComptable"), exerciceComptable));

            if (numeroPiece != null && !numeroPiece.isBlank()) {
                predicates
                        .add(cb.like(cb.lower(root.get("numeroPiece")), "%" + numeroPiece.trim().toLowerCase() + "%"));
            }
            if (fournisseur != null && !fournisseur.isBlank()) {
                predicates
                        .add(cb.like(cb.lower(root.get("fournisseur")), "%" + fournisseur.trim().toLowerCase() + "%"));
            }
            if (datePieceFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("datePiece"), datePieceFrom));
            }
            if (datePieceTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("datePiece"), datePieceTo));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Returns the list of societes relevant to the given user.
     * ADMIN → all societes; COMPTABLE → assigned societes; CLIENT → their single
     * societe.
     */
    private List<Societe> getSocietesForUser(User user) {
        return switch (user.getRole()) {
            case ADMIN -> societeRepository.findAll();
            case COMPTABLE -> {
                List<Societe> s = societeRepository.findByAccountantId(user.getId());
                if (s.isEmpty()) {
                    throw new ResourceNotFoundException("Société", user.getId().toString());
                }
                yield s;
            }
            case CLIENT -> {
                if (user.getClientSociete() == null) {
                    throw new BusinessException("NO_SOCIETE", "Ce client n'est associé à aucune société");
                }
                yield List.of(user.getClientSociete());
            }
        };
    }

    public int countDocumentsBySociete(Long societeId) {
        Societe societe = societeRepository.findById(societeId)
                .orElseThrow(() -> new ResourceNotFoundException("Société", societeId.toString()));
        return documentRepository.countBySociete(societe);
    }

    public int countDocumentsBySocieteAndStatut(Long societeId, Document.StatutDocument statut) {
        Societe societe = societeRepository.findById(societeId)
                .orElseThrow(() -> new ResourceNotFoundException("Société", societeId.toString()));
        return documentRepository.countBySocieteAndStatut(societe, statut);
    }

    private DocumentResponseDTO mapToDTO(Document document) {
        LocalDate retentionExpiresAt = document.getRetentionExpiresAt();
        boolean retentionExpired = LocalDate.now().isAfter(retentionExpiresAt);

        boolean canBeDeleted = switch (document.getStatut()) {
            case EN_ATTENTE -> true;
            case REJETE -> true;
            case VALIDE, SUPPRIME -> false;
        };

        boolean canBePurged = document.getStatut() == Document.StatutDocument.SUPPRIME;

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
                .retentionExpiresAt(retentionExpiresAt)
                .retentionExpired(retentionExpired)
                .canBeDeleted(canBeDeleted)
                .canBePurged(canBePurged)
                .build();
    }
}
