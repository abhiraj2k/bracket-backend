This is the perfect next step. Translating the granular system design into a concrete REST API contract bridges the gap between your backend architecture and your frontend React/React Native clients.

Based on our finalized requirements, here is the complete, developer-ready API stub document in markdown format.

Markdown

````
# Expense Tracker MVP1 - REST API Specifications

## Global Headers
* `Authorization`: Bearer `<JWT_TOKEN>` (Assumed for all endpoints after registration)
* `Content-Type`: `application/json`

---

## 1. Identity & Core Wallet Management

### 1.1 Register User
* **Endpoint:** `POST /api/v1/users/register`
* [cite_start]**Description:** Creates a new `app_user` and their overarching `household` record[cite: 419].
* **Request Body:**
```json
{
  "name": "John Doe",
  "email": "john.doe@example.com",
  "password": "securePassword123"
}
````

- **Response (201 Created):**
    

JSON

```
{
  "user_id": "uuid-string",
  "household_id": "uuid-string",
  "base_currency": "INR"
}
```

### 1.2 Create Account/Wallet

- **Endpoint:** `POST /api/v1/accounts`
    
- **Description:** Initializes a new financial wallet. Accepts the initial balance. Enforces specific enums and billing constraints.
    
- **Request Body:**
    

JSON

```
{
  "name": "HDFC Credit Card",
  "account_type": "CREDIT_CARD", 
  "balance": 0.00,
  "currency_code": "INR",
  "billing_start_day": 16,
  "billing_end_day": 15
}
```

_Note: `billing_start_day` and `billing_end_day` are only required if `account_type` is `CREDIT_CARD`_.

## 2. Global Categorization & Tagging

### 2.1 Update Category Mapping Alias

- **Endpoint:** `PUT /api/v1/categories/mapping/{mapping_id}`
    
- **Description:** Updates the `custom_alias` field locally for the user without mutating the immutable global category dictionary.
    
- **Request Body:**
    

JSON

```
{
  "custom_alias": "Survival"
}
```

### 2.2 Create Custom Personal Tag

- **Endpoint:** `POST /api/v1/tags`
    
- **Description:** Creates a hyper-specific tag mapped exclusively to the user (`is_global = FALSE`).
    
- **Request Body:**
    

JSON

```
{
  "name": "My Dog Buster"
}
```

## 3. The Strict Ledger (Transactions)

### 3.1 Create Transaction (Expense/Income/Transfer)

- **Endpoint:** `POST /api/v1/transactions`
    
- **Description:** Submits a complex payload containing the header data and an array of line items.
    
- **Business Rules Enforced:** * The sum of line `amount` values MUST equal the header `total_amount`.
    
    - If `transaction_type` is `TRANSFER`, `dest_account_id` MUST be populated and line items MUST have a `null` category.
        
- **Request Body (Example: Split Expense):**
    

JSON

```
{
  "source_account_id": "uuid-of-hdfc-card",
  "dest_account_id": null,
  "transaction_type": "EXPENSE",
  "total_amount": 3000.00,
  "transaction_date": "2026-06-20T14:30:00Z",
  "note": "Supermarket run",
  "line_items": [
    {
      "category_id": "uuid-of-groceries",
      "amount": 2500.00,
      "tag_ids": ["uuid-of-offline-tag"]
    },
    {
      "category_id": "uuid-of-wants",
      "amount": 500.00,
      "tag_ids": []
    }
  ]
}
```

### 3.2 Delete Transaction

- **Endpoint:** `DELETE /api/v1/transactions/{transaction_id}`
    
- **Description:** Deletes the header. The database relies on `ON DELETE CASCADE` to wipe associated line items and tag mappings instantly.
    

## 4. Budgeting & Zero-Based Rollover

### 4.1 Define Budget Goal

- **Endpoint:** `POST /api/v1/budgets/goals`
    
- **Description:** Establishes a static budgeting rule mapping a target amount to a category.
    
- **Request Body:**
    

JSON

```
{
  "name": "Wants",
  "target_amount": 30000.00
}
```

### 4.2 Get Current Budget Periods

- **Endpoint:** `GET /api/v1/budgets/periods/current`
    
- **Description:** Retrieves the active monthly trackers, displaying the calculated `starting_balance` (adjusted for previous deficits) and current `spent_amount`.
    
- **Response (200 OK):**
    

JSON

```
[
  {
    "period_month": 7,
    "period_year": 2026,
    "goal_name": "Wants",
    "starting_balance": 25000.00, 
    "spent_amount": 2000.00
  }
]
```

## 5. Future Liabilities & Automation

### 5.1 Setup Recurring Transaction

- **Endpoint:** `POST /api/v1/recurring-transactions`
    
- **Description:** Saves JSON templates for the backend Midnight Scheduler to execute automatically.
    
- **Request Body:**
    

JSON

```
{
  "frequency": "MONTHLY",
  "next_execution_date": "2026-07-05",
  "header_template": {
    "source_account_id": "uuid-string",
    "transaction_type": "EXPENSE",
    "total_amount": 199.00
  },
  "line_items_template": [
    {
      "category_id": "uuid-of-entertainment",
      "amount": 199.00
    }
  ]
}
```

## 6. Dashboard & Reporting

### 6.1 Get Chronological Ledger

- **Endpoint:** `GET /api/v1/reports/ledger`
    
- **Description:** Fetches a paginated history of transactions for the ledger view. Hits indexed `transaction_date` columns.
    
- **Query Parameters:** `?page=0&size=20&month=06&year=2026`
    
- **Response (200 OK):**
    

JSON

```
{
  "content": [
    {
      "transaction_id": "uuid-string",
      "date": "2026-06-20",
      "type": "EXPENSE",
      "total": 3000.00
    }
  ],
  "total_pages": 5,
  "current_page": 0
}
```

### 6.2 Get Category Breakdown

- **Endpoint:** `GET /api/v1/reports/breakdown`
    
- **Description:** Aggregates spending by Root Category to populate frontend pie charts. Offloads GROUP BY math to PostgreSQL.
    
- **Query Parameters:** `?month=06&year=2026`
    
- **Response (200 OK):**
    

JSON

```
[
  {
    "category_name": "Necessities",
    "total_spent": 14500.00
  },
  {
    "category_name": "Wants",
    "total_spent": 3000.00
  }
]
```