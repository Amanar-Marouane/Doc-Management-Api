package com.example.backend.dto;

import lombok.*;
import java.time.LocalDateTime;

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
    
    // Compliance metrics
    private long totalDocuments;
    private long pendingDocuments;
    private long approvedDocuments;
    private long rejectedDocuments;
    private double compliancePercentage;
    private String complianceStatus; // "COMPLIANT", "AT_RISK", "NON_COMPLIANT"
    
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
}
