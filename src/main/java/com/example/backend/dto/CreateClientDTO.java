package com.example.backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateClientDTO {
    private String email;
    private String password;
    private String fullName;
    /** ID of the societe this client belongs to */
    private Long societeId;
}
