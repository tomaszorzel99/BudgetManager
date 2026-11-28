-- Kategoria salda początkowego (analiza backendu, sekcje 5.1 i 8.2).
-- Typ INITIAL_BALANCE zamiast INCOME: kategoria znika z listy przychodów
-- i nie da się w niej zapisać zwykłego przychodu.
UPDATE categories
SET type = 'INITIAL_BALANCE'
WHERE system_key = 'INITIAL_BALANCE';

-- Podkategoria salda początkowego dostaje klucz systemowy.
-- Backend szuka jej teraz po kluczu, a nie po nazwie.
UPDATE subcategory
SET system_key = 'INITIAL_BALANCE'
WHERE system_key IS NULL
  AND name = 'Initial Balance'
  AND category_id = (SELECT id FROM categories WHERE system_key = 'INITIAL_BALANCE')
ORDER BY id
LIMIT 1;