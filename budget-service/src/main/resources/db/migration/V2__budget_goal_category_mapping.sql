ALTER TABLE budget_service.budget_goal
    ADD COLUMN budget_type VARCHAR(20) NOT NULL DEFAULT 'CUSTOM',
    ADD CONSTRAINT chk_budget_type CHECK (budget_type IN ('NEEDS', 'WANTS', 'INVESTMENTS', 'CUSTOM'));

CREATE TABLE budget_service.budget_goal_category_mapping (
    budget_goal_id UUID    NOT NULL,
    category_id    UUID    NOT NULL,
    is_default     BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (budget_goal_id, category_id),
    CONSTRAINT fk_bgcm_goal FOREIGN KEY (budget_goal_id) REFERENCES budget_service.budget_goal (id)
);

CREATE INDEX idx_bgcm_category ON budget_service.budget_goal_category_mapping (category_id);
