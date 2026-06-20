-- budget_service schema: owns budget_goal and budget_period.
-- user_id references user_service.app_user(id) — plain UUID, no cross-schema FK.

CREATE TABLE IF NOT EXISTS budget_goal (
    id            UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID           NOT NULL,
    name          VARCHAR(50)    NOT NULL,
    target_amount NUMERIC(15, 2) NOT NULL,
    is_active     BOOLEAN        NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_budget_goal_user ON budget_goal (user_id);

CREATE TABLE IF NOT EXISTS budget_period (
    id               UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    budget_goal_id   UUID           NOT NULL,
    period_month     INTEGER        NOT NULL,
    period_year      INTEGER        NOT NULL,
    starting_balance NUMERIC(15, 2) NOT NULL,
    spent_amount     NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    CONSTRAINT fk_period_goal       FOREIGN KEY (budget_goal_id) REFERENCES budget_goal(id),
    CONSTRAINT chk_period_month     CHECK (period_month BETWEEN 1 AND 12),
    CONSTRAINT uq_period_goal_month UNIQUE (budget_goal_id, period_month, period_year)
);

CREATE INDEX IF NOT EXISTS idx_budget_period_month ON budget_period (period_month);
CREATE INDEX IF NOT EXISTS idx_budget_period_year  ON budget_period (period_year);
