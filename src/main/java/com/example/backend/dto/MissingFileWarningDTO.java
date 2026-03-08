package com.example.backend.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MissingFileWarningDTO {
    private Long id;
    private Long documentId;
    private String numeroPiece;
    private String typeDocument;
    private String statut;
    private Integer exerciceComptable;
    private Long societeId;
    private String raisonSociale;
    private String expectedPath;
    private LocalDateTime detectedAt;
}
