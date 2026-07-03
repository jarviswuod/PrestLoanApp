CREATE TABLE loans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    principal DECIMAL(19, 2) NOT NULL,
    annual_interest_rate DECIMAL(9, 6) NOT NULL,
    tenure_months INT NOT NULL,
    original_tenure_months INT NOT NULL,
    emi DECIMAL(19, 2) NOT NULL,
    original_emi DECIMAL(19, 2) NOT NULL,
    start_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6),
    is_deleted BIT(1) NOT NULL DEFAULT b'0'
);

CREATE TABLE loan_schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    loan_id BIGINT NOT NULL,
    installment_number INT NOT NULL,
    due_date DATE NOT NULL,
    opening_balance DECIMAL(19, 2) NOT NULL,
    emi_amount DECIMAL(19, 2) NOT NULL,
    principal_component DECIMAL(19, 2) NOT NULL,
    interest_component DECIMAL(19, 2) NOT NULL,
    closing_balance DECIMAL(19, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6),
    is_deleted BIT(1) NOT NULL DEFAULT b'0',
    CONSTRAINT fk_loan_schedule_loan FOREIGN KEY (loan_id) REFERENCES loans(id),
    CONSTRAINT uk_loan_installment UNIQUE (loan_id, installment_number)
);

CREATE TABLE loan_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    loan_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    prepayment_option VARCHAR(50),
    amount DECIMAL(19, 2) NOT NULL,
    installment_number INT NOT NULL,
    notes VARCHAR(1000),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6),
    is_deleted BIT(1) NOT NULL DEFAULT b'0',
    CONSTRAINT fk_loan_transaction_loan FOREIGN KEY (loan_id) REFERENCES loans(id)
);
