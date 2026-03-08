package com.example.backend.repository;

import com.example.backend.entity.MissingFileWarning;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MissingFileWarningRepository extends JpaRepository<MissingFileWarning, Long> {

    @Query("SELECT w FROM MissingFileWarning w JOIN FETCH w.document d JOIN FETCH d.societe ORDER BY w.detectedAt DESC")
    List<MissingFileWarning> findAllWithDocumentAndSociete();
}
