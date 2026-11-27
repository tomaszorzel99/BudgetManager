-- Kredyty, etap 2: spłaty (rata, nadpłata, wypłata), kategoria kosztów kredytu, dopłata do rat.

-- Klucze kategorii systemowych: aplikacja szuka ich po kluczu, nie po nazwie, którą można zmienić.
ALTER TABLE categories
    ADD COLUMN system_key VARCHAR(40) NULL,
    ADD CONSTRAINT uk_category_system_key UNIQUE (system_key);
ALTER TABLE subcategory
    ADD COLUMN system_key VARCHAR(40) NULL,
    ADD CONSTRAINT uk_subcategory_system_key UNIQUE (system_key);

UPDATE categories SET system_key = 'INITIAL_BALANCE' WHERE name = 'Initial Balance';
UPDATE categories SET system_key = 'ADJUSTMENT' WHERE name = 'Korekta salda' AND type = 'ADJUSTMENT';

-- Koszty kredytu (odsetki i opłaty, bez kapitału). Jeśli taka kategoria już jest, tylko ją oznaczamy.
INSERT INTO categories (name, color, type, description)
SELECT 'Kredyty i pożyczki', '#78716C', 'EXPENSE', 'Odsetki i opłaty od kredytów'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Kredyty i pożyczki');
UPDATE categories SET system_key = 'LOAN_COSTS' WHERE name = 'Kredyty i pożyczki' AND type = 'EXPENSE';

INSERT INTO subcategory (name, category_id, description, system_key)
SELECT 'Odsetki', id, 'Odsetki z rat i nadpłat (po dopłacie)', 'LOAN_INTEREST'
FROM categories WHERE system_key = 'LOAN_COSTS';
INSERT INTO subcategory (name, category_id, description, system_key)
SELECT 'Prowizje i opłaty', id, 'Prowizje i opłaty bankowe', 'LOAN_FEES'
FROM categories WHERE system_key = 'LOAN_COSTS';

-- Dopłata do rat (np. Bezpieczny Kredyt 2%): oprocentowanie, które faktycznie płaci kredytobiorca,
-- i data końca dopłat. Puste = kredyt bez dopłat.
ALTER TABLE loans
    ADD COLUMN subsidized_rate    DECIMAL(7, 4) NULL,
    ADD COLUMN subsidy_end_date   DATE          NULL,
    ADD CONSTRAINT chk_loan_subsidized_rate CHECK (subsidized_rate IS NULL OR (subsidized_rate >= 0 AND subsidized_rate <= 100));

CREATE TABLE loan_payments
(
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    loan_id                 BIGINT         NOT NULL,
    kind                    VARCHAR(20)    NOT NULL, -- INSTALLMENT / OVERPAYMENT / DISBURSEMENT
    payment_date            DATE           NOT NULL,
    principal_amount        DECIMAL(38, 2) NOT NULL DEFAULT 0,
    interest_amount         DECIMAL(38, 2) NOT NULL DEFAULT 0, -- odsetki naliczone przez bank (przed dopłatą)
    subsidy_amount          DECIMAL(38, 2) NOT NULL DEFAULT 0, -- część odsetek zapłacona przez państwo
    fee_amount              DECIMAL(38, 2) NOT NULL DEFAULT 0,
    strategy                VARCHAR(20)    NULL,               -- tylko nadpłata: SHORTEN_TERM / REDUCE_INSTALLMENT
    payer_account_id        BIGINT         NOT NULL,
    transfer_id             BIGINT         NULL,
    interest_transaction_id BIGINT         NULL,
    fee_transaction_id      BIGINT         NULL,
    description             VARCHAR(255)   NULL,
    user_id                 BIGINT         NOT NULL,
    created_at              DATETIME(6)    NOT NULL,
    updated_at              DATETIME(6)    NOT NULL,
    created_by              VARCHAR(255)   NOT NULL,
    updated_by              VARCHAR(255)   NULL,
    CONSTRAINT fk_lp_loan FOREIGN KEY (loan_id) REFERENCES loans (id),
    CONSTRAINT fk_lp_payer FOREIGN KEY (payer_account_id) REFERENCES accounts (id),
    CONSTRAINT fk_lp_transfer FOREIGN KEY (transfer_id) REFERENCES transfers (id),
    CONSTRAINT fk_lp_interest FOREIGN KEY (interest_transaction_id) REFERENCES transactions (id),
    CONSTRAINT fk_lp_fee FOREIGN KEY (fee_transaction_id) REFERENCES transactions (id),
    CONSTRAINT fk_lp_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uk_lp_transfer UNIQUE (transfer_id),
    CONSTRAINT uk_lp_interest UNIQUE (interest_transaction_id),
    CONSTRAINT uk_lp_fee UNIQUE (fee_transaction_id),
    CONSTRAINT chk_lp_amounts CHECK (principal_amount >= 0 AND interest_amount >= 0 AND fee_amount >= 0
        AND subsidy_amount >= 0 AND subsidy_amount <= interest_amount
        AND principal_amount + interest_amount + fee_amount > 0)
);
CREATE INDEX idx_lp_loan_date ON loan_payments (loan_id, payment_date);
CREATE INDEX idx_lp_user_date ON loan_payments (user_id, payment_date);