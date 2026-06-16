-- user_service schema: owns household, app_user, account.
-- Schema created by Flyway (spring.flyway.create-schemas=true) before this migration runs.

CREATE TABLE IF NOT EXISTS household (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(100) NOT NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS app_user (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    household_id  UUID        NOT NULL,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    base_currency VARCHAR(3)  NOT NULL DEFAULT 'INR',
    created_at    TIMESTAMP   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_app_user_email    UNIQUE (email),
    CONSTRAINT fk_user_household    FOREIGN KEY (household_id) REFERENCES household(id)
);

CREATE INDEX IF NOT EXISTS idx_app_user_household ON app_user (household_id);

CREATE TABLE IF NOT EXISTS account (
    id                UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID           NOT NULL,
    household_id      UUID           NOT NULL,
    name              VARCHAR(100)   NOT NULL,
    account_type      VARCHAR(20)    NOT NULL,
    balance           NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    currency_code     VARCHAR(3)     NOT NULL DEFAULT 'INR',
    billing_start_day INTEGER,
    billing_end_day   INTEGER,
    is_active         BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP      NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_account_user      FOREIGN KEY (user_id)      REFERENCES app_user(id),
    CONSTRAINT fk_account_household FOREIGN KEY (household_id) REFERENCES household(id),
    CONSTRAINT chk_account_type     CHECK (account_type IN ('BANK', 'CREDIT_CARD', 'CASH', 'LOAN')),
    CONSTRAINT chk_billing_start    CHECK (billing_start_day IS NULL OR billing_start_day BETWEEN 1 AND 31),
    CONSTRAINT chk_billing_end      CHECK (billing_end_day   IS NULL OR billing_end_day   BETWEEN 1 AND 31)
);

CREATE INDEX IF NOT EXISTS idx_account_user      ON account (user_id);
CREATE INDEX IF NOT EXISTS idx_account_household ON account (household_id);
