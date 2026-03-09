CREATE TABLE deadlines (
    id                 BIGINT PRIMARY KEY AUTO_INCREMENT,
    societe_id         BIGINT       NOT NULL,
    fiscal_year        INT          NOT NULL,
    due_date           DATE         NOT NULL,
    document_category  VARCHAR(255) NOT NULL,
    status             VARCHAR(20)  NOT NULL DEFAULT 'UPCOMING',
    created_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_deadline_societe
        FOREIGN KEY (societe_id) REFERENCES societes(id) ON DELETE CASCADE,
    CONSTRAINT chk_deadline_status
        CHECK (status IN ('UPCOMING', 'COMPLETED', 'OVERDUE'))
);

CREATE INDEX idx_deadlines_societe_id  ON deadlines(societe_id);
CREATE INDEX idx_deadlines_status      ON deadlines(status);
CREATE INDEX idx_deadlines_due_date    ON deadlines(due_date);
CREATE INDEX idx_deadlines_fiscal_year ON deadlines(fiscal_year);
