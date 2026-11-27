-- Nieruchomości: typ konta PROPERTY (accounts.account_type to VARCHAR od V14, więc bez ALTER).
-- Podkategoria dla aktualizacji wyceny: zmiana wartości mieszkania to korekta, nie przychód ani wydatek.
INSERT INTO subcategory (name, category_id, description, system_key)
SELECT 'Wycena nieruchomości', id, 'Zmiana szacowanej wartości nieruchomości', 'PROPERTY_VALUATION'
FROM categories
WHERE system_key = 'ADJUSTMENT';