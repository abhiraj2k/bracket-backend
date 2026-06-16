## **Expense Tracker MVP1 - Implementation & System Design Document**

---

### **1. High-Level Architecture & Technology Stack**

The application is built on an enterprise-grade stack designed for strict financial integrity and future extensibility.

* 
**Client / Frontend Layer:** The mobile application will be built using React Native, while the web application will utilize React. Both clients communicate with the backend via RESTful APIs over HTTPS.


* 
**Application / Backend Layer:** The core framework is Spring Boot paired with Spring Cloud. The backend utilizes a modular microservices architecture, divided into User, Ledger, Budget, and Scheduler Services. Spring Cloud Gateway handles request routing, CORS, and rate-limiting.


* 
**Persistence / Database Layer:** PostgreSQL serves as the primary relational database, chosen for strict ACID compliance to guarantee data integrity. MongoDB remains on standby for non-relational, unstructured data in MVP2.


* 
**Optimization & Infrastructure:** * **Caching:** Redis or Spring Cache will load the immutable Category and Tag dictionaries into memory on application startup to prevent database bottlenecks.


* 
**Distributed Locking:** ShedLock or Quartz Scheduler, coupled with PostgreSQL, prevents duplicate cron job executions across multiple server instances.





---

### **2. Core Database Schema (PostgreSQL)**

The database explicitly uses `NUMERIC(15, 2)` for all monetary balances to prevent floating-point rounding errors.

#### **Identity & Account Layer**

Designed with `household_id` integration from Day 1 to allow seamless scaling to family profiles in MVP2.

| Table | Primary Columns | Data Types & Constraints |
| --- | --- | --- |
| `household` | `id`, `name` | UUID (PK), VARCHAR(100) (Not Null).

 |
| `app_user` | `id`, `household_id`, `base_currency` | UUID (PK), UUID (FK), VARCHAR(3) (Default 'INR').

 |
| `account` | `id`, `account_type`, `balance`, `billing_start_day` | UUID (PK), Enum(BANK, CREDIT_CARD, CASH, LOAN), NUMERIC(15,2), INTEGER (1-31).

 |

#### **The Strict Ledger (Transaction Engine)**

Relies heavily on `ON DELETE CASCADE` and strict `CHECK` constraints to prevent database ghost balances.

| Table | Primary Columns | Data Types & Constraints |
| --- | --- | --- |
| `transaction_header` | `id`, `transaction_type`, `total_amount`, `transaction_date` | UUID (PK), Enum(INCOME, EXPENSE, TRANSFER), NUMERIC(15,2) (> 0), TIMESTAMP (Indexed).

 |
| `transaction_line_item` | `id`, `transaction_id`, `category_id`, `amount` | UUID (PK), UUID (FK - Cascade), UUID (FK), NUMERIC(15,2).

 |

#### **Budgeting & Schedulers**

Separates static budget goals from dynamically generated monthly execution periods to handle zero-based rollover mathematics.

| Table | Primary Columns | Data Types & Constraints |
| --- | --- | --- |
| `budget_goal` | `id`, `name`, `target_amount` | UUID (PK), VARCHAR(50), NUMERIC(15,2).

 |
| `budget_period` | `id`, `period_month`, `starting_balance`, `spent_amount` | UUID (PK), INTEGER (Indexed), NUMERIC(15,2), NUMERIC(15,2).

 |
| `recurring_transaction` | `id`, `next_execution_date`, `header_template` | UUID (PK), DATE (Indexed), JSONB.

 |

---

### **3. Application Workflows & Business Logic**

#### **A. Transaction & Credit Card Ledger Logic**

The backend uses strict `@Transactional` annotations to enforce financial integrity during complex ledger entries.

* 
**Balance Validation:** The backend Java logic must sum the `amount` of all line items; if the sum does not exactly equal the header's `total_amount`, it throws a `LedgerImbalanceException` and aborts the save.


* 
**Transfers:** If the transaction is a `TRANSFER` (like paying a credit card bill), `dest_account_id` must be populated, and line items must have a null category to explicitly prevent double-counting expenses.


* 
**Credit Card Purchases:** Recording an expense to a credit card directly updates the specific wallet balance and immediately impacts the "Future Expense Bracket" (Liability) dashboard calculation.



#### **B. Zero-Based Rollover Engine**

Triggered internally on the 1st of every month at 00:01 AM.

* 
**Deficits:** The engine queries the previous month's `budget_period`. If `spent_amount > starting_balance`, it calculates the deficit, subtracts it from the target goal to create the new month's `starting_balance`, and automatically logs the overspend under an "Expense from last month" category.


* 
**Surpluses:** Unspent money is intentionally tagged as a "Rollover Balance" rather than raw "Income" to prevent artificially skewing historical reporting.



#### **C. Recurring Transaction Scheduler**

A Spring Cron job (`@Scheduled`) executes daily at midnight.

* 
**Locking & Execution:** Attempts to acquire the ShedLock; if acquired, it queries `recurring_transaction` for rows where `next_execution_date <= CURRENT_DATE`.


* 
**Processing:** It deserializes the `JSONB` templates, pushes the payload to the Ledger Service, and securely records the outcome (SUCCESS or FAILED with stack trace) into a `recurring_execution_log` table.



---

### **4. System Level API Specification (Stubs)**

* **`POST /api/v1/accounts`**: Initializes a wallet. Validates that if `account_type` is `CREDIT_CARD`, the payload includes integer values for `billing_start_day` and `billing_end_day` (1-31).


* 
**`PUT /api/v1/categories/mapping/{mapping_id}`**: Updates a user's `custom_alias` locally, protecting the core global categorization dictionary from unauthorized mutation.


* **`POST /api/v1/transactions`**: Submits the `transaction_header` alongside an array of `transaction_line_item` objects. Bound tightly by the `LedgerImbalanceException` logic.


* 
**`GET /api/v1/reports/ledger`**: Retrieves a paginated transaction history, leveraging the explicitly indexed `transaction_date` column for rapid execution.


* 
**`GET /api/v1/reports/breakdown`**: Offloads complex spending aggregation (grouped by Root Category) directly to the PostgreSQL engine via raw SQL `GROUP BY` clauses, saving JVM memory.