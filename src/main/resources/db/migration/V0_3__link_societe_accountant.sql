ALTER TABLE societes
    ADD COLUMN accountant_id BIGINT NULL,
    ADD CONSTRAINT fk_societe_accountant
        FOREIGN KEY (accountant_id) REFERENCES users(id) ON DELETE SET NULL;
