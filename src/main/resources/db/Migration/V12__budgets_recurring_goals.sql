-- Budżety miesięczne na kategorie, transakcje cykliczne i cele oszczędnościowe.

CREATE TABLE budgets
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT         NOT NULL,
    category_id BIGINT         NOT NULL,
    amount      DECIMAL(38, 2) NOT NULL,
    currency    VARCHAR(3)     NOT NULL,
    created_at  DATETIME(6)    NOT NULL,
    updated_at  DATETIME(6)    NOT NULL,
    created_by  VARCHAR(255)   NOT NULL,
    updated_by  VARCHAR(255)   NULL,
    CONSTRAINT uk_budget_user_category_currency UNIQUE (user_id, category_id, currency),
    CONSTRAINT fk_budget_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_budget_category FOREIGN KEY (category_id) REFERENCES categories (id)
);

CREATE TABLE recurring_transactions
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT         NOT NULL,
    account_id     BIGINT         NOT NULL,
    category_id    BIGINT         NOT NULL,
    subcategory_id BIGINT         NOT NULL,
    type           VARCHAR(30)    NOT NULL,
    amount         DECIMAL(38, 2) NOT NULL,
    description    VARCHAR(255)   NULL,
    frequency      VARCHAR(20)    NOT NULL,
    start_date     DATE           NOT NULL,
    next_date      DATE           NOT NULL,
    end_date       DATE           NULL,
    auto_post      BIT            NOT NULL,
    active         BIT            NOT NULL,
    created_at     DATETIME(6)    NOT NULL,
    updated_at     DATETIME(6)    NOT NULL,
    created_by     VARCHAR(255)   NOT NULL,
    updated_by     VARCHAR(255)   NULL,
    CONSTRAINT fk_recurring_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_recurring_account FOREIGN KEY (account_id) REFERENCES accounts (id),
    CONSTRAINT fk_recurring_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT fk_recurring_subcategory FOREIGN KEY (subcategory_id) REFERENCES subcategory (id)
);

CREATE INDEX idx_recurring_due ON recurring_transactions (active, next_date);

CREATE TABLE savings_goals
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT         NOT NULL,
    name          VARCHAR(100)   NOT NULL,
    target_amount DECIMAL(38, 2) NOT NULL,
    saved_amount  DECIMAL(38, 2) NOT NULL DEFAULT 0,
    currency      VARCHAR(3)     NOT NULL,
    target_date   DATE           NULL,
    account_id    BIGINT         NULL,
    color         VARCHAR(7)     NOT NULL,
    created_at    DATETIME(6)    NOT NULL,
    updated_at    DATETIME(6)    NOT NULL,
    created_by    VARCHAR(255)   NOT NULL,
    updated_by    VARCHAR(255)   NULL,
    CONSTRAINT fk_goal_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_goal_account FOREIGN KEY (account_id) REFERENCES accounts (id)
);
