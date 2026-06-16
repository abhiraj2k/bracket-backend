# Expense Tracker MVP1 - Business Logic Breakdown

## 1. User & Account Service

### 1.1 Registration Logic
* The system must extract the user's details from the registration payload.
* [cite_start]The service must first generate a new `household` record and persist it to the database[cite: 419].
* [cite_start]The service must then create the `app_user` record, attaching the newly generated `household_id` to it[cite: 419].
* [cite_start]The user's `base_currency` must default strictly to 'INR'[cite: 209].

### 1.2 Account Creation & Validation Logic
* [cite_start]When parsing an account creation payload, the service must enforce that the `account_type` matches one of the permitted enums: `BANK`, `CREDIT_CARD`, `CASH`, or `LOAN`[cite: 421].
* [cite_start]If the `account_type` is identified as `CREDIT_CARD`, the business logic must validate the billing cycle inputs[cite: 422].
* [cite_start]Specifically, `billing_start_day` and `billing_end_day` must be validated as integers between 1 and 31[cite: 422].
* [cite_start]The service must map all monetary balances securely using `NUMERIC(15, 2)` to prevent any floating-point arithmetic errors in the database[cite: 423].

---

## 2. Metadata Service (Categorization & Tagging)

### 2.1 Caching & Provisioning Logic
* [cite_start]On application startup, the service must load the immutable global dictionary of categories and tags into memory using a caching solution like Spring Cache or Redis[cite: 424].
* [cite_start]The service must listen for a successful user registration event[cite: 425].
* [cite_start]Upon detecting this event, it must trigger an asynchronous process[cite: 425].
* [cite_start]This async process must bulk-insert default category mappings into the `user_category_mapping` table for the new user[cite: 425].

### 2.2 Alias Mutation Logic
* [cite_start]When a user requests to rename a category, the service must intercept the update payload[cite: 426].
* [cite_start]The logic must exclusively update the `custom_alias` field on the user's specific mapping row[cite: 426].
* [cite_start]The service must guarantee that the global dictionary tables remain unmutated by this operation[cite: 426].

---

## 3. Ledger Service

### 3.1 Transaction Integrity Logic
* [cite_start]The method handling the transaction creation must be wrapped entirely within a Spring `@Transactional` boundary to guarantee atomicity[cite: 428].
* [cite_start]Before attempting to save the record, the service must loop through the `transaction_line_item` array and mathematically sum the `amount` values[cite: 429].
* [cite_start]If the calculated sum does not perfectly match the `total_amount` defined in the transaction header, the service must instantly throw a custom `LedgerImbalanceException` and abort the transaction[cite: 430].

### 3.2 Transfer Enforcement Logic
* The service must inspect the `transaction_type` of the incoming payload.
* [cite_start]If the type is `TRANSFER`, the service must enforce that the `dest_account_id` field is populated (NOT NULL)[cite: 431].
* [cite_start]Furthermore, for transfers, the service must iterate through the line items and enforce that every `category_id` is strictly `NULL`[cite: 431].

### 3.3 Rollback & Deletion Logic
* [cite_start]For transaction deletions, the service only needs to target the `transaction_header` record[cite: 433].
* [cite_start]The business logic relies on the database's `ON DELETE CASCADE` constraint to automatically and safely wipe all child line items and tag mappings[cite: 432, 433].

---

## 4. Budget Service

### 4.1 Real-Time Synchronization
* [cite_start]The service must actively listen for internal events published by the Ledger Service whenever an `EXPENSE` is successfully posted[cite: 434].
* [cite_start]Upon receiving the event, the logic must calculate the financial hit and dynamically update the `spent_amount` field on the user's current `budget_period` record[cite: 435].

### 4.2 Zero-Based Rollover Engine
* [cite_start]The service must execute a scheduled job triggered precisely on the 1st of every month at 00:01 AM[cite: 436].
* [cite_start]The engine must query the database for the user's `budget_period` rows from the immediately preceding month[cite: 437].
* [cite_start]The logic must evaluate if the `spent_amount` is greater than the `starting_balance`[cite: 438].
* [cite_start]If true, the service must calculate the exact deficit amount[cite: 438].
* [cite_start]The service must generate the new month's `budget_period` records[cite: 439].
* [cite_start]It must automatically subtract the calculated deficit from the `target_amount` to establish the adjusted `starting_balance` for the new month[cite: 439].
* [cite_start]Finally, the service must programmatically log the deficit amount as a system-generated expense mapped to the "Expense from last month" category[cite: 440].

---

## 5. Scheduler Service

### 5.1 Concurrency & Execution Logic
* [cite_start]The scheduler must be configured using a cron expression (`0 0 0 * * ?`) to execute precisely at midnight[cite: 443].
* [cite_start]Before executing any database queries, the service must attempt to acquire a distributed lock via ShedLock or Quartz to prevent duplicate executions across multiple instances[cite: 444].
* [cite_start]Once the lock is secured, the service must query the `recurring_transaction` table for records where the `next_execution_date` is less than or equal to the `CURRENT_DATE`[cite: 446].

### 5.2 Payload Processing Logic
* [cite_start]For each fetched record, the service must deserialize the `header_template` and `line_items_template` JSONB payloads[cite: 447].
* [cite_start]The deserialized data must be formulated into a standard transaction payload and passed to the Ledger Service[cite: 448].
* [cite_start]The service must capture the execution result (SUCCESS or FAILED) and write it, along with any exception stack traces, to the `recurring_execution_log` table[cite: 449].
* [cite_start]Finally, the logic must calculate and update the `next_execution_date` on the blueprint row for the next recurring cycle[cite: 450].

---

## 6. Reporting Service

### 6.1 Data Aggregation Logic
* [cite_start]To calculate the "Future Expense Bracket", the service must execute a derived read-query that sums the `balance` across all user accounts where the `account_type` is exactly `CREDIT_CARD`[cite: 442].
* [cite_start]For visual dashboard breakdowns (e.g., pie charts), the service must offload the mathematical grouping logic by utilizing PostgreSQL's `GROUP BY` clauses or Spring Data JPA projections, avoiding in-memory JVM calculations[cite: 453].

### 6.2 Query Optimization Logic
* [cite_start]When fetching the chronological ledger, the service must implement pagination (e.g., `Pageable`) to prevent loading massive datasets into memory[cite: 452].
* [cite_start]The service's queries must be explicitly written to target the indexed `transaction_date` columns in the header table, and the `period_month`/`period_year` columns in the budget table[cite: 454].