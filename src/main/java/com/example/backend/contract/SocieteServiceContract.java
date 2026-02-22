package com.example.backend.contract;

import com.example.backend.dto.*;

import java.util.List;

public interface SocieteServiceContract {
    SocieteDTO createSociete(SocieteRequestDTO request);

    SocieteDTO getSocieteById(Long id);

    List<SocieteDTO> getAllSocietes();

    SocieteDTO updateSociete(Long id, SocieteRequestDTO request);

    void deleteSociete(Long id);

    SocieteDTO assignAccountantToSociete(Long societeId, Long accountantId);

    SocieteDTO updateAccountantForSociete(Long societeId, Long accountantId);

    void removeAccountantFromSociete(Long societeId);

    List<SocieteDTO> getSocietiesByAccountant(Long accountantId);

    SocietComplianceOverviewDTO getSocietComplianceOverview(Long societeId);
}
