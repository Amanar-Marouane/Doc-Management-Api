package com.example.backend.repository;

import com.example.backend.entity.Societe;
import com.example.backend.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SocieteRepository extends JpaRepository<Societe, Long> {
    Optional<Societe> findByIce(String ice);

    boolean existsByIce(String ice);

    Optional<Societe> findByAccountantId(Long accountantId);

    List<Societe> findByAccountant(User accountant);
}
