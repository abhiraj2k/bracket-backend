# Expense Tracker MVP1 - Agile Features & User Stories

## Feature 1: Identity & Core Wallet Management
**Epic Description:** Establish the foundational user records and the various financial wallets (accounts) required to track real-world cash flow accurately from Day 1.

### Story 1.1: User & Household Registration
**As a** new user, 
**I want to** create a profile that defaults to a household structure, 
[cite_start]**So that** the application is future-proofed for family sharing in MVP2[cite: 182, 183].
* [cite_start]**AC 1:** The backend must create a `household` record and an `app_user` record upon registration[cite: 199].
* [cite_start]**AC 2:** The `app_user` must be linked to the `household_id` via a Foreign Key[cite: 207].
* [cite_start]**AC 3:** The default `base_currency` must be set to 'INR'[cite: 209].

### Story 1.2: Wallet Setup & Day 0 Balances
**As a** user, 
**I want to** create different types of accounts (Bank, Cash, Credit Card) and input an opening balance, 
[cite_start]**So that** my financial math is accurate from the very first day[cite: 83].
* [cite_start]**AC 1:** Users can create an unlimited number of accounts[cite: 240].
* [cite_start]**AC 2:** Supported `account_type` enums must be `BANK`, `CREDIT_CARD`, `CASH`, and `LOAN`[cite: 217].
* [cite_start]**AC 3:** The UI must prompt for an "Opening Balance" during account creation[cite: 83].
* [cite_start]**AC 4:** Financial balances must be stored as `NUMERIC(15, 2)` in PostgreSQL to prevent floating-point rounding errors[cite: 223, 224].

### Story 1.3: Credit Card Billing Cycle Configuration
**As a** user setting up a Credit Card account, 
**I want to** define my billing cycle start and end dates, 
[cite_start]**So that** the system can accurately track my monthly liabilities[cite: 9].
* [cite_start]**AC 1:** If the `account_type` is `CREDIT_CARD`, the UI must require inputs for `billing_start_day` and `billing_end_day` (integers 1-31)[cite: 219, 220].
* [cite_start]**AC 2:** These fields must remain nullable for non-credit card accounts[cite: 219].

---

## Feature 2: The Global Categorization & Tagging Engine
**Epic Description:** Implement a flexible, two-level UI category hierarchy backed by an immutable global dictionary and a cross-pollinating tagging system.

### Story 2.1: Global Category Subscription & Aliasing
**As a** user, 
**I want to** select from a standardized list of categories and optionally rename them for myself, 
[cite_start]**So that** I don't have to build my category tree from scratch, and my changes don't affect other users[cite: 289, 292].
* [cite_start]**AC 1:** The system must maintain an immutable `category` table serving as a Global Dictionary (`is_global = TRUE`)[cite: 293, 302].
* [cite_start]**AC 2:** The backend must automatically map default categories to new users via the `user_category_mapping` junction table[cite: 314].
* [cite_start]**AC 3:** Users can rename a global category locally by updating the `custom_alias` field in their mapping row[cite: 315].
* [cite_start]**AC 4:** The UI must enforce a maximum 2-level hierarchy (Root -> Sub-category) based on the `parent_category_id` adjacency list[cite: 24, 252].

### Story 2.2: Transaction Tagging
**As a** user, 
**I want to** apply multiple granular tags (e.g., Online, Store Name) to my expenses, 
[cite_start]**So that** I can track specific spending habits without cluttering the main category tree[cite: 25, 26].
* [cite_start]**AC 1:** Tags must be stored in a global `tag` pool to prevent database bloat[cite: 308].
* [cite_start]**AC 2:** Users must be able to create custom personal tags (`is_global = FALSE`) that are mapped exclusively to their `user_id`[cite: 317, 318].
* [cite_start]**AC 3:** The UI must allow attaching multiple tags to a single transaction line item via the `transaction_tag_mapping` table[cite: 275].

---

## Feature 3: Strict Ledger & Transaction Workflows
**Epic Description:** Build the core double-entry-style ledger using Spring Boot `@Transactional` constraints to ensure absolute data integrity.

### Story 3.1: Standard Manual Entries & Split Transactions
**As a** user, 
**I want to** log an expense and split it across multiple categories, 
[cite_start]**So that** a single receipt (e.g., Groceries and Wants) is accurately tracked[cite: 85, 86].
* [cite_start]**AC 1:** The system must record a `transaction_header` for the total amount leaving the `source_account_id`[cite: 264, 266].
* [cite_start]**AC 2:** The system must record one or more `transaction_line_item` rows mapping amounts to specific `category_id`s[cite: 270, 273].
* [cite_start]**AC 3:** **Strict Ledger Check:** The backend must throw an exception and rollback if the sum of line item amounts does not exactly equal the header's `total_amount`[cite: 342, 343].
* [cite_start]**AC 4:** The database must enforce `CHECK (total_amount > 0)` to prevent negative entry hacks[cite: 346].

