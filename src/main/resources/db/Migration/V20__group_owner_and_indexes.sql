-- Właściciel grupy (analiza backendu, sekcja 5.2) i porządki w schemacie (sekcja 8.2).

-- Właściciel: osoba, która założyła grupę (created_by to id użytkownika), a jeśli jej już
-- nie ma w grupie, członek z najniższym id.
ALTER TABLE user_group
    ADD COLUMN owner_id BIGINT NULL,
    ADD CONSTRAINT fk_user_group_owner FOREIGN KEY (owner_id) REFERENCES users (id);

UPDATE user_group g
SET g.owner_id = (SELECT m.user_id
                  FROM user_user_group m
                  WHERE m.group_id = g.id
                    AND CAST(m.user_id AS CHAR) = g.created_by
                  LIMIT 1);

UPDATE user_group g
SET g.owner_id = (SELECT MIN(m.user_id) FROM user_user_group m WHERE m.group_id = g.id)
WHERE g.owner_id IS NULL;

-- Indeksy pod listę transakcji (konta grup + daty) i listę przelewów.
CREATE INDEX idx_transaction_account_date ON transactions (account_id, transaction_date);
CREATE INDEX idx_transfer_date ON transfers (transfer_date);

-- Saldo i typ konta są zawsze ustawiane przez aplikację; puste wartości (stare dane) uzupełniamy.
UPDATE accounts SET balance = 0 WHERE balance IS NULL;
UPDATE accounts SET account_type = 'CHECKING' WHERE account_type IS NULL;
ALTER TABLE accounts
    MODIFY COLUMN balance DECIMAL(38, 2) NOT NULL,
    MODIFY COLUMN account_type VARCHAR(30) NOT NULL;