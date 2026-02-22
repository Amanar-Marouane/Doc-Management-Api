package com.example.backend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import com.example.backend.dto.DocumentValidationDTO;

public class DocumentValidationValidator
        implements ConstraintValidator<ValidDocumentValidation, DocumentValidationDTO> {

    @Override
    public boolean isValid(DocumentValidationDTO dto,
            ConstraintValidatorContext context) {

        if (dto == null)
            return true;

        if (dto.getAction() == DocumentValidationDTO.Action.REJETER) {
            if (dto.getCommentaire() == null || dto.getCommentaire().trim().isEmpty()) {

                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                        "Commentaire is mandatory when rejecting a document")
                        .addPropertyNode("commentaire")
                        .addConstraintViolation();

                return false;
            }
        }

        return true;
    }
}