### Story 3.2: Account Transfers & Credit Card Bill Settlement
**As a** user, 
**I want to** transfer money between accounts (e.g., ATM withdrawal, paying a credit card), 
[cite_start]**So that** the cash movement is recorded without falsely inflating my expenses[cite: 49, 84].
* [cite_start]**AC 1:** The user must select a source account and a destination account[cite: 267].
* [cite_start]**AC 2:** The `transaction_type` must be set to `TRANSFER`[cite: 268].
* [cite_start]**AC 3:** **Transfer Integrity:** If type is `TRANSFER`, the backend must strictly enforce that `dest_account_id` IS NOT NULL and `category_id` IS NULL to prevent double-counting expenses[cite: 344].

### Story 3.3: Strict Rollbacks (The "Oops" Factor)
**As a** user, 
**I want to** delete or edit an erroneous transaction, 
[cite_start]**So that** my wallet balances and budget trackers remain perfectly synced[cite: 104].
* [cite_start]**AC 1:** Updates or deletions must execute within a Spring Boot `@Transactional` block[cite: 194].
* [cite_start]**AC 2:** The database must utilize `ON DELETE CASCADE` so deleting a `transaction_header` instantly wipes all associated `transaction_line_item` and `transaction_tag_mapping` rows[cite: 283].

---

## Feature 4: Budgeting & Zero-Based Rollover
**Epic Description:** Enforce financial discipline through static goal brackets and dynamic, zero-based monthly rollovers.

### Story 4.1: Goal Brackets & Thresholds
**As a** user, 
**I want to** map budget goals (e.g., Wants, Investments) to my root categories and receive visual alerts, 
[cite_start]**So that** I know when I am overspending or hitting my savings targets[cite: 14, 15].
* [cite_start]**AC 1:** Users define rules in the `budget_goal` table, setting a `target_amount`[cite: 368, 371].
* [cite_start]**AC 2:** The UI dashboard must display a Yellow Warning when Wants/Needs hit 80% of the target, and a Red Alert at 100%[cite: 46].
* [cite_start]**AC 3:** The UI must display an encouraging message when Investments hit 50%, and a Green Reward at 100%[cite: 46, 47].

### Story 4.2: Zero-Based Monthly Rollover Logic
**As a** user, 
**I want** my budgets to reset on the 1st of the month while penalizing past overspending, 
[cite_start]**So that** I maintain a strict Zero-Based budgeting system[cite: 121, 122].
* [cite_start]**AC 1:** On the 1st of the month, the system generates new `budget_period` rows for the current month[cite: 372].
* [cite_start]**AC 2:** **Deficit Handling:** If the previous month's `spent_amount` exceeds the `starting_balance`, the system automatically deducts the deficit from the new month's `starting_balance` and logs it under an "Expense from last month" category[cite: 126, 379].
* [cite_start]**AC 3:** **Surplus Handling:** Unspent amounts must be categorized strictly as a "Rollover Balance" to prevent artificially inflating standard Income reports[cite: 141, 142].

---

## Feature 5: Future Liabilities & Automation
**Epic Description:** Track outstanding debts dynamically and reduce manual entry friction through schedulers.

### Story 5.1: The "Future Expense Bracket" (Credit Card Liability)
**As a** user, 
**I want to** see exactly how much I owe the bank for my credit card purchases in a dedicated widget, 
[cite_start]**So that** I don't forget about upcoming bills[cite: 56, 59].
* [cite_start]**AC 1:** Any expense logged from a `CREDIT_CARD` account must incrementally increase the "Future Expense Bracket" dashboard widget[cite: 74].
* [cite_start]**AC 2:** Executing a `TRANSFER` from a Bank account to the Credit Card account must clear this widget balance[cite: 75, 76].
* [cite_start]**AC 3:** If the transfer amount exceeds the bracket (due to late fees), the UI must prompt the user to log the difference as an Expense[cite: 76, 77].

### Story 5.2: Recurring Transactions via Scheduler
**As a** user, 
**I want to** set up fixed monthly expenses to log automatically, 
[cite_start]**So that** I don't have to manually type them out every month[cite: 145].
* [cite_start]**AC 1:** Users can configure a `recurring_transaction` with a frequency of DAILY, WEEKLY, MONTHLY, or YEARLY[cite: 358].
* [cite_start]**AC 2:** The backend must run a midnight Spring Scheduler job to read the `next_execution_date` and insert the stored JSON templates into the strict ledger[cite: 355, 359].
* [cite_start]**AC 3:** The scheduler must be secured with a distributed lock (e.g., ShedLock) to prevent duplicate executions across multiple server instances[cite: 363, 364].
* [cite_start]**AC 4:** Every attempt must write a SUCCESS or FAILED entry to the `recurring_execution_log` table for auditing[cite: 391, 392].

---

## Feature 6: Dashboard & Reporting
**Epic Description:** Provide retrospective data analysis through optimized database queries.

### Story 6.1: Historical Ledger & Visual Breakdowns
**As a** user, 
**I want to** view a chronological ledger of my transactions and a pie chart of my spending, 
[cite_start]**So that** I can analyze where my money went[cite: 115, 146].
* [cite_start]**AC 1:** The UI must display a chronological list of transactions filterable by month[cite: 115].
* [cite_start]**AC 2:** The UI must render a visual breakdown (e.g., pie chart) aggregating spending by Root Category[cite: 115].
* [cite_start]**AC 3:** To ensure query performance at scale, the backend must rely on explicitly indexed columns: `transaction_date` in the header, and `period_month`/`period_year` in the budget table[cite: 394, 395].