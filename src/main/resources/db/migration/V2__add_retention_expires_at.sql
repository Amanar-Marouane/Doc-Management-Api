-- Add retention_expires_at column to documents table
-- Per Moroccan Law N° 9-88: documents must be kept for 10 years from end of their fiscal year.
-- retention_expires_at = December 31 of (exercice_comptable + 10)

ALTER TABLE documents
    ADD COLUMN retention_expires_at DATE NOT NULL DEFAULT '2034-12-31';

-- Backfill existing rows based on their exercice_comptable value
UPDATE documents
SET retention_expires_at = DATE(CONCAT(exercice_comptable + 10, '-12-31'));
