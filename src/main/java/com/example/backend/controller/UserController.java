package com.example.backend.controller;

import com.example.backend.contract.UserServiceContract;
import com.example.backend.dto.*;
import com.example.backend.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserServiceContract userService;

    /**
     * Get current authenticated user profile
     */
    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser() {
        UserDTO user = userService.getCurrentUser();
        return ResponseEntity.ok(user);
    }

    /**
     * Update current user's profile (fullName, email, password)
     */
    @PutMapping("/me")
    public ResponseEntity<UserDTO> updateCurrentUser(@RequestBody UpdateUserDTO request) {
        UserDTO currentUser = userService.getCurrentUser();
        UserDTO updated = userService.updateUser(currentUser.getId(), request);
        return ResponseEntity.ok(updated);
    }

    // ==================== ADMIN ENDPOINTS ====================

    /**
     * Create a new Comptable (Accountant) user - ADMIN ONLY
     */
    @PostMapping("/comptable")
    public ResponseEntity<UserDTO> createComptable(@RequestBody CreateComptableDTO request) {
        UserDTO created = userService.createComptable(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Get all Comptables - ADMIN ONLY
     */
    @GetMapping("/comptables")
    public ResponseEntity<List<UserDTO>> getAllComptables() {
        List<UserDTO> comptables = userService.getAllComptables();
        return ResponseEntity.ok(comptables);
    }

    /**
     * Get users with filters and pagination - ADMIN ONLY
     * 
     * @param role      Filter by role (COMPTABLE, SOCIETE, ADMIN)
     * @param societeId Filter by societe ID
     * @param active    Filter by active status
     * @param search    Search by name or email
     * @param page      Page number (default: 0)
     * @param size      Page size (default: 10)
     * @param sortBy    Sort field (default: createdAt)
     */
    @GetMapping
    public ResponseEntity<PageResponse<UserDTO>> getUsers(
            @RequestParam(required = false) User.Role role,
            @RequestParam(required = false) Long societeId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy) {
        PageResponse<UserDTO> users = userService.getUsersWithFilters(role, societeId, active, search, page, size,
                sortBy);
        return ResponseEntity.ok(users);
    }

    /**
     * Get user by ID - ADMIN ONLY
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        UserDTO user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    /**
     * Update any user's basic profile - ADMIN ONLY OR SELF
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(@PathVariable Long id, @RequestBody UpdateUserDTO request) {
        UserDTO updated = userService.updateUser(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Admin update - can change role, societe, active status, etc. - ADMIN ONLY
     */
    @PutMapping("/{id}/admin")
    public ResponseEntity<UserDTO> adminUpdateUser(@PathVariable Long id, @RequestBody AdminUpdateUserDTO request) {
        UserDTO updated = userService.adminUpdateUser(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Activate or deactivate a user - ADMIN ONLY
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<UserDTO> updateUserStatus(@PathVariable Long id, @RequestBody UpdateUserStatusDTO request) {
        UserDTO updated = userService.updateUserStatus(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete a user - ADMIN ONLY
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
