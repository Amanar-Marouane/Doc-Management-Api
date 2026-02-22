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
}
