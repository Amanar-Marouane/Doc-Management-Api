package com.example.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentValidationDTO {

    @NotNull(message = "L'action est obligatoire (VALIDER ou REJETER)")
    private Action action;

    private String commentaire;

    public enum Action {
        VALIDER,
        REJETER
    }
}
