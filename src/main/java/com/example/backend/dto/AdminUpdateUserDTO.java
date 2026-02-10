package com.example.backend.dto;

import com.example.backend.entity.User;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUpdateUserDTO {
    private String fullName;
    private String email;
    private String password;
    private User.Role role;
    private Long societeId; // null for COMPTABLE and ADMIN
    private Boolean active;
}
