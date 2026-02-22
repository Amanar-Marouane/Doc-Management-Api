package com.example.backend.dto;

import com.example.backend.validation.ValidDocumentValidation;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@ValidDocumentValidation
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
