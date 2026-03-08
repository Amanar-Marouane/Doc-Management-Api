CREATE TABLE missing_file_warnings (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    document_id     BIGINT       NOT NULL,
    expected_path   VARCHAR(500) NOT NULL,
    detected_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_missing_warning_document
        FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE
);

CREATE INDEX idx_missing_file_warnings_document_id ON missing_file_warnings(document_id);
CREATE INDEX idx_missing_file_warnings_detected_at ON missing_file_warnings(detected_at);
