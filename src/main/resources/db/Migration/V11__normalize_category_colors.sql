-- 1) Jeden zapis: wielkie litery, bez spacji.
UPDATE categories
SET color = UPPER(TRIM(color))
WHERE color IS NOT NULL;

-- 2) Puste i niepoprawne wartości (np. 'red', '#FFF') -> kolor, który front pokazuje dziś.
UPDATE categories
SET color = '#6575E9'
WHERE color IS NULL OR color NOT REGEXP '^#[0-9A-F]{6}$';

-- 3) Od teraz kolor jest wymagany także w bazie.
ALTER TABLE categories MODIFY COLUMN color VARCHAR(7) NOT NULL;