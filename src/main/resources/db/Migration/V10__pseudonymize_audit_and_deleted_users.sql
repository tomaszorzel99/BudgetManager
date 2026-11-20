-- 1) Kolumny audytu: e-mail -> id użytkownika.
--    Dla kont usuniętych starym kodem pierwotny e-mail siedzi po prefiksie
--    "Deleted" + UUID + "_" (44 znaki), więc odtwarzamy go od 45. znaku.

UPDATE accounts t JOIN users u
    ON t.created_by = IF(u.email LIKE 'Deleted%', SUBSTRING(u.email, 45), u.email)
SET t.created_by = CAST(u.id AS CHAR);
UPDATE accounts t JOIN users u
    ON t.updated_by = IF(u.email LIKE 'Deleted%', SUBSTRING(u.email, 45), u.email)
SET t.updated_by = CAST(u.id AS CHAR);

UPDATE transactions t JOIN users u
    ON t.created_by = IF(u.email LIKE 'Deleted%', SUBSTRING(u.email, 45), u.email)
SET t.created_by = CAST(u.id AS CHAR);
UPDATE transactions t JOIN users u
    ON t.updated_by = IF(u.email LIKE 'Deleted%', SUBSTRING(u.email, 45), u.email)
SET t.updated_by = CAST(u.id AS CHAR);

UPDATE transfers t JOIN users u
    ON t.created_by = IF(u.email LIKE 'Deleted%', SUBSTRING(u.email, 45), u.email)
SET t.created_by = CAST(u.id AS CHAR);
UPDATE transfers t JOIN users u
    ON t.updated_by = IF(u.email LIKE 'Deleted%', SUBSTRING(u.email, 45), u.email)
SET t.updated_by = CAST(u.id AS CHAR);

UPDATE user_group t JOIN users u
    ON t.created_by = IF(u.email LIKE 'Deleted%', SUBSTRING(u.email, 45), u.email)
SET t.created_by = CAST(u.id AS CHAR);
UPDATE user_group t JOIN users u
    ON t.updated_by = IF(u.email LIKE 'Deleted%', SUBSTRING(u.email, 45), u.email)
SET t.updated_by = CAST(u.id AS CHAR);

UPDATE users t JOIN users u
    ON t.created_by = IF(u.email LIKE 'Deleted%', SUBSTRING(u.email, 45), u.email)
SET t.created_by = CAST(u.id AS CHAR);
UPDATE users t JOIN users u
    ON t.updated_by = IF(u.email LIKE 'Deleted%', SUBSTRING(u.email, 45), u.email)
SET t.updated_by = CAST(u.id AS CHAR);

-- 2) Konta usunięte starym kodem: e-mail bez śladu po pierwotnym adresie.
UPDATE users
SET email = CONCAT('deleted-', id, '@deleted.invalid')
WHERE enabled = 0 AND email LIKE 'Deleted%';