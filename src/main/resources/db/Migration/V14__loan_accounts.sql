-- Kredyty, etap 1: typ konta LOAN i parametry kredytu.

-- Typ konta jako tekst (tak jak transactions.type w V4): kolejne typy nie wymagają ALTER ENUM.
ALTER TABLE accounts
    MODIFY COLUMN account_type VARCHAR(30) NULL;

CREATE TABLE loans
(
    id                         BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id                 BIGINT         NOT NULL,
    lender                     VARCHAR(100)   NULL,
    original_principal         DECIMAL(38, 2) NOT NULL,
    start_date                 DATE           NOT NULL,
    term_months                INT            NOT NULL,
    annual_interest_rate       DECIMAL(7, 4)  NOT NULL, -- w procentach: 7.2500 = 7,25%
    rate_type                  VARCHAR(20)    NOT NULL, -- FIXED / VARIABLE
    installment_type           VARCHAR(20)    NOT NULL, -- ANNUITY / DECREASING
    payment_day                INT            NOT NULL,
    overpayment_strategy       VARCHAR(20)    NOT NULL, -- SHORTEN_TERM / REDUCE_INSTALLMENT (domyślna)
    default_payment_account_id BIGINT         NULL,
    created_at                 DATETIME(6)    NOT NULL,
    updated_at                 DATETIME(6)    NOT NULL,
    created_by                 VARCHAR(255)   NOT NULL,
    updated_by                 VARCHAR(255)   NULL,
    CONSTRAINT uk_loan_account UNIQUE (account_id),
    CONSTRAINT fk_loan_account FOREIGN KEY (account_id) REFERENCES accounts (id),
    CONSTRAINT fk_loan_pay_account FOREIGN KEY (default_payment_account_id) REFERENCES accounts (id),
    CONSTRAINT chk_loan_day CHECK (payment_day BETWEEN 1 AND 31),
    CONSTRAINT chk_loan_term CHECK (term_months BETWEEN 1 AND 600),
    CONSTRAINT chk_loan_principal CHECK (original_principal > 0),
    CONSTRAINT chk_loan_rate CHECK (annual_interest_rate >= 0 AND annual_interest_rate <= 100)
);