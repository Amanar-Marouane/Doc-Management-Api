package com.example.backend.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocieteDTO {
    private Long id;
    private String raisonSociale;
    private String ice;
    private String adresse;
    private String telephone;
    private String emailContact;
    private LocalDateTime createdAt;
}
