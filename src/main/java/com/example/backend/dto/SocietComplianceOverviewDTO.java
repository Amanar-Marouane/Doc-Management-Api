package com.example.backend.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocietComplianceOverviewDTO {
    private Long societeId;
    private String raisonSociale;
    private String ice;
    private String adresse;
    private boolean societyActive;
    private LocalDateTime societyCreatedAt;

    // Single accountant assigned
    private AccountantSummaryDTO accountant;

    // --- Global compliance metrics ---
    private long totalDocuments;
    private long pendingDocuments;
    private long approvedDocuments;
    private long rejectedDocuments;
    private long deletedDocuments;
    private double compliancePercentage;
    private String complianceStatus; // "COMPLIANT", "AT_RISK", "NON_COMPLIANT"

    // --- Breakdown per fiscal year ---
    private List<ExerciceBreakdownDTO> exerciceBreakdowns;

    // --- Breakdown per document type (global) ---
    private List<DocTypeBreakdownDTO> docTypeBreakdowns;

    // --- Per fiscal year, with per-type drill-down ---
    private List<ExerciceDetailDTO> exerciceDetails;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AccountantSummaryDTO {
        private Long accountantId;
        private String accountantName;
        private String accountantEmail;
        private boolean active;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExerciceBreakdownDTO {
        private Integer exerciceComptable;
        private long total;
        private long pending;
        private long approved;
        private long rejected;
        private long deleted;
        private double compliancePercentage;
        private String complianceStatus;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DocTypeBreakdownDTO {
        private String typeDocument;
        private long total;
        private long pending;
        private long approved;
        private long rejected;
        private long deleted;
        private double compliancePercentage;
        private String complianceStatus;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExerciceDetailDTO {
        private Integer exerciceComptable;
        private long total;
        private long pending;
        private long approved;
        private long rejected;
        private long deleted;
        private double compliancePercentage;
        private String complianceStatus;
        private List<DocTypeBreakdownDTO> docTypeBreakdowns;
    }
}
