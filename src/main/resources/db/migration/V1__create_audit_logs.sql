CREATE TABLE audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    action VARCHAR(50) NOT NULL,
    document_id BIGINT NOT NULL,
    performed_by_user_id BIGINT NOT NULL,
    performed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    details VARCHAR(1000),
    CONSTRAINT fk_audit_log_document FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
    CONSTRAINT fk_audit_log_user FOREIGN KEY (performed_by_user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_audit_logs_document_id ON audit_logs(document_id);
CREATE INDEX idx_audit_logs_performed_by ON audit_logs(performed_by_user_id);
CREATE INDEX idx_audit_logs_performed_at ON audit_logs(performed_at);
