-- Kategorie i podkategorie należą do grupy (analiza backendu, punkt 4).
-- Kategorie systemowe (system_key) zostają wspólne: group_id = NULL.
-- Kategoria używana w kilku grupach dostaje osobną kopię w każdej z nich,
-- a transakcje, stałe wydatki i budżety przechodzą na kopię swojej grupy.

ALTER TABLE categories
    ADD COLUMN group_id BIGINT NULL,
    ADD CONSTRAINT fk_category_group FOREIGN KEY (group_id) REFERENCES user_group (id);
ALTER TABLE subcategory
    ADD COLUMN group_id BIGINT NULL,
    ADD CONSTRAINT fk_subcategory_group FOREIGN KEY (group_id) REFERENCES user_group (id);

-- Nazwa kategorii przestaje być unikalna w całej bazie (będzie unikalna w grupie).
-- Indeks szukamy po kolumnie, bo jego nazwa zależy od tego, kto utworzył schemat.
SET @v19_idx = (SELECT s.INDEX_NAME
                FROM information_schema.STATISTICS s
                WHERE s.TABLE_SCHEMA = DATABASE()
                  AND s.TABLE_NAME = 'categories'
                  AND s.NON_UNIQUE = 0
                  AND s.COLUMN_NAME = 'name'
                  AND (SELECT COUNT(*)
                       FROM information_schema.STATISTICS s2
                       WHERE s2.TABLE_SCHEMA = s.TABLE_SCHEMA
                         AND s2.TABLE_NAME = s.TABLE_NAME
                         AND s2.INDEX_NAME = s.INDEX_NAME) = 1
                LIMIT 1);
SET @v19_sql = IF(@v19_idx IS NULL, 'DO 0', CONCAT('ALTER TABLE categories DROP INDEX `', @v19_idx, '`'));
PREPARE v19_stmt FROM @v19_sql;
EXECUTE v19_stmt;
DEALLOCATE PREPARE v19_stmt;

-- Wbudowane podkategorie „Korekta salda” (V7) zostają wspólne jak sama kategoria.
UPDATE subcategory
SET system_key = 'ADJUSTMENT_REFUND'
WHERE system_key IS NULL
  AND name = 'Zwrot zakupu'
  AND category_id = (SELECT id FROM categories WHERE system_key = 'ADJUSTMENT')
ORDER BY id
LIMIT 1;
UPDATE subcategory
SET system_key = 'ADJUSTMENT_MANUAL'
WHERE system_key IS NULL
  AND name = 'Korekta manualna'
  AND category_id = (SELECT id FROM categories WHERE system_key = 'ADJUSTMENT')
ORDER BY id
LIMIT 1;

-- ---------------------------------------------------------------- kategorie użytkowników

-- Pary (kategoria, grupa): w jakich grupach kategoria jest używana.
CREATE TABLE v19_category_usage
(
    category_id BIGINT NOT NULL,
    group_id    BIGINT NOT NULL,
    PRIMARY KEY (category_id, group_id)
);

-- Transakcje i stałe wydatki: grupa konta.
INSERT IGNORE INTO v19_category_usage (category_id, group_id)
SELECT DISTINCT t.category_id, a.group_id
FROM transactions t
         JOIN accounts a ON a.id = t.account_id
         JOIN categories c ON c.id = t.category_id
WHERE c.system_key IS NULL;

INSERT IGNORE INTO v19_category_usage (category_id, group_id)
SELECT DISTINCT r.category_id, a.group_id
FROM recurring_transactions r
         JOIN accounts a ON a.id = r.account_id
         JOIN categories c ON c.id = r.category_id
WHERE c.system_key IS NULL;

-- Budżety: pierwsza grupa właściciela budżetu, chyba że kategoria jest już w którejś z jego grup.
INSERT IGNORE INTO v19_category_usage (category_id, group_id)
SELECT b.category_id, MIN(ug.group_id)
FROM budgets b
         JOIN categories c ON c.id = b.category_id
         JOIN user_user_group ug ON ug.user_id = b.user_id
WHERE c.system_key IS NULL
  AND NOT EXISTS (SELECT 1
                  FROM v19_category_usage u
                           JOIN user_user_group ug2 ON ug2.group_id = u.group_id
                  WHERE u.category_id = b.category_id
                    AND ug2.user_id = b.user_id)
GROUP BY b.category_id, b.user_id;

-- Kategorie jeszcze nieużywane dostaje każda grupa: każdy widzi to, co widział dotąd.
INSERT INTO v19_category_usage (category_id, group_id)
SELECT c.id, g.id
FROM categories c
         CROSS JOIN user_group g
WHERE c.system_key IS NULL
  AND NOT EXISTS (SELECT 1 FROM v19_category_usage u WHERE u.category_id = c.id);

-- Oryginał zostaje w grupie o najniższym id.
UPDATE categories c
    JOIN (SELECT category_id, MIN(group_id) AS group_id
          FROM v19_category_usage
          GROUP BY category_id) o ON o.category_id = c.id
SET c.group_id = o.group_id;

-- Pozostałe grupy dostają kopie (v19_from = id oryginału).
ALTER TABLE categories ADD COLUMN v19_from BIGINT NULL;
INSERT INTO categories (name, color, type, description, group_id, v19_from)
SELECT c.name, c.color, c.type, c.description, u.group_id, c.id
FROM v19_category_usage u
         JOIN categories c ON c.id = u.category_id
