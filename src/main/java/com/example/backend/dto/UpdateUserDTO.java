package com.example.backend.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserDTO {
    private String fullName;
    private String email;
    private String password; // Optional - only if user wants to change password
    private List<Long> societeIds; // Optional - admin can sync comptable societes
}
