package com.example.backend.service;

import com.example.backend.dto.CreateComptableDTO;
import com.example.backend.dto.UserDTO;
import com.example.backend.entity.User;
import com.example.backend.repository.SocieteRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.util.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SocieteRepository societeRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private MockedStatic<SecurityUtils> mockedSecurityUtils;

    @BeforeEach
    void setUp() {
        mockedSecurityUtils = mockStatic(SecurityUtils.class);
    }

    @AfterEach
    void tearDown() {
        mockedSecurityUtils.close();
    }

    @Test
    void createComptable_ShouldSucceed_WhenUserIsAdminAndEmailDoesNotExist() {
        // Arrange
        CreateComptableDTO request = CreateComptableDTO.builder()
                .email("new@comptable.com")
                .password("password123")
                .fullName("New Comptable")
                .build();

        mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);
        when(userRepository.existsByEmail("new@comptable.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");

        User savedUser = User.builder()
                .id(1L)
                .email(request.getEmail())
                .fullName(request.getFullName())
                .role(User.Role.COMPTABLE)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        UserDTO result = userService.createComptable(request);

        // Assert
        assertNotNull(result);
        assertEquals("new@comptable.com", result.getEmail());
        assertEquals(User.Role.COMPTABLE, result.getRole());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void createComptable_ShouldThrowException_WhenUserIsNotAdmin() {
        // Arrange
        CreateComptableDTO request = new CreateComptableDTO();
        mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(false);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.createComptable(request);
        });
        assertEquals("Only admins can create users", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createComptable_ShouldThrowException_WhenEmailExists() {
        // Arrange
        CreateComptableDTO request = CreateComptableDTO.builder()
                .email("existing@email.com")
                .build();

        mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);
        when(userRepository.existsByEmail("existing@email.com")).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.createComptable(request);
        });
        assertEquals("Email already exists", exception.getMessage());
    }

    @Test
    void getUserById_ShouldReturnUser_WhenExists() {
        // Arrange
        Long id = 1L;
        User user = User.builder()
                .id(id)
                .email("test@user.com")
                .fullName("Test User")
                .role(User.Role.COMPTABLE)
                .build();

        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        // Act
        UserDTO result = userService.getUserById(id);

        // Assert
        assertNotNull(result);
        assertEquals(id, result.getId());
        assertEquals("test@user.com", result.getEmail());
    }

    @Test
    void getUserById_ShouldThrowException_WhenNotExists() {
        // Arrange
        Long id = 99L;
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.getUserById(id);
        });
        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void deleteUser_ShouldSucceed_WhenUserIsAdminAndTargetIsNotAdmin() {
        // Arrange
        Long id = 1L;
        User userToDelete = User.builder()
                .id(id)
                .email("delete@me.com")
                .role(User.Role.COMPTABLE)
                .build();

        mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);
        when(userRepository.findById(id)).thenReturn(Optional.of(userToDelete));

        // Act
        userService.deleteUser(id);

        // Assert
        verify(userRepository, times(1)).deleteById(id);
    }

    @Test
    void deleteUser_ShouldThrowException_WhenTargetIsAdmin() {
        // Arrange
        Long id = 1L;
        User adminToDelete = User.builder()
                .id(id)
                .email("admin@me.com")
                .role(User.Role.ADMIN)
                .build();

        mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);
        when(userRepository.findById(id)).thenReturn(Optional.of(adminToDelete));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.deleteUser(id);
        });
        assertEquals("Cannot delete admin user", exception.getMessage());
        verify(userRepository, never()).deleteById(anyLong());
    }
}
