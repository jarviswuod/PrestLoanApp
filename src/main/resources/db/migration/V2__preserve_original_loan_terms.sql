ALTER TABLE loans
    ADD COLUMN original_tenure_months INT NOT NULL DEFAULT 0 AFTER tenure_months,
    ADD COLUMN original_emi DECIMAL(19, 2) NOT NULL DEFAULT 0.00 AFTER emi;

UPDATE loans
SET original_tenure_months = tenure_months,
    original_emi = emi
WHERE original_tenure_months = 0
   OR original_emi = 0.00;
