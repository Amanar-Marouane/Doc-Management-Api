package com.example.backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserDTO {
    private String fullName;
    private String email;
    private String password; // Optional - only if user wants to change password
}