WHERE u.group_id <> c.group_id;

-- Podkategorie kategorii użytkowników: grupa kategorii, a pod kopią kategorii kopia podkategorii.
UPDATE subcategory s
    JOIN categories c ON c.id = s.category_id
SET s.group_id = c.group_id
WHERE c.system_key IS NULL
  AND c.v19_from IS NULL
  AND s.system_key IS NULL;

ALTER TABLE subcategory ADD COLUMN v19_from BIGINT NULL;
INSERT INTO subcategory (name, description, category_id, group_id, v19_from)
SELECT s.name, s.description, cc.id, cc.group_id, s.id
FROM subcategory s
         JOIN categories cc ON cc.v19_from = s.category_id
WHERE s.system_key IS NULL;

-- ---------------------------------------------------------------- podkategorie użytkowników w kategoriach systemowych

CREATE TABLE v19_subcategory_usage
(
    subcategory_id BIGINT NOT NULL,
    group_id       BIGINT NOT NULL,
    PRIMARY KEY (subcategory_id, group_id)
);

INSERT IGNORE INTO v19_subcategory_usage (subcategory_id, group_id)
SELECT DISTINCT t.subcategory_id, a.group_id
FROM transactions t
         JOIN accounts a ON a.id = t.account_id
         JOIN subcategory s ON s.id = t.subcategory_id
         JOIN categories c ON c.id = s.category_id
WHERE s.system_key IS NULL
  AND c.system_key IS NOT NULL;

INSERT IGNORE INTO v19_subcategory_usage (subcategory_id, group_id)
SELECT DISTINCT r.subcategory_id, a.group_id
FROM recurring_transactions r
         JOIN accounts a ON a.id = r.account_id
         JOIN subcategory s ON s.id = r.subcategory_id
         JOIN categories c ON c.id = s.category_id
WHERE s.system_key IS NULL
  AND c.system_key IS NOT NULL;

INSERT INTO v19_subcategory_usage (subcategory_id, group_id)
SELECT s.id, g.id
FROM subcategory s
         JOIN categories c ON c.id = s.category_id
         CROSS JOIN user_group g
WHERE s.system_key IS NULL
  AND c.system_key IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM v19_subcategory_usage u WHERE u.subcategory_id = s.id);

UPDATE subcategory s
    JOIN (SELECT subcategory_id, MIN(group_id) AS group_id
          FROM v19_subcategory_usage
          GROUP BY subcategory_id) o ON o.subcategory_id = s.id
SET s.group_id = o.group_id;

INSERT INTO subcategory (name, description, category_id, group_id, v19_from)
SELECT s.name, s.description, s.category_id, u.group_id, s.id
FROM v19_subcategory_usage u
         JOIN subcategory s ON s.id = u.subcategory_id
WHERE u.group_id <> s.group_id;

-- ---------------------------------------------------------------- przepięcie wpisów na kopie

UPDATE transactions t
    JOIN accounts a ON a.id = t.account_id
    JOIN categories cc ON cc.v19_from = t.category_id AND cc.group_id = a.group_id
SET t.category_id = cc.id;

UPDATE transactions t
    JOIN accounts a ON a.id = t.account_id
    JOIN subcategory sc ON sc.v19_from = t.subcategory_id AND sc.group_id = a.group_id
SET t.subcategory_id = sc.id;

UPDATE recurring_transactions r
    JOIN accounts a ON a.id = r.account_id
    JOIN categories cc ON cc.v19_from = r.category_id AND cc.group_id = a.group_id
SET r.category_id = cc.id;

UPDATE recurring_transactions r
    JOIN accounts a ON a.id = r.account_id
    JOIN subcategory sc ON sc.v19_from = r.subcategory_id AND sc.group_id = a.group_id
SET r.subcategory_id = sc.id;

-- Budżet zostaje przy oryginale, jeśli jego właściciel jest w grupie oryginału;
-- inaczej przechodzi na kopię z pierwszej grupy właściciela.
CREATE TABLE v19_budget_move
(
    budget_id   BIGINT NOT NULL PRIMARY KEY,
    category_id BIGINT NOT NULL
);
INSERT INTO v19_budget_move (budget_id, category_id)
SELECT b.id, MIN(cc.id)
FROM budgets b
         JOIN categories c ON c.id = b.category_id
         JOIN categories cc ON cc.v19_from = c.id
         JOIN user_user_group ug ON ug.group_id = cc.group_id AND ug.user_id = b.user_id
WHERE NOT EXISTS (SELECT 1
                  FROM user_user_group ug2
                  WHERE ug2.user_id = b.user_id
                    AND ug2.group_id = c.group_id)
GROUP BY b.id;
UPDATE budgets b
    JOIN v19_budget_move m ON m.budget_id = b.id
SET b.category_id = m.category_id;

-- ---------------------------------------------------------------- sprzątanie i nowe ograniczenia

DROP TABLE v19_budget_move;
DROP TABLE v19_subcategory_usage;
DROP TABLE v19_category_usage;
ALTER TABLE subcategory DROP COLUMN v19_from;
ALTER TABLE categories DROP COLUMN v19_from;

ALTER TABLE categories
    ADD CONSTRAINT uk_category_group_name_type UNIQUE (group_id, name, type);