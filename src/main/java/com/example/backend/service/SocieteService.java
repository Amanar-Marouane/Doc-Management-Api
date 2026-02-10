package com.example.backend.service;

import com.example.backend.dto.*;
import com.example.backend.entity.Societe;
import com.example.backend.entity.User;
import com.example.backend.repository.SocieteRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SocieteService {

    private final SocieteRepository societeRepository;
    private final UserRepository userRepository;

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
        if (!SecurityUtils.isAdmin()) {
            throw new RuntimeException("Only admins can view societes");
        }

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

        // Check if accountant already assigned to another society
        societeRepository.findByAccountantId(accountantId).ifPresent(s -> {
            throw new RuntimeException("Accountant is already assigned to another society");
        });

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

        // Check if new accountant is already assigned to another society
        societeRepository.findByAccountantId(accountantId).ifPresent(s -> {
            if (!s.getId().equals(societeId)) {
                throw new RuntimeException("Accountant is already assigned to another society");
            }
        });

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

        // TODO: Calculate compliance metrics from document service
        long totalDocuments = 0;
        long pendingDocuments = 0;
        long approvedDocuments = 0;
        long rejectedDocuments = 0;
        double compliancePercentage = totalDocuments > 0
                ? (double) approvedDocuments / totalDocuments * 100
                : 0.0;

        String complianceStatus = getComplianceStatus(compliancePercentage);

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
                .compliancePercentage(compliancePercentage)
                .complianceStatus(complianceStatus)
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
