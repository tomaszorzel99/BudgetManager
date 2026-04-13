
ALTER TABLE BudgetManager.transactions
    MODIFY COLUMN type varchar(30) NOT NULL;


UPDATE BudgetManager.transactions SET type = 'INCOME' WHERE type = '0';
UPDATE BudgetManager.transactions SET type = 'EXPENSE' WHERE type = '1';
UPDATE BudgetManager.transactions SET type = 'INITIAL_BALANCE' WHERE type = '2';