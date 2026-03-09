package com.example.backend.dto;

import com.example.backend.entity.Deadline;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
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

    @NotBlank(message = "La catégorie de document est obligatoire")
    private String documentCategory;

    private Deadline.DeadlineStatus status;
}
