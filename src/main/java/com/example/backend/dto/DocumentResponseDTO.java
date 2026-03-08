package com.example.backend.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.example.backend.entity.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentResponseDTO {

    private Long id;
    private String numeroPiece;
    private Document.TypeDocument typeDocument;
    private String cheminFichier;
    private String categorieComptable;
    private LocalDate datePiece;
    private BigDecimal montant;
    private String fournisseur;
    private String nomFichierOriginal;
    private Document.StatutDocument statut;
    private LocalDateTime dateValidation;
    private String commentaireComptable;
    private String societeRaisonSociale;
    private String uploadedByName;
    private String validatedByName;
    private Integer exerciceComptable;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Date after which this document may be deleted (exerciceComptable + 10 years,
     * Dec 31).
     */
    private LocalDate retentionExpiresAt;

    /**
     * True if the 10-year retention period has fully elapsed (future-proof flag).
     */
    private boolean retentionExpired;

    /** True for EN_ATTENTE (anyone) and REJETE (admin) statuses — soft-delete is allowed. */
    private boolean canBeDeleted;

    /** True when status is SUPPRIME — document can be permanently purged by admin. */
    private boolean canBePurged;
}
