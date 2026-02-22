package com.example.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.backend.entity.Document;
import com.example.backend.entity.Societe;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findBySocieteAndExerciceComptable(Societe societe, Integer exerciceComptable);

    List<Document> findByStatut(Document.StatutDocument statut);

    List<Document> findBySocieteAndStatut(Societe societe, Document.StatutDocument statut);

    Optional<Document> findByNumeroPiece(String numeroPiece);

    List<Document> findBySociete(Societe societe);

    List<Document> findByStatutAndExerciceComptable(Document.StatutDocument statut, Integer exerciceComptable);
}
