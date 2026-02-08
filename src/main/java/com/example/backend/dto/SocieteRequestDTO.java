package com.example.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocieteRequestDTO {
    @NotBlank(message = "Raison sociale is required")
    private String raisonSociale;

    @NotBlank(message = "ICE is required")
    @Size(max = 15, message = "ICE must not exceed 15 characters")
    private String ice;

    @NotBlank(message = "Address is required")
    private String adresse;

    @NotBlank(message = "Telephone is required")
    @Size(max = 20, message = "Telephone must not exceed 20 characters")
    private String telephone;

    @NotBlank(message = "Email contact is required")
    @Email(message = "Invalid email format")
    private String emailContact;
}
