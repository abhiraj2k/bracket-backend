-- ledger_service schema: owns transaction_header, transaction_line_item, transaction_tag_mapping.
-- Cross-service refs (user_id, source_account_id, dest_account_id, category_id, tag_id)
-- stored as plain UUID — no DB-level FK across schemas; integrity enforced at application layer.

CREATE TABLE IF NOT EXISTS transaction_header (
    id                UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID           NOT NULL,
    source_account_id UUID           NOT NULL,
    dest_account_id   UUID,
    transaction_type  VARCHAR(20)    NOT NULL,
    total_amount      NUMERIC(15, 2) NOT NULL,
    transaction_date  TIMESTAMP      NOT NULL,
    note              TEXT,
    created_at        TIMESTAMP      NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_transaction_type CHECK (transaction_type IN ('INCOME', 'EXPENSE', 'TRANSFER')),
    CONSTRAINT chk_total_amount_pos  CHECK (total_amount > 0)
);

CREATE INDEX IF NOT EXISTS idx_txn_header_user_id ON transaction_header (user_id);
CREATE INDEX IF NOT EXISTS idx_txn_header_date    ON transaction_header (transaction_date);

CREATE TABLE IF NOT EXISTS transaction_line_item (
    id             UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id UUID           NOT NULL,
    category_id    UUID,
    amount         NUMERIC(15, 2) NOT NULL,
    CONSTRAINT fk_line_item_header FOREIGN KEY (transaction_id)
        REFERENCES transaction_header(id) ON DELETE CASCADE,
    CONSTRAINT chk_amount_pos CHECK (amount > 0)
);

CREATE INDEX IF NOT EXISTS idx_line_item_transaction ON transaction_line_item (transaction_id);
CREATE INDEX IF NOT EXISTS idx_line_item_category    ON transaction_line_item (category_id);

-- tag_id references metadata_service.tag(id) — plain UUID, no cross-schema FK
CREATE TABLE IF NOT EXISTS transaction_tag_mapping (
    line_item_id UUID NOT NULL,
    tag_id       UUID NOT NULL,
    PRIMARY KEY (line_item_id, tag_id),
    CONSTRAINT fk_ttm_line_item FOREIGN KEY (line_item_id)
        REFERENCES transaction_line_item(id) ON DELETE CASCADE
);
