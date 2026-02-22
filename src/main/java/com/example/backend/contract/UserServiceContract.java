package com.example.backend.contract;

import com.example.backend.dto.*;
import com.example.backend.entity.User;

import java.util.List;

public interface UserServiceContract {
    UserDTO createComptable(CreateComptableDTO request);

    List<UserDTO> getAllComptables();

    UserDTO getUserById(Long id);

    UserDTO getCurrentUser();

    void deleteUser(Long id);

    UserDTO updateUser(Long id, UpdateUserDTO request);

    UserDTO adminUpdateUser(Long id, AdminUpdateUserDTO request);

    UserDTO updateUserStatus(Long id, UpdateUserStatusDTO request);

    PageResponse<UserDTO> getUsersWithFilters(User.Role role, Long societeId, Boolean active,
            String search, int page, int size, String sortBy);
}
