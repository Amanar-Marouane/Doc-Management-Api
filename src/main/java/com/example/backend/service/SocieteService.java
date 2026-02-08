package com.example.backend.service;

import com.example.backend.dto.SocieteDTO;
import com.example.backend.dto.SocieteRequestDTO;
import com.example.backend.entity.Societe;
import com.example.backend.repository.SocieteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SocieteService {

    private final SocieteRepository societeRepository;

    @Transactional
    public SocieteDTO createSociete(SocieteRequestDTO request) {
        if (societeRepository.existsByIce(request.getIce())) {
            throw new RuntimeException("ICE already exists");
        }

        Societe societe = Societe.builder()
                .raisonSociale(request.getRaisonSociale())
                .ice(request.getIce())
                .adresse(request.getAdresse())
                .telephone(request.getTelephone())
                .emailContact(request.getEmailContact())
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
        return societeRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public SocieteDTO updateSociete(Long id, SocieteRequestDTO request) {
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
        if (!societeRepository.existsById(id)) {
            throw new RuntimeException("Societe not found");
        }
        societeRepository.deleteById(id);
    }

    private SocieteDTO toDTO(Societe societe) {
        return SocieteDTO.builder()
                .id(societe.getId())
                .raisonSociale(societe.getRaisonSociale())
                .ice(societe.getIce())
                .adresse(societe.getAdresse())
                .telephone(societe.getTelephone())
                .emailContact(societe.getEmailContact())
                .createdAt(societe.getCreatedAt())
                .build();
    }
}
