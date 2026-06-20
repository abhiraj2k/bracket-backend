-- metadata_service schema: owns category, user_category_mapping, tag, user_tag_mapping.
-- is_global=TRUE entries form the immutable Global Dictionary; never UPDATE/DELETE them.

CREATE TABLE IF NOT EXISTS category (
    id                 UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name               VARCHAR(100) NOT NULL,
    parent_category_id UUID,
    category_type      VARCHAR(20)  NOT NULL,
    is_global          BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_category_id) REFERENCES category(id),
    CONSTRAINT chk_category_type  CHECK (category_type IN ('INCOME', 'EXPENSE'))
);

-- user_id references user_service.app_user(id) — plain UUID, no cross-schema FK
CREATE TABLE IF NOT EXISTS user_category_mapping (
    user_id      UUID         NOT NULL,
    category_id  UUID         NOT NULL,
    custom_alias VARCHAR(100),
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
    PRIMARY KEY (user_id, category_id),
    CONSTRAINT fk_ucm_category FOREIGN KEY (category_id) REFERENCES category(id)
);

CREATE TABLE IF NOT EXISTS tag (
    id        UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name      VARCHAR(50) NOT NULL,
    is_global BOOLEAN     NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_tag_name UNIQUE (name)
);

-- user_id references user_service.app_user(id) — plain UUID, no cross-schema FK
CREATE TABLE IF NOT EXISTS user_tag_mapping (
    user_id UUID NOT NULL,
    tag_id  UUID NOT NULL,
    PRIMARY KEY (user_id, tag_id),
    CONSTRAINT fk_utm_tag FOREIGN KEY (tag_id) REFERENCES tag(id)
);
