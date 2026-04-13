ALTER TABLE BudgetManager.accounts
    ADD COLUMN deleted_at datetime NULL DEFAULT NULL;