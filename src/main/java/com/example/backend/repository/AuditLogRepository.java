package com.example.backend.repository;

import com.example.backend.entity.AuditLog;
import com.example.backend.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long>,
        JpaSpecificationExecutor<AuditLog> {

    List<AuditLog> findByDocumentOrderByPerformedAtAsc(Document document);

    List<AuditLog> findByDocumentIdOrderByPerformedAtAsc(Long documentId);

    void deleteByDocument(Document document);
}
