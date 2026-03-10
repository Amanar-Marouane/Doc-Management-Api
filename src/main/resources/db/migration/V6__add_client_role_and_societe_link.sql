-- V6: Add CLIENT role support
-- A CLIENT user is linked to exactly one Societe (their company).
-- This column is NULL for ADMIN and COMPTABLE users.
ALTER TABLE users
    ADD COLUMN client_societe_id BIGINT NULL,
    ADD CONSTRAINT fk_user_client_societe
        FOREIGN KEY (client_societe_id) REFERENCES societes(id) ON DELETE SET NULL;
