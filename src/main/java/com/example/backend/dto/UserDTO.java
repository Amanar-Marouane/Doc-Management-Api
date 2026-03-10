package com.example.backend.dto;

import com.example.backend.entity.User;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

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
    /** COMPTABLE: list of managed societes; null for other roles */
    private List<SocieteDTO> societes;
    /** CLIENT: the societe this user belongs to; null for other roles */
    private SocieteDTO clientSociete;
    private boolean active;
    private LocalDateTime createdAt;
}
