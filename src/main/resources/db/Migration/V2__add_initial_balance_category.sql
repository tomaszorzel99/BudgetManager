INSERT INTO BudgetManager.categories (name, color, type, description)
VALUES ('Initial Balance', '#808080', 'INCOME', 'System category');

INSERT INTO BudgetManager.subcategory (name, category_id, description)
VALUES ('Initial Balance',
        (SELECT id FROM BudgetManager.categories WHERE name = 'Initial Balance'),
        'System subcategory');