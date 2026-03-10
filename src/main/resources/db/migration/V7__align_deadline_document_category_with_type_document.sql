-- Align Deadline.documentCategory with Document.TypeDocument enum
-- Removes any legacy free-text values and enforces the same enum set.

-- 1. Remove rows with a documentCategory that is not a valid Document.TypeDocument value
--    (safe for development; prevents constraint violation on the next step)
DELETE FROM deadlines
WHERE document_category NOT IN ('FACTURE_ACHAT', 'FACTURE_VENTE', 'TICKET_CAISSE', 'RELEVE_BANCAIRE');

-- 2. Add CHECK constraint so the column only accepts Document.TypeDocument values
ALTER TABLE deadlines
    ADD CONSTRAINT chk_deadline_document_category
        CHECK (document_category IN ('FACTURE_ACHAT', 'FACTURE_VENTE', 'TICKET_CAISSE', 'RELEVE_BANCAIRE'));
