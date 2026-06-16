# Expense Tracker MVP1 - Database Schema (PostgreSQL)

## 1. Identity & Core Wallets

**Table: `household`** (Future-proofing for MVP2 family sharing)
| Column Name | Data Type | Constraints / Details |
| :--- | :--- | :--- |
| `id` | UUID | Primary Key (PK) |
| `name` | VARCHAR(100) | Not Null (e.g., "Smith Family") |
| `created_at` | TIMESTAMP | Not Null, Default NOW() |
| `updated_at` | TIMESTAMP | Not Null, Default NOW() |

**Table: `app_user`**
| Column Name | Data Type | Constraints / Details |
| :--- | :--- | :--- |
| `id` | UUID | Primary Key (PK) |
| `household_id` | UUID | FK -> `household(id)`, Indexed |
| `name` | VARCHAR(100) | Not Null |
| `email` | VARCHAR(255) | Unique, Not Null |
| `base_currency` | VARCHAR(3) | Not Null, Default 'INR' |
| `created_at` | TIMESTAMP | Not Null, Default NOW() |

**Table: `account`** (The Wallets & Credit Cards)
| Column Name | Data Type | Constraints / Details |
| :--- | :--- | :--- |
| `id` | UUID | Primary Key (PK) |
| `user_id` | UUID | FK -> `app_user(id)`, Indexed |
| `household_id` | UUID | FK -> `household(id)`, Indexed |
| `name` | VARCHAR(100) | Not Null (e.g., "HDFC Credit Card") |
| `account_type` | VARCHAR(50) | Not Null, Enum: (BANK, CREDIT_CARD, CASH, LOAN) |
| `balance` | NUMERIC(15, 2)| Not Null, Default 0.00 |
| `currency_code`| VARCHAR(3) | Not Null, Default 'INR' |
| `billing_start_day`| INTEGER | Nullable (1-31). Only populated if `account_type` = CREDIT_CARD |
| `billing_end_day`| INTEGER | Nullable (1-31). Only populated if `account_type` = CREDIT_CARD |
| `is_active` | BOOLEAN | Not Null, Default TRUE |
| `created_at` | TIMESTAMP | Not Null, Default NOW() |
| `updated_at` | TIMESTAMP | Not Null, Default NOW() |

---

## 2. Global Dictionaries (Metadata)

**Table: `category`** (Global Adjacency List)
| Column Name | Data Type | Constraints / Details |
| :--- | :--- | :--- |
| `id` | UUID | Primary Key (PK) |
| `name` | VARCHAR(100) | Not Null |
| `parent_category_id`| UUID | FK -> `category(id)`, Nullable |
| `category_type` | VARCHAR(20) | Enum: (INCOME, EXPENSE) |
| `is_global` | BOOLEAN | Not Null, Default TRUE |

**Table: `user_category_mapping`**
| Column Name | Data Type | Constraints / Details |
| :--- | :--- | :--- |
| `user_id` | UUID | FK -> `app_user(id)`, PK Part 1 |
| `category_id` | UUID | FK -> `category(id)`, PK Part 2 |
| `custom_alias` | VARCHAR(100) | Nullable |
| `is_active` | BOOLEAN | Not Null, Default TRUE |

**Table: `tag`** (Global Pool)
| Column Name | Data Type | Constraints / Details |
| :--- | :--- | :--- |
| `id` | UUID | Primary Key (PK) |
| `name` | VARCHAR(50) | Not Null, Unique |
| `is_global` | BOOLEAN | Not Null, Default TRUE |

**Table: `user_tag_mapping`**
| Column Name | Data Type | Constraints / Details |
| :--- | :--- | :--- |
| `user_id` | UUID | FK -> `app_user(id)`, PK Part 1 |
| `tag_id` | UUID | FK -> `tag(id)`, PK Part 2 |

---

## 3. The Strict Ledger (Transactions)

**Table: `transaction_header`**
| Column Name | Data Type | Constraints / Details |
| :--- | :--- | :--- |
| `id` | UUID | Primary Key (PK) |
| `user_id` | UUID | FK -> `app_user(id)`, Indexed |
| `source_account_id` | UUID | FK -> `account(id)`, Not Null |
| `dest_account_id` | UUID | FK -> `account(id)`, Nullable (Used for Transfers) |
| `transaction_type` | VARCHAR(20) | Enum: (INCOME, EXPENSE, TRANSFER) |
| `total_amount` | NUMERIC(15, 2)| Not Null, CHECK (`total_amount` > 0) |
| `transaction_date` | TIMESTAMP | Not Null, Indexed |
| `note` | TEXT | Nullable |
| `created_at` | TIMESTAMP | Not Null, Default NOW() |

**Table: `transaction_line_item`**
| Column Name | Data Type | Constraints / Details |
| :--- | :--- | :--- |
| `id` | UUID | Primary Key (PK) |
| `transaction_id` | UUID | FK -> `transaction_header(id)`, ON DELETE CASCADE |
| `category_id` | UUID | FK -> `category(id)`, Nullable ONLY for Transfers, Indexed |
| `amount` | NUMERIC(15, 2)| Not Null, CHECK (`amount` > 0) |

