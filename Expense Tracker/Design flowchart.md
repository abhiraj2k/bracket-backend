Here is a readable, text-based flowchart and decision tree that maps out the entire application workflow, with a special emphasis on the **Credit Card Logic** you requested.

### 1. Account Setup & Onboarding Flow

- **Start:** User creates an account.
    
    - ➔ **Action:** The system creates `household` and `app_user` records.
        
- **Next:** User creates a Wallet/Account.
    
    - ➔ **Action:** Select `account_type`.
        
    - ➔ **Decision:** Is `account_type` set to `CREDIT_CARD`?
        
        - **YES** ➔ Require and validate `billing_start_day` and `billing_end_day` (between 1-31).
            
        - **NO** ➔ Proceed without requiring billing dates.
            
    - ➔ **Action:** User enters the initial balance to guarantee mathematical accuracy from Day 1.
        

### 2. Transaction Entry Flow & Credit Card Logic (The Strict Ledger)

- **Start:** User inputs the transaction amount, date, and source account.
    
- ➔ **Decision:** What is the `transaction_type`?
    
    **PATH A: TRANSFER** (e.g., ATM Cash Withdrawal, Paying a Bill)
    
    - ➔ **Action:** Require a `dest_account_id`.
        
    - ➔ **Rule Validation:** Line items MUST have a `null` category to prevent double-counting your expenses.
        
    - ➔ **Decision:** Is this a Credit Card Bill Payment?
        
        - **YES** ➔ The transfer zeroes out the "Future Expense Bracket" liability.
            
            - ➔ **Decision:** Does the transfer amount exceed the owed liability (e.g., late fees)?
                
                - **YES** ➔ The system prompts the user to log a new, separate standard expense for the fee difference.
                    
    
    **PATH B: EXPENSE or INCOME**
    
    - ➔ **Decision:** Is this a Split Transaction?
        
        - **YES** ➔ User maps multiple line items to different categories and tags (e.g., separating Groceries from Wants on a single receipt).
            
        - **NO** ➔ User creates a single line item.
            
    - ➔ **Rule Validation (The Fail-Safe):** Does the sum of line item amounts exactly equal the header's `total_amount`?
        
        - **NO** ➔ System throws a `LedgerImbalanceException` and performs a strict rollback.
            
        - **YES** ➔ Proceed to save.
            
    - ➔ **Decision:** Was the expense paid using a Credit Card?
        
        - **YES** ➔ Immediately increase the dedicated "Future Expense Bracket" (Liability) widget on the dashboard.
            
    - ➔ **Action:** The Ledger Service publishes an internal event to synchronize with the Budget Service.
        

### 3. Automated Budget Rollover Flow

- **Start:** The Zero-Based Rollover Engine triggers at 00:01 AM on the 1st of the month.
    
- ➔ **Action:** Queries the previous month's `budget_period` rows.
    
- ➔ **Decision:** Is `spent_amount` > `starting_balance`?
    
    - **YES (Deficit)** ➔
        
        - Calculate the deficit amount.
            
        - Subtract the deficit from the `target_amount` to set the new month's `starting_balance`.
            
        - Log the deficit amount as an automated transaction under the "Expense from last month" category.
            
    - **NO (Surplus)** ➔
        
        - Unspent money is categorized as a "Rollover Balance" to prevent artificially inflating standard income reporting.
            

### 4. Scheduler Engine Flow (Recurring Transactions)

- **Start:** Cron job runs daily at midnight.
    
- ➔ **Action:** Attempt to acquire the distributed database lock (e.g., using ShedLock).
    
- ➔ **Decision:** Is the lock acquired successfully?
    
    - **NO** ➔ Abort (another server instance is currently processing the job).
        
    - **YES** ➔ Query `recurring_transaction` where `next_execution_date <= CURRENT_DATE`.
        
        - ➔ **Loop Through Records:**
            
            - Deserialize the `header_template` and `line_items_template` JSONB payloads.
                
            - Pass payloads to the Ledger Service.
                
            - ➔ **Decision:** Did the transaction save successfully?
                
                - **YES** ➔ Write `SUCCESS` to `recurring_execution_log` and update the `next_execution_date`.
                    
                - **NO** ➔ Write `FAILED` to `recurring_execution_log` with the associated exception stack trace.