-- scheduler_service schema: owns recurring_transaction, recurring_execution_log, shedlock.
-- user_id references user_service.app_user(id) — plain UUID, no cross-schema FK.
-- header_template / line_items_template are JSONB mirrors of the ledger POST payload.

CREATE TABLE IF NOT EXISTS recurring_transaction (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID        NOT NULL,
    frequency           VARCHAR(20) NOT NULL,
    next_execution_date DATE        NOT NULL,
    header_template     JSONB       NOT NULL,
    line_items_template JSONB       NOT NULL,
    is_active           BOOLEAN     NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_recurrence_freq CHECK (frequency IN ('DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY'))
);

CREATE INDEX IF NOT EXISTS idx_recur_txn_user      ON recurring_transaction (user_id);
CREATE INDEX IF NOT EXISTS idx_recur_txn_next_exec ON recurring_transaction (next_execution_date);

CREATE TABLE IF NOT EXISTS recurring_execution_log (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    recurring_id   UUID        NOT NULL,
    execution_date TIMESTAMP   NOT NULL DEFAULT NOW(),
    status         VARCHAR(20) NOT NULL,
    error_message  TEXT,
    CONSTRAINT fk_log_recurring FOREIGN KEY (recurring_id) REFERENCES recurring_transaction(id),
    CONSTRAINT chk_exec_status  CHECK (status IN ('SUCCESS', 'FAILED'))
);

-- ShedLock distributed lock table (standard schema from ShedLock docs)
CREATE TABLE IF NOT EXISTS shedlock (
    name       VARCHAR(64)  NOT NULL,
    lock_until TIMESTAMP    NOT NULL,
    locked_at  TIMESTAMP    NOT NULL,
    locked_by  VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);
