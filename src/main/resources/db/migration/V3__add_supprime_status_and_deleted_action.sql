-- Extend audit_logs.action enum to include DOCUMENT_DELETED
ALTER TABLE audit_logs
  MODIFY COLUMN action ENUM('DOCUMENT_REJECTED','DOCUMENT_UPLOADED','DOCUMENT_VALIDATED','DOCUMENT_DELETED') NOT NULL;

-- Extend documents.statut enum to include SUPPRIME (soft-deleted state)
ALTER TABLE documents
  MODIFY COLUMN statut ENUM('EN_ATTENTE','REJETE','VALIDE','SUPPRIME') NOT NULL;
