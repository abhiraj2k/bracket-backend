## 1. High-Level Architecture Overview

- **Client Layer:** React Native (Mobile App) and React (Web App). They will communicate via RESTful APIs over HTTPS.
    
- **API Gateway / Routing:** Spring Cloud Gateway to route requests, handle CORS, and manage basic rate-limiting.
    
- **Application Layer (Backend):** Spring Boot microservices/modules (e.g., User Service, Ledger Service, Budget Service, Scheduler Service).
    
- **Persistence Layer:** PostgreSQL configured for strict ACID compliance.
    

## 2. Implementation by Feature

### Feature 1: Identity & Core Wallet Management

**Component Focus:** User Service & Account Service

- **API Design:** * `POST /api/v1/users/register`: Payload must trigger the creation of a `household` record first, then attach the generated ID to the new `app_user` record.
    
    - `POST /api/v1/accounts`: Accepts account details and the initial balance.
        
- **Service Layer Logic:**
    
    - When an account is created, the system must enforce the `account_type` enums (`BANK`, `CREDIT_CARD`, `CASH`, `LOAN`).
        
    - **Validation Check:** If the payload contains `account_type = CREDIT_CARD`, the service must validate that `billing_start_day` and `billing_end_day` are integers between 1-31.
        
- **Database Implementation:** All monetary balances must be mapped to PostgreSQL `NUMERIC(15, 2)` via JPA/Hibernate to prevent floating-point errors.
    

### Feature 2: Global Categorization & Tagging

**Component Focus:** Metadata Service

- **Caching Strategy (Optimization):** Since the `category` and `tag` tables act as an immutable Global Dictionary, load these records into memory (e.g., Spring Cache or Redis) on application startup to prevent constant DB hits for dropdown menus.
    
- **Service Layer Logic:**
    
    - **User Registration Event:** Upon successful user creation, trigger an asynchronous event to bulk-insert mapping rows into `user_category_mapping` so the user starts with default categories.
        
    - **Custom Overrides:** `PUT /api/v1/categories/mapping/{id}` updates the `custom_alias` field locally for the user without mutating the global table.
        

### Feature 3: The Strict Ledger (Transactions)

**Component Focus:** Ledger Service

- **API Design:** `POST /api/v1/transactions` accepts a complex JSON payload containing the header data and an array of line items.
    
- **Service Layer Logic (The Fail-Safes):**
    
    - **The `@Transactional` Boundary:** The entire method saving the header, line items, and tag mappings must be wrapped in Spring's `@Transactional` annotation.
        
    - **Ledger Balance Check:** Before calling `repository.save()`, the Java logic must sum the `amount` of all line items. If this sum `!= total_amount` in the header, throw a custom `LedgerImbalanceException` and abort.
        
    - **Transfer Integrity:** If the incoming `transaction_type` is `TRANSFER`, validate that `dest_account_id` is NOT NULL and line items have `category_id` as NULL.
        
- **Database Implementation:** Rely on PostgreSQL `ON DELETE CASCADE` for `transaction_line_item` and `transaction_tag_mapping`. When the service deletes a header, Postgres instantly wipes the children.
    

### Feature 4: Budgeting & Zero-Based Rollover

**Component Focus:** Budgeting Service

- **Service Layer Logic:**
    
    - **Real-time Budget Sync:** Every time the Ledger Service successfully posts an `EXPENSE`, it must publish an internal event. The Budget Service listens, calculates the hit, and updates the `spent_amount` in the current month's `budget_period`.
        
    - **Zero-Based Rollover Engine:** * Triggered on the 1st of every month at 00:01 AM.
        
        - Queries the previous month's `budget_period` rows.
            
        - If `spent_amount` > `starting_balance`, calculate the deficit.
            
        - Generate the new month's `budget_period`. Subtract the deficit from the `target_amount` to set the new `starting_balance`.
            
        - Log the deficit as a system-generated transaction under the "Expense from last month" category.
            

### Feature 5: Future Liabilities & Automation

**Component Focus:** Scheduler Service

- **Credit Card Liability Logic:** The "Future Expense Bracket" widget on the frontend is simply a derived read-query. It sums the `balance` of all accounts where `account_type = CREDIT_CARD`.
    
- **Recurring Transaction Scheduler:**
    
    - **Implementation:** Use Spring's `@Scheduled(cron = "0 0 0 * * ?")` to run at midnight.
        
    - **Distributed Locking:** To prevent multiple Spring Cloud instances from running the same job, integrate **ShedLock** or **Quartz** with PostgreSQL.
        
    - **Execution Flow:**
        
        1. Acquire distributed lock.
            
        2. Query `recurring_transaction` where `next_execution_date <= CURRENT_DATE`.
            
        3. Deserialize `header_template` and `line_items_template` JSONB payloads.
            
        4. Pass payloads to the Ledger Service to execute.
            
        5. Write result (SUCCESS/FAILED) and any exception stack traces to `recurring_execution_log`.
            
        6. Update `next_execution_date` on the blueprint row.
            

### Feature 6: Dashboard & Reporting

**Component Focus:** Reporting Service (Read-Optimized)

- **API Design:** `GET /api/v1/reports/ledger` and `GET /api/v1/reports/breakdown`.
    
- **Service Layer Logic:**
    
    - **Pagination:** The chronological ledger must implement Pageable to prevent loading thousands of rows into memory simultaneously.
        
    - **Aggregations:** For the pie charts, utilize Spring Data JPA projections or raw SQL `GROUP BY` clauses to offload the mathematical aggregation of categories to the PostgreSQL engine rather than calculating it in the JVM JVM memory.
        
- **Database Implementation:** Queries will strictly hit the explicitly indexed columns (`transaction_date` in the header table, and `period_month`/`period_year` in the budget table) to ensure rapid response times at scale.