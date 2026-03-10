package com.example.backend.dto;

import com.example.backend.entity.Deadline;
import com.example.backend.entity.Document;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeadlineRequestDTO {

    @NotNull(message = "L'identifiant de la société est obligatoire")
    private Long societeId;

    @NotNull(message = "L'exercice fiscal est obligatoire")
    @Min(value = 2000, message = "L'exercice fiscal doit être >= 2000")
    @Max(value = 2100, message = "L'exercice fiscal doit être <= 2100")
    private Integer fiscalYear;

    @NotNull(message = "La date d'échéance est obligatoire")
    private LocalDate dueDate;

    @NotNull(message = "La catégorie de document est obligatoire")
    private Document.TypeDocument documentCategory;

    private Deadline.DeadlineStatus status;
}
