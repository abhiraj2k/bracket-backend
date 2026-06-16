# Functional Requirements Document (FRD)
## Project: Expense Tracker Application (MVP1)

### 1. System Overview
[cite_start]The application is a single-user personal finance ledger designed to track income, expenses, and asset/liability balances[cite: 1, 177]. It relies on a strict double-entry-like ledger architecture with an emphasis on Zero-Based Budgeting and proactive credit card liability tracking.

---

### 2. User Onboarding (Day 0 Flow)
[cite_start]**Objective:** Establish an accurate financial baseline before tracking begins[cite: 81].
* [cite_start]**Setup Accounts:** The user must configure their initial accounts/wallets (e.g., Bank Account, Cash Wallet, Credit Card)[cite: 35].
* [cite_start]**Initial Balances:** The system shall prompt the user to input the "Opening Balance" for all created wallets to ensure mathematical accuracy from Day 1[cite: 83].
* [cite_start]**Define Budgets:** The user establishes monthly Goal Brackets (e.g., Wants, Necessities, Investments) mapped 1-to-1 with Root Categories[cite: 14, 15].

---

### 3. Account & Wallet Management
[cite_start]**Objective:** Maintain accurate balances across different storage types to reflect real-world cash flow[cite: 34].
* [cite_start]**Supported Account Types:** Bank, Credit Card, Cash, and Loan[cite: 217].
* [cite_start]**Credit Card Specifics:** * Credit Card accounts must track a billing cycle `billing_start_day` and `billing_end_day`[cite: 219, 220].
    * [cite_start]The system must track outstanding credit card debt via a dedicated dashboard widget known as the "Future Expense Bracket"[cite: 56, 72].

---

### 4. Categorization & Tagging Engine
**Objective:** Allow granular reporting without cluttering the user interface.
* [cite_start]**UI Presentation:** The category tree shall be strictly capped at 2 levels (Root Category -> Sub-category) on the frontend[cite: 24, 162].
* [cite_start]**Tagging:** Deep granularity (e.g., Online, Offline, Store Name) shall be managed via user-defined or global tags mapped to individual transaction line items[cite: 26, 263].
* [cite_start]**Global Dictionary:** Core categories and tags are managed globally by the system to prevent database mutation[cite: 294]. [cite_start]Users can map to these global entities and apply custom aliases (e.g., renaming "Necessities" to "Survival")[cite: 315].

---

### 5. Transaction Workflow (The Ledger)
[cite_start]**Objective:** Provide strict, fail-proof entry of financial movements[cite: 192, 262].
* [cite_start]**Standard Expenses & Income:** Logged manually as Debits and Credits[cite: 13, 157]. [cite_start]A purchase immediately deducts from the specified Account balance and the mapped Budget Goal[cite: 73, 248].
* [cite_start]**Split Transactions:** A single transaction header (total amount leaving an account) can be split into multiple line items mapped to different categories/tags (e.g., separating Groceries from Wants on a single supermarket receipt)[cite: 86, 87].
* [cite_start]**Transfers:** Moving money between owned accounts (e.g., ATM withdrawals) must be logged as a `TRANSFER`[cite: 84]. [cite_start]Transfers do not hit Expense/Income categories to prevent double-counting[cite: 49].
* [cite_start]**Credit Card Payments:** * Paying a credit card bill is executed as a `TRANSFER` from a Bank Account to the Credit Card Account[cite: 38, 39]. 
    * [cite_start]This clears the "Future Expense Bracket" liability to zero[cite: 76]. 
    * [cite_start]Overages (like late fees) will prompt the user to log the difference as a distinct Expense[cite: 76, 77].
* [cite_start]**Reversals/Refunds:** Handled by reversing the transaction type (e.g., logging an Income to an Expense category), as the system strictly prohibits negative monetary entries[cite: 346, 347].
* [cite_start]**CRUD Operations:** Any deletion or modification of a transaction executes a strict database rollback, simultaneously reverting account balances and budget trackers[cite: 104, 133].

---

### 6. Budgeting & Zero-Based Rollover Logic
[cite_start]**Objective:** Enforce financial discipline through strict month-to-month tracking[cite: 135].
* [cite_start]**Threshold Rules:** The dashboard shall display dynamic warnings or rewards based on spending[cite: 16]. 
    * [cite_start]*Example:* Exceeding 80% of "Wants" triggers a warning; hitting 100% of "Investments" triggers a positive reward[cite: 46, 47].
* **Month-End Rollover (Zero-Based):**
    * [cite_start]Budgets reset to their baseline target amount on the 1st of every month[cite: 170, 366].
    * [cite_start]**Deficits:** If a user overspends a bracket (e.g., Wants goes over by ₹5,000), the system automatically deducts ₹5,000 from the *new* month's Wants bracket, logging it under an "Expense from last month" category[cite: 137].
    * [cite_start]**Surpluses:** Unspent budget is carried over as a "Rollover Balance" (preventing artificial inflation of standard Income reports)[cite: 141, 142].

---

### 7. Automation & Quality of Life
[cite_start]**Objective:** Reduce the friction of manual data entry[cite: 111, 112].
* [cite_start]**Transaction Duplication:** Users can manually duplicate historical transactions via the UI[cite: 127].
* [cite_start]**Recurring Transactions:** Users can configure fixed expenses (e.g., Netflix, Rent) to recur on daily, weekly, monthly, or yearly intervals[cite: 358].
* [cite_start]**Scheduler Execution:** A server-side scheduler will automatically post these entries to the ledger on the target date[cite: 351, 353]. [cite_start]An execution log will track success/failure rates for system auditing[cite: 390, 391].

---

### 8. Reporting & Dashboards
[cite_start]**Objective:** Provide actionable retrospective insights[cite: 114].
* [cite_start]**Chronological Ledger:** A complete, scrollable history of all line items and transfers[cite: 115].
* [cite_start]**Visual Breakdowns:** Standard charts (e.g., pie charts) demonstrating expenditure distribution by Root Category over a specified timeframe[cite: 115, 173].

---

### 9. Out of Scope (Deferred to MVP2)
To maintain development velocity, the following features are intentionally excluded from MVP1:
1. [cite_start]Handling complex debt/loan workflows (e.g., automated EMI Principal vs. Interest splits)[cite: 90, 97].
2. [cite_start]CSV Data Export/Portability[cite: 147].
3. [cite_start]Multi-currency transaction support and live conversion rates[cite: 184, 186].
4. [cite_start]Multi-user / Family profile sharing functionality[cite: 182, 183].