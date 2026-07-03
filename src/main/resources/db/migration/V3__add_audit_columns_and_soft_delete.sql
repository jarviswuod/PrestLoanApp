ALTER TABLE loans
    ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) AFTER created_at,
    ADD COLUMN deleted_at DATETIME(6) NULL AFTER updated_at,
    ADD COLUMN is_deleted BIT(1) NOT NULL DEFAULT b'0' AFTER deleted_at;

UPDATE loans
SET updated_at = created_at
WHERE updated_at IS NULL;

ALTER TABLE loan_schedules
    ADD COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) AFTER status,
    ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) AFTER created_at,
    ADD COLUMN deleted_at DATETIME(6) NULL AFTER updated_at,
    ADD COLUMN is_deleted BIT(1) NOT NULL DEFAULT b'0' AFTER deleted_at;

UPDATE loan_schedules
SET updated_at = created_at
WHERE updated_at IS NULL;

ALTER TABLE loan_transactions
    ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) AFTER created_at,
    ADD COLUMN deleted_at DATETIME(6) NULL AFTER updated_at,
    ADD COLUMN is_deleted BIT(1) NOT NULL DEFAULT b'0' AFTER deleted_at;

UPDATE loan_transactions
SET updated_at = created_at
WHERE updated_at IS NULL;
