package com.example.backend.repository;

import com.example.backend.entity.AuditLog;
import com.example.backend.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByDocumentOrderByPerformedAtAsc(Document document);

    List<AuditLog> findByDocumentIdOrderByPerformedAtAsc(Long documentId);
}
