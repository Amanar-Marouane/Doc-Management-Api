package com.example.backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DocumentValidationValidator.class)
@Documented
public @interface ValidDocumentValidation {

    String message() default "Commentaire is required when action is REJETER";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}