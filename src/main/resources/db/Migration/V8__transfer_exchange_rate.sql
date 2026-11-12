ALTER TABLE transfers
        ADD COLUMN received_amount DECIMAL(38, 2) NULL,
        ADD COLUMN exchange_rate DECIMAL(30, 12) NULL;

UPDATE transfers
     SET received_amount = amount,
         exchange_rate = 1.000000000000;

ALTER TABLE transfers
        MODIFY COLUMN received_amount DECIMAL(38, 2) NOT NULL,
        MODIFY COLUMN exchange_rate DECIMAL(30, 12) NOT NULL;