ALTER TABLE transactions
    MODIFY COLUMN subcategory_id BIGINT NULL;

ALTER TABLE recurring_transactions
    MODIFY COLUMN subcategory_id BIGINT NULL;