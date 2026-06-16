# Expense Tracker MVP1 - Service-Level Low-Level Design (LLD)

## 1. User & Account Service
* [cite_start]**Core Responsibility:** Managing identities, households, and real-world wallets[cite: 417, 419].
* [cite_start]**Registration Flow:** Creating a user must sequentially trigger the creation of a `household` record first, and then attach that generated ID to the new `app_user` record[cite: 419].
* [cite_start]**Wallet Initialization:** The account creation method accepts account details and an initial balance[cite: 420].
* [cite_start]**Type Enforcement:** The service strictly enforces the `account_type` enums: BANK, CREDIT_CARD, CASH, and LOAN[cite: 421].
* [cite_start]**Credit Card Validation:** If the payload specifies `account_type = CREDIT_CARD`, the service validates that `billing_start_day` and `billing_end_day` are integers between 1-31[cite: 422].
* [cite_start]**Data Mapping:** All monetary balances are mapped to PostgreSQL `NUMERIC(15, 2)` via JPA/Hibernate to prevent floating-point errors[cite: 423].

---

## 2. Metadata Service (Categorization & Tagging)
* [cite_start]**Core Responsibility:** Managing the immutable Global Dictionary of categories and tags[cite: 424].
* [cite_start]**Caching Strategy:** To optimize performance and prevent constant database hits for dropdown menus, global categories and tags are loaded into memory (using Spring Cache or Redis) on application startup[cite: 424].
* [cite_start]**Async Provisioning:** The service listens for a "User Registration Event" and triggers an asynchronous process to bulk-insert mapping rows into `user_category_mapping`, ensuring new users start with default categories[cite: 425].
* [cite_start]**Alias Mutation:** The update method targets the `custom_alias` field locally for the user, explicitly avoiding any mutation to the global dictionary table[cite: 426].

---

## 3. Ledger Service
* [cite_start]**Core Responsibility:** Acting as the strict transaction engine and fail-safe ledger[cite: 417, 427].
* [cite_start]**Payload Handling:** The service accepts a complex JSON payload containing transaction header data alongside an array of line items[cite: 427].
* [cite_start]**Transactional Boundary:** The entire method responsible for saving the header, line items, and tag mappings is wrapped in Spring's `@Transactional` annotation to ensure atomicity[cite: 428].
* **Ledger Balance Validation:** Before invoking `repository.save()`, the Java logic sums the `amount` of all line items. [cite_start]If this sum does not equal the `total_amount` in the header, the service throws a custom `LedgerImbalanceException` and aborts the operation[cite: 429, 430].
* [cite_start]**Transfer Integrity Validation:** If the incoming `transaction_type` is TRANSFER, the service validates that `dest_account_id` is NOT NULL and that all line items have a `category_id` of NULL[cite: 431].
* [cite_start]**Deletion Logic:** Deletions do not require manual child-record cleanup; the service relies on PostgreSQL's `ON DELETE CASCADE` to instantly wipe associated line items and tag mappings when a header is deleted[cite: 432, 433].

---

## 4. Budget Service
* [cite_start]**Core Responsibility:** Handling real-time budget synchronization and enforcing the zero-based monthly rollover logic[cite: 417, 434, 436].
* **Event-Driven Sync:** The service listens for an internal event published whenever the Ledger Service successfully posts an EXPENSE. [cite_start]It calculates the hit and updates the `spent_amount` in the current month's `budget_period`[cite: 434, 435].
* [cite_start]**Rollover Trigger:** The Zero-Based Rollover Engine is triggered programmatically on the 1st of every month at 00:01 AM[cite: 436].
* [cite_start]**Rollover Execution:** The engine queries the previous month's `budget_period` rows[cite: 437].
* [cite_start]**Deficit Calculation:** If `spent_amount` exceeds the `starting_balance`, the service calculates the deficit[cite: 438].
* [cite_start]**Period Generation:** The service generates the new month's `budget_period`, subtracting the calculated deficit from the `target_amount` to establish the new `starting_balance`[cite: 439].
* [cite_start]**Penalty Logging:** The deficit is automatically logged as a system-generated transaction categorized under "Expense from last month"[cite: 440].

---

## 5. Scheduler Service
* [cite_start]**Core Responsibility:** Automating recurring transactions and maintaining execution logs[cite: 417, 443].
* [cite_start]**Cron Implementation:** The recurring transaction scheduler uses Spring's `@Scheduled(cron = "0 0 0 * * ?")` annotation to run at midnight[cite: 443].
* [cite_start]**Concurrency Control:** To prevent multiple Spring Cloud instances from executing the same job concurrently, the service integrates a distributed locking mechanism (like ShedLock or Quartz) backed by PostgreSQL[cite: 444].
* [cite_start]**Execution Flow:** Once the distributed lock is acquired, the service queries the `recurring_transaction` table for records where `next_execution_date <= CURRENT_DATE`[cite: 445, 446].
* [cite_start]**Payload Deserialization:** The service deserializes the `header_template` and `line_items_template` JSONB payloads[cite: 447].
* [cite_start]**Ledger Invocation:** The deserialized payloads are passed directly to the Ledger Service for execution[cite: 448].
* [cite_start]**Audit Logging:** The result (SUCCESS or FAILED) along with any exception stack traces are written to the `recurring_execution_log` table[cite: 449].
* [cite_start]**Date Advancement:** Finally, the service updates the `next_execution_date` on the blueprint row for the next cycle[cite: 450].

---

## 6. Reporting Service
* [cite_start]**Core Responsibility:** Providing read-optimized data retrieval for dashboards and historical ledgers[cite: 451].
* [cite_start]**Liability Calculation:** The "Future Expense Bracket" widget data is generated via a derived read-query that sums the `balance` of all accounts where `account_type = CREDIT_CARD`[cite: 441, 442].
* [cite_start]**Pagination:** To prevent memory overload, the chronological ledger queries implement Spring Data's `Pageable` interface[cite: 452].
* [cite_start]**Database Optimization:** Queries are designed to strictly hit explicitly indexed columns, such as `transaction_date` in the header table and `period_month`/`period_year` in the budget table[cite: 454].
* **Math Offloading:** For visual breakdowns like pie charts, the service utilizes Spring Data JPA projections or raw SQL `GROUP BY` clauses. [cite_start]This offloads the mathematical aggregation to the PostgreSQL engine rather than calculating it within the JVM memory[cite: 453].