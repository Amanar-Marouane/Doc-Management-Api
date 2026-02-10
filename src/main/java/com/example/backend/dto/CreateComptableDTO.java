package com.example.backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateComptableDTO {
    private String email;
    private String password;
    private String fullName;
    private Long societeId; // Optional: assign to societe on creation
}