**Table: `transaction_tag_mapping`**
| Column Name | Data Type | Constraints / Details |
| :--- | :--- | :--- |
| `line_item_id` | UUID | FK -> `transaction_line_item(id)`, ON DELETE CASCADE, PK Part 1 |
| `tag_id` | UUID | FK -> `tag(id)`, PK Part 2 |

---

## 4. Automation & Budgets

**Table: `recurring_transaction`**
| Column Name | Data Type | Constraints / Details |
| :--- | :--- | :--- |
| `id` | UUID | Primary Key (PK) |
| `user_id` | UUID | FK -> `app_user(id)`, Indexed |
| `frequency` | VARCHAR(20) | Enum: (DAILY, WEEKLY, MONTHLY, YEARLY) |
| `next_execution_date` | DATE | Not Null, Indexed |
| `header_template` | JSONB | Stores `transaction_header` payload |
| `line_items_template` | JSONB | Stores `transaction_line_item` array |
| `is_active` | BOOLEAN | Not Null, Default TRUE |

**Table: `recurring_execution_log`** (Scheduler Audit Log)
| Column Name | Data Type | Constraints / Details |
| :--- | :--- | :--- |
| `id` | UUID | Primary Key (PK) |
| `recurring_id` | UUID | FK -> `recurring_transaction(id)` |
| `execution_date` | TIMESTAMP | Not Null, Default NOW() |
| `status` | VARCHAR(20) | Enum: (SUCCESS, FAILED) |
| `error_message` | TEXT | Nullable |

**Table: `budget_goal`** (Static Rules)
| Column Name | Data Type | Constraints / Details |
| :--- | :--- | :--- |
| `id` | UUID | Primary Key (PK) |
| `user_id` | UUID | FK -> `app_user(id)` |
| `name` | VARCHAR(50) | Not Null (e.g., "Wants") |
| `target_amount` | NUMERIC(15, 2)| Not Null |
| `is_active` | BOOLEAN | Not Null, Default TRUE |

**Table: `budget_period`** (Monthly Rollover Tracker)
| Column Name | Data Type | Constraints / Details |
| :--- | :--- | :--- |
| `id` | UUID | Primary Key (PK) |
| `budget_goal_id` | UUID | FK -> `budget_goal(id)` |
| `period_month` | INTEGER | Not Null (1-12), Indexed |
| `period_year` | INTEGER | Not Null (e.g., 2026), Indexed |
| `starting_balance` | NUMERIC(15, 2)| Default `target_amount` |
| `spent_amount` | NUMERIC(15, 2)| Default 0.00 |


# Expense Tracker MVP1 - Database Schema Additions

## 1. Scheduler Execution Log

**Table: `recurring_execution_log`** (Scheduler Audit Log)
| Column Name | Data Type | Constraints / Details |
| :--- | :--- | :--- |
| `id` | UUID | Primary Key (PK) |
| `recurring_id` | UUID | FK -> `recurring_transaction(id)` |
| `execution_date` | TIMESTAMP | Not Null, Default NOW() |
| `status` | VARCHAR(20) | Enum: (SUCCESS, FAILED) |
| `error_message` | TEXT | Nullable |

---

## 2. Reporting Indexes (Updated Tables)

**Table: `transaction_header`** (Updated with Date Indexing)
| Column Name | Data Type | Constraints / Details |
| :--- | :--- | :--- |
| `id` | UUID | Primary Key (PK) |
| `user_id` | UUID | FK -> `app_user(id)`, Indexed |
| `source_account_id` | UUID | FK -> `account(id)`, Not Null |
| `dest_account_id` | UUID | FK -> `account(id)`, Nullable (Used for Transfers) |
| `transaction_type` | VARCHAR(20) | Enum: (INCOME, EXPENSE, TRANSFER) |
| `total_amount` | NUMERIC(15, 2)| Not Null, CHECK (`total_amount` > 0) |
| `transaction_date` | TIMESTAMP | Not Null, **Indexed (Crucial for time-series reports)** |
| `note` | TEXT | Nullable |
| `created_at` | TIMESTAMP | Not Null, Default NOW() |

**Table: `budget_period`** (Updated with Month/Year Indexing)
| Column Name | Data Type | Constraints / Details |
| :--- | :--- | :--- |
| `id` | UUID | Primary Key (PK) |
| `budget_goal_id` | UUID | FK -> `budget_goal(id)` |
| `period_month` | INTEGER | Not Null (1-12), **Indexed** |
| `period_year` | INTEGER | Not Null (e.g., 2026), **Indexed** |
| `starting_balance` | NUMERIC(15, 2)| Default `target_amount` |
| `spent_amount` | NUMERIC(15, 2)| Default 0.00 |
