package com.example.backend.service;

import com.example.backend.dto.AuditLogDTO;
import com.example.backend.entity.AuditLog;
import com.example.backend.entity.Document;
import com.example.backend.entity.User;
import com.example.backend.repository.AuditLogRepository;
import com.example.backend.util.AppLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditLogService {

        private final AuditLogRepository auditLogRepository;

        @Transactional
        public void logUpload(Document document, User performedBy) {
                AuditLog log = AuditLog.builder()
                                .action(AuditLog.AuditAction.DOCUMENT_UPLOADED)
                                .document(document)
                                .performedBy(performedBy)
                                .details(String.format(
                                                "Document '%s' uploadé par %s pour la société '%s' (exercice %d)",
                                                document.getNumeroPiece(),
                                                performedBy.getFullName(),
                                                document.getSociete().getRaisonSociale(),
                                                document.getExerciceComptable()))
                                .build();
                auditLogRepository.save(log);
                AppLogger.info(String.format("[AUDIT] UPLOAD document='%s' by='%s'",
                                document.getNumeroPiece(), performedBy.getEmail()));
        }

        @Transactional
        public void logValidation(Document document, User performedBy) {
                AuditLog log = AuditLog.builder()
                                .action(AuditLog.AuditAction.DOCUMENT_VALIDATED)
                                .document(document)
                                .performedBy(performedBy)
                                .details(String.format("Document '%s' validé par %s",
                                                document.getNumeroPiece(), performedBy.getFullName()))
                                .build();
                auditLogRepository.save(log);
                AppLogger.info(String.format("[AUDIT] VALIDATED document='%s' by='%s'",
                                document.getNumeroPiece(), performedBy.getEmail()));
        }

        @Transactional
        public void logRejection(Document document, User performedBy, String reason) {
                AuditLog log = AuditLog.builder()
                                .action(AuditLog.AuditAction.DOCUMENT_REJECTED)
                                .document(document)
                                .performedBy(performedBy)
                                .details(String.format("Document '%s' rejeté par %s. Motif: %s",
                                                document.getNumeroPiece(), performedBy.getFullName(), reason))
                                .build();
                auditLogRepository.save(log);
                AppLogger.info(String.format("[AUDIT] REJECTED document='%s' by='%s' reason='%s'",
                                document.getNumeroPiece(), performedBy.getEmail(), reason));
        }

        @Transactional
        public void logDeletion(Document document, User performedBy) {
                AuditLog log = AuditLog.builder()
                                .action(AuditLog.AuditAction.DOCUMENT_DELETED)
                                .document(document)
                                .performedBy(performedBy)
                                .details(String.format(
                                                "Document '%s' supprimé par %s (exercice %d, rétention expirée le %s)",
                                                document.getNumeroPiece(),
                                                performedBy.getFullName(),
                                                document.getExerciceComptable(),
                                                document.getRetentionExpiresAt()))
                                .build();
                auditLogRepository.save(log);
                AppLogger.info(String.format("[AUDIT] DELETED document='%s' by='%s'",
                                document.getNumeroPiece(), performedBy.getEmail()));
        }

        @Transactional
        public void deleteForDocument(Document document) {
                auditLogRepository.deleteByDocument(document);
                AppLogger.info(String.format("[AUDIT] Purged all audit logs for document='%s'",
                                document.getNumeroPiece()));
        }

        public List<AuditLogDTO> getAuditLogsForDocument(Long documentId) {
                return auditLogRepository.findByDocumentIdOrderByPerformedAtAsc(documentId)
                                .stream()
                                .map(this::toDTO)
                                .collect(Collectors.toList());
        }

        public Page<AuditLogDTO> getAuditLogsPaginated(
                        AuditLog.AuditAction action,
                        Long documentId,
                        Long performedById,
                        LocalDateTime from,
                        LocalDateTime to,
                        Integer page,
                        Integer size,
                        String sortBy,
                        String sortDir) {

                Sort sort = "asc".equalsIgnoreCase(sortDir)
                                ? Sort.by(sortBy).ascending()
                                : Sort.by(sortBy).descending();
                Pageable pageable = PageRequest.of(page, size, sort);

                Specification<AuditLog> spec = Specification.where((Specification<AuditLog>) (root, query, cb) -> cb.conjunction());

                if (action != null) {
                        spec = spec.and((root, query, cb) -> cb.equal(root.get("action"), action));
                }
                if (documentId != null) {
                        spec = spec.and((root, query, cb) -> cb.equal(root.get("document").get("id"), documentId));
                }
                if (performedById != null) {
                        spec = spec.and((root, query, cb) -> cb.equal(root.get("performedBy").get("id"), performedById));
                }
                if (from != null) {
                        spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("performedAt"), from));
                }
                if (to != null) {
                        spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("performedAt"), to));
                }

                return auditLogRepository.findAll(spec, pageable).map(this::toDTO);
        }

        private AuditLogDTO toDTO(AuditLog log) {
                return AuditLogDTO.builder()
                                .id(log.getId())
                                .action(log.getAction())
                                .documentId(log.getDocument().getId())
                                .documentNumeroPiece(log.getDocument().getNumeroPiece())
                                .performedById(log.getPerformedBy().getId())
                                .performedByName(log.getPerformedBy().getFullName())
                                .performedAt(log.getPerformedAt())
                                .details(log.getDetails())
                                .build();
        }
}
