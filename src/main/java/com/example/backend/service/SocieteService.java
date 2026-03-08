package com.example.backend.service;

import com.example.backend.contract.SocieteServiceContract;
import com.example.backend.dto.*;
import com.example.backend.entity.*;
import com.example.backend.repository.DocumentRepository;
import com.example.backend.repository.SocieteRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SocieteService implements SocieteServiceContract {

    private final SocieteRepository societeRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;

    @Transactional
    public SocieteDTO createSociete(SocieteRequestDTO request) {
        if (!SecurityUtils.isAdmin()) {
            throw new RuntimeException("Only admins can create societes");
        }

        if (societeRepository.existsByIce(request.getIce())) {
            throw new RuntimeException("ICE already exists");
        }

        Societe societe = Societe.builder()
                .raisonSociale(request.getRaisonSociale())
                .ice(request.getIce())
                .adresse(request.getAdresse())
                .telephone(request.getTelephone())
                .emailContact(request.getEmailContact())
                .accountant(null)
                .build();

        Societe saved = societeRepository.save(societe);
        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public SocieteDTO getSocieteById(Long id) {
        Societe societe = societeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Societe not found"));
        return toDTO(societe);
    }

    @Transactional(readOnly = true)
    public List<SocieteDTO> getAllSocietes() {
        if (!SecurityUtils.isAdmin()) {
            throw new RuntimeException("Only admins can view societes");
        }

        return societeRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public SocieteDTO updateSociete(Long id, SocieteRequestDTO request) {
        if (!SecurityUtils.isAdmin()) {
            throw new RuntimeException("Only admins can update societes");
        }

        Societe societe = societeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Societe not found"));

        if (!societe.getIce().equals(request.getIce()) &&
                societeRepository.existsByIce(request.getIce())) {
            throw new RuntimeException("ICE already exists");
        }

        societe.setRaisonSociale(request.getRaisonSociale());
        societe.setIce(request.getIce());
        societe.setAdresse(request.getAdresse());
        societe.setTelephone(request.getTelephone());
        societe.setEmailContact(request.getEmailContact());

        Societe updated = societeRepository.save(societe);
        return toDTO(updated);
    }

    @Transactional
    public void deleteSociete(Long id) {
        if (!SecurityUtils.isAdmin()) {
            throw new RuntimeException("Only admins can delete societes");
        }

        if (!societeRepository.existsById(id)) {
            throw new RuntimeException("Societe not found");
        }
        societeRepository.deleteById(id);
    }

    @Transactional
    public SocieteDTO assignAccountantToSociete(Long societeId, Long accountantId) {
        if (!SecurityUtils.isAdmin()) {
            throw new RuntimeException("Only admins can assign accountants");
        }

        Societe societe = societeRepository.findById(societeId)
                .orElseThrow(() -> new RuntimeException("Societe not found"));

        User accountant = userRepository.findById(accountantId)
                .orElseThrow(() -> new RuntimeException("Accountant not found"));

        if (accountant.getRole() != User.Role.COMPTABLE) {
            throw new RuntimeException("User must be COMPTABLE to be assigned as accountant");
        }

        societe.setAccountant(accountant);
        Societe updated = societeRepository.save(societe);
        return toDTO(updated);
    }

    @Transactional
    public SocieteDTO updateAccountantForSociete(Long societeId, Long accountantId) {
        if (!SecurityUtils.isAdmin()) {
            throw new RuntimeException("Only admins can update accountants");
        }

        Societe societe = societeRepository.findById(societeId)
                .orElseThrow(() -> new RuntimeException("Societe not found"));

        User accountant = userRepository.findById(accountantId)
                .orElseThrow(() -> new RuntimeException("Accountant not found"));

        if (accountant.getRole() != User.Role.COMPTABLE) {
            throw new RuntimeException("User must be COMPTABLE to be an accountant");
        }

        societe.setAccountant(accountant);
        Societe updated = societeRepository.save(societe);
        return toDTO(updated);
    }

    @Transactional
    public void removeAccountantFromSociete(Long societeId) {
        if (!SecurityUtils.isAdmin()) {
            throw new RuntimeException("Only admins can remove accountants");
        }

        Societe societe = societeRepository.findById(societeId)
                .orElseThrow(() -> new RuntimeException("Societe not found"));

        societe.setAccountant(null);
        societeRepository.save(societe);
    }

    @Transactional(readOnly = true)
    public List<SocieteDTO> getSocietiesByAccountant(Long accountantId) {
        User accountant = userRepository.findById(accountantId)
                .orElseThrow(() -> new RuntimeException("Accountant not found"));

        if (accountant.getRole() != User.Role.COMPTABLE) {
            throw new RuntimeException("User is not an accountant");
        }

        return societeRepository.findByAccountant(accountant).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SocietComplianceOverviewDTO getSocietComplianceOverview(Long societeId) {
        Societe societe = societeRepository.findById(societeId)
                .orElseThrow(() -> new RuntimeException("Societe not found"));

        SocietComplianceOverviewDTO.AccountantSummaryDTO accountant = null;
        if (societe.getAccountant() != null) {
            accountant = SocietComplianceOverviewDTO.AccountantSummaryDTO.builder()
                    .accountantId(societe.getAccountant().getId())
                    .accountantName(societe.getAccountant().getFullName())
                    .accountantEmail(societe.getAccountant().getEmail())
                    .active(societe.getAccountant().isActive())
                    .build();
        }

        // Load all documents once — single DB round-trip
        List<Document> allDocs = documentRepository.findBySociete(societe);

        // --- Global metrics ---
        long[] global = computeMetrics(allDocs);
        long totalDocuments   = global[0];
        long pendingDocuments = global[1];
        long approvedDocuments = global[2];
        long rejectedDocuments = global[3];
        long deletedDocuments  = global[4];
        double compliancePercentage = global[5] > 0 ? (double) approvedDocuments / global[5] * 100 : 0.0;

        // --- Per fiscal year ---
        Map<Integer, List<Document>> byExercice = allDocs.stream()
                .collect(Collectors.groupingBy(Document::getExerciceComptable));

        List<SocietComplianceOverviewDTO.ExerciceBreakdownDTO> exerciceBreakdowns = byExercice.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> buildExerciceBreakdown(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        // --- Global per doc type ---
        List<SocietComplianceOverviewDTO.DocTypeBreakdownDTO> docTypeBreakdowns =
                Arrays.stream(Document.TypeDocument.values())
                        .map(type -> {
                            List<Document> typeDocs = allDocs.stream()
                                    .filter(d -> d.getTypeDocument() == type)
                                    .collect(Collectors.toList());
                            return buildDocTypeBreakdown(type, typeDocs);
                        })
                        .collect(Collectors.toList());

        // --- Detailed enterprise level: per fiscal year × per doc type ---
        List<SocietComplianceOverviewDTO.ExerciceDetailDTO> exerciceDetails = byExercice.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    List<Document> exerciceDocs = e.getValue();
                    long[] m = computeMetrics(exerciceDocs);
                    double pct = m[5] > 0 ? (double) m[2] / m[5] * 100 : 0.0;

                    List<SocietComplianceOverviewDTO.DocTypeBreakdownDTO> typeBreakdowns =
                            Arrays.stream(Document.TypeDocument.values())
                                    .map(type -> {
                                        List<Document> slice = exerciceDocs.stream()
                                                .filter(d -> d.getTypeDocument() == type)
                                                .collect(Collectors.toList());
                                        return buildDocTypeBreakdown(type, slice);
                                    })
                                    .collect(Collectors.toList());

                    return SocietComplianceOverviewDTO.ExerciceDetailDTO.builder()
                            .exerciceComptable(e.getKey())
                            .total(m[0])
                            .pending(m[1])
                            .approved(m[2])
                            .rejected(m[3])
                            .deleted(m[4])
                            .compliancePercentage(pct)
                            .complianceStatus(getComplianceStatus(pct))
                            .docTypeBreakdowns(typeBreakdowns)
                            .build();
                })
                .collect(Collectors.toList());

        return SocietComplianceOverviewDTO.builder()
                .societeId(societe.getId())
                .raisonSociale(societe.getRaisonSociale())
                .ice(societe.getIce())
                .adresse(societe.getAdresse())
                .societyActive(true)
                .societyCreatedAt(societe.getCreatedAt())
                .accountant(accountant)
                .totalDocuments(totalDocuments)
                .pendingDocuments(pendingDocuments)
                .approvedDocuments(approvedDocuments)
                .rejectedDocuments(rejectedDocuments)
                .deletedDocuments(deletedDocuments)
                .compliancePercentage(compliancePercentage)
                .complianceStatus(getComplianceStatus(compliancePercentage))
                .exerciceBreakdowns(exerciceBreakdowns)
                .docTypeBreakdowns(docTypeBreakdowns)
                .exerciceDetails(exerciceDetails)
                .build();
    }

    /**
     * Returns [total, pending, approved, rejected, deleted, actionable].
     * actionable = total - deleted (used as denominator for compliance %).
     */
    private long[] computeMetrics(List<Document> docs) {
        long total    = docs.size();
        long pending  = docs.stream().filter(d -> d.getStatut() == Document.StatutDocument.EN_ATTENTE).count();
        long approved = docs.stream().filter(d -> d.getStatut() == Document.StatutDocument.VALIDE).count();
        long rejected = docs.stream().filter(d -> d.getStatut() == Document.StatutDocument.REJETE).count();
        long deleted  = docs.stream().filter(d -> d.getStatut() == Document.StatutDocument.SUPPRIME).count();
        long actionable = total - deleted;
        return new long[]{total, pending, approved, rejected, deleted, actionable};
    }

    private SocietComplianceOverviewDTO.ExerciceBreakdownDTO buildExerciceBreakdown(
            Integer exercice, List<Document> docs) {
        long[] m = computeMetrics(docs);
        double pct = m[5] > 0 ? (double) m[2] / m[5] * 100 : 0.0;
        return SocietComplianceOverviewDTO.ExerciceBreakdownDTO.builder()
                .exerciceComptable(exercice)
                .total(m[0])
                .pending(m[1])
                .approved(m[2])
                .rejected(m[3])
                .deleted(m[4])
                .compliancePercentage(pct)
                .complianceStatus(getComplianceStatus(pct))
                .build();
    }

    private SocietComplianceOverviewDTO.DocTypeBreakdownDTO buildDocTypeBreakdown(
            Document.TypeDocument type, List<Document> docs) {
        long[] m = computeMetrics(docs);
        double pct = m[5] > 0 ? (double) m[2] / m[5] * 100 : 0.0;
        return SocietComplianceOverviewDTO.DocTypeBreakdownDTO.builder()
                .typeDocument(type.name())
                .total(m[0])
                .pending(m[1])
                .approved(m[2])
                .rejected(m[3])
                .deleted(m[4])
                .compliancePercentage(pct)
                .complianceStatus(getComplianceStatus(pct))
                .build();
    }

    private String getComplianceStatus(double percentage) {
        if (percentage >= 90)
            return "COMPLIANT";
        if (percentage >= 70)
            return "AT_RISK";
        return "NON_COMPLIANT";
    }

    private SocieteDTO toDTO(Societe societe) {
        UserDTO accountantDTO = null;
        if (societe.getAccountant() != null) {
            accountantDTO = UserDTO.builder()
                    .id(societe.getAccountant().getId())
                    .email(societe.getAccountant().getEmail())
                    .fullName(societe.getAccountant().getFullName())
                    .role(societe.getAccountant().getRole())
                    .active(societe.getAccountant().isActive())
                    .createdAt(societe.getAccountant().getCreatedAt())
                    .build();
        }

        return SocieteDTO.builder()
                .id(societe.getId())
                .raisonSociale(societe.getRaisonSociale())
                .ice(societe.getIce())
                .adresse(societe.getAdresse())
                .telephone(societe.getTelephone())
                .emailContact(societe.getEmailContact())
                .accountant(accountantDTO)
                .createdAt(societe.getCreatedAt())
                .build();
    }
}
