ALTER TABLE BudgetManager.categories
    MODIFY COLUMN type enum('EXPENSE','INCOME','INITIAL_BALANCE','ADJUSTMENT') NOT NULL;

ALTER TABLE BudgetManager.transactions
    MODIFY COLUMN type varchar(30) NOT NULL;

INSERT INTO BudgetManager.categories (name, color, type, description)
VALUES ('Korekta salda', '#94a3b8', 'ADJUSTMENT', 'Zwroty, korekty, transakcje sprzed aplikacji');

INSERT INTO BudgetManager.subcategory (name, category_id, description)
VALUES ('Zwrot zakupu',
        (SELECT id FROM BudgetManager.categories WHERE name = 'Korekta salda'),
        'Zwrot pieniędzy za zakupy'),
       ('Korekta manualna',
        (SELECT id FROM BudgetManager.categories WHERE name = 'Korekta salda'),
        'Ręczna korekta salda');