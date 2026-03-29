package com.example.backend.service;

import com.example.backend.dto.SocieteDTO;
import com.example.backend.dto.SocieteRequestDTO;
import com.example.backend.entity.Societe;
import com.example.backend.repository.DocumentRepository;
import com.example.backend.repository.SocieteRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.util.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SocieteServiceTest {

    @Mock
    private SocieteRepository societeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DocumentRepository documentRepository;

    @InjectMocks
    private SocieteService societeService;

    private MockedStatic<SecurityUtils> mockedSecurityUtils;

    @BeforeEach
    void setUp() {
        mockedSecurityUtils = mockStatic(SecurityUtils.class);
    }

    @AfterEach
    void tearDown() {
        mockedSecurityUtils.close();
    }

    @Test
    void createSociete_ShouldSucceed_WhenUserIsAdminAndIceDoesNotExist() {
        // Arrange
        SocieteRequestDTO request = SocieteRequestDTO.builder()
                .raisonSociale("Test Company")
                .ice("123456789")
                .adresse("123 Test St")
                .telephone("0612345678")
                .emailContact("test@company.com")
                .build();

        mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);
        when(societeRepository.existsByIce("123456789")).thenReturn(false);

        Societe savedSociete = Societe.builder()
                .id(1L)
                .raisonSociale(request.getRaisonSociale())
                .ice(request.getIce())
                .adresse(request.getAdresse())
                .telephone(request.getTelephone())
                .emailContact(request.getEmailContact())
                .build();

        when(societeRepository.save(any(Societe.class))).thenReturn(savedSociete);

        // Act
        SocieteDTO result = societeService.createSociete(request);

        // Assert
        assertNotNull(result);
        assertEquals("Test Company", result.getRaisonSociale());
        assertEquals("123456789", result.getIce());
        verify(societeRepository, times(1)).save(any(Societe.class));
    }

    @Test
    void createSociete_ShouldThrowException_WhenUserIsNotAdmin() {
        // Arrange
        SocieteRequestDTO request = new SocieteRequestDTO();
        mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(false);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            societeService.createSociete(request);
        });
        assertEquals("Only admins can create societes", exception.getMessage());
        verify(societeRepository, never()).save(any(Societe.class));
    }

    @Test
    void createSociete_ShouldThrowException_WhenIceExists() {
        // Arrange
        SocieteRequestDTO request = SocieteRequestDTO.builder()
                .ice("123456789")
                .build();

        mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);
        when(societeRepository.existsByIce("123456789")).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            societeService.createSociete(request);
        });
        assertEquals("ICE already exists", exception.getMessage());
        verify(societeRepository, never()).save(any(Societe.class));
    }

    @Test
    void getSocieteById_ShouldReturnSociete_WhenExists() {
        // Arrange
        Long id = 1L;
        Societe societe = Societe.builder()
                .id(id)
                .raisonSociale("Test Company")
                .ice("123456789")
                .build();

        when(societeRepository.findById(id)).thenReturn(Optional.of(societe));

        // Act
        SocieteDTO result = societeService.getSocieteById(id);

        // Assert
        assertNotNull(result);
        assertEquals(id, result.getId());
        assertEquals("Test Company", result.getRaisonSociale());
    }

    @Test
    void getSocieteById_ShouldThrowException_WhenNotExists() {
        // Arrange
        Long id = 99L;
        when(societeRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            societeService.getSocieteById(id);
        });
        assertEquals("Societe not found", exception.getMessage());
    }
}
