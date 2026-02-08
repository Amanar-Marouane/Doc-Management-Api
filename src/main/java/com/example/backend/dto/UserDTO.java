package com.example.backend.dto;

import com.example.backend.entity.User;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
    private Long id;
    private String email;
    private String fullName;
    private User.Role role;
    private SocieteDTO societe;
    private boolean active;
    private LocalDateTime createdAt;
}
