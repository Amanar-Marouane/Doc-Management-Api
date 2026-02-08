package com.example.backend.repository;

import com.example.backend.entity.Societe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SocieteRepository extends JpaRepository<Societe, Long> {
    Optional<Societe> findByIce(String ice);
    boolean existsByIce(String ice);
}
