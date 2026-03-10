package com.example.backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimpleSocieteDTO {
    private Long id;
    private String raisonSociale;
}
