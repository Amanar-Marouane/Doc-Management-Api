package com.example.backend.dto;

import com.example.backend.entity.Deadline;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeadlineResponseDTO {

    private Long id;
    private Long societeId;
    private String societeRaisonSociale;
    private Integer fiscalYear;
    private LocalDate dueDate;
    private String documentCategory;
    private Deadline.DeadlineStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
