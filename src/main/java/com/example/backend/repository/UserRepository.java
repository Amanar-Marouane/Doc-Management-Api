package com.example.backend.repository;

import com.example.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByRole(User.Role role);

    List<User> findBySocieteId(Long societeId);

    List<User> findByActive(boolean active);

    List<User> findByRoleAndActive(User.Role role, boolean active);

    List<User> findBySocieteIdAndActive(Long societeId, boolean active);

    @Query("SELECT u FROM User u WHERE " +
            "(:role IS NULL OR u.role = :role) AND " +
            "(:societeId IS NULL OR u.societe.id = :societeId) AND " +
            "(:active IS NULL OR u.active = :active) AND " +
            "(:search IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> findByFilters(@Param("role") User.Role role,
            @Param("societeId") Long societeId,
            @Param("active") Boolean active,
            @Param("search") String search,
            Pageable pageable);
}
