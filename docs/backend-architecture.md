# Backend Architecture & API Reference

Complete reference for the Expense Tracker backend. Written for a UI engineer who needs to integrate against the API and for a new Claude context picking up this codebase.

---

## Table of Contents

1. [System Overview](#1-system-overview)
2. [Auth & Security](#2-auth--security)
3. [Standard Response Shapes](#3-standard-response-shapes)
4. [Common Enums](#4-common-enums)
5. [Service: User Service (port 8081)](#5-service-user-service-port-8081)
6. [Service: Metadata Service (port 8082)](#6-service-metadata-service-port-8082)
7. [Service: Ledger Service (port 8083)](#7-service-ledger-service-port-8083)
8. [Service: Budget Service (port 8084)](#8-service-budget-service-port-8084)
9. [Service: Scheduler Service (port 8085)](#9-service-scheduler-service-port-8085)
10. [Service: Reporting Service (port 8086)](#10-service-reporting-service-port-8086)
11. [Error Handling](#11-error-handling)
12. [Cross-Service Flows](#12-cross-service-flows)
13. [Domain Rules & Invariants](#13-domain-rules--invariants)

---

## 1. System Overview

### Architecture

```
Client (React / React Native)
        │  HTTPS
        ▼
 ┌─────────────────────────────────────────┐
 │  Gateway Service  (port 8080)           │
 │  • JWT validation (rejects 401 if bad)  │
 │  • Injects X-User-Id header             │
 │  • Load-balanced routing (lb://)        │
 └────────────┬────────────────────────────┘
              │  Internal HTTP (via Eureka lb)
    ┌─────────┼─────────────────────────────────┐
    │         │                                  │
user-service  metadata-service  ledger-service   budget-service
(8081)        (8082)            (8083)           (8084)
                                                 scheduler-service (8085)
                                                 reporting-service (8086)
```

### Infrastructure

| Component | Tech | Notes |
|-----------|------|-------|
| Service discovery | Eureka (port 8761) | All services register; gateway resolves `lb://service-name` |
| Config server | Spring Cloud Config (port 8888) | Reads from `config-repo/` directory |
| Database | PostgreSQL 16 | One DB, per-service schemas |
| Cache | Redis 7 | metadata-service caches categories + tags per user |
| Distributed lock | ShedLock (JDBC) | Guards the midnight recurring-transaction cron |

### Entry Point for UI

**All API calls go through the gateway: `http://localhost:8080`**

Do not call individual service ports directly. The gateway handles auth, CORS, and routing.

CORS is open (`allowed-origins: "*"`) in dev — all HTTP methods and headers permitted.

---

## 2. Auth & Security

### How it works

1. UI calls `POST /api/v1/users/register` or `POST /api/v1/auth/login` — these are the only **public** routes (no token required).
2. Both return an `accessToken` (JWT, 24-hour TTL).
3. Every other request must include `Authorization: Bearer <accessToken>`.
4. The gateway parses the JWT, extracts the `sub` claim (userId UUID), and forwards the request downstream with an `X-User-Id: <uuid>` header. Domain services read this header — they do not parse JWTs themselves.
5. Invalid or missing token → **401 Unauthorized** from the gateway, request never reaches the service.

### JWT Claims

| Claim | Value |
|-------|-------|
| `sub` | userId (UUID string) |
| `hid` | householdId (UUID string) |
| `iat` | issued-at (epoch seconds) |
| `exp` | issued-at + 86400 seconds |

### Storing the token

Store `accessToken` in memory or secure storage. Send it as a Bearer token on every request after login/register.

---

## 3. Standard Response Shapes

### Success: `ApiResponse<T>`

```json
{
  "success": true,
  "message": "Success",
  "data": { ... }
}
```

The `data` field holds the typed payload. `message` is a human-readable status string.

### Paginated: `ApiResponse<PageResponse<T>>`

```json
{
  "success": true,
  "message": "Success",
  "data": {
    "content": [ ... ],
    "currentPage": 0,
    "totalPages": 5,
    "totalElements": 97,
    "pageSize": 20
  }
}
```

### Error: `ErrorResponse`

```json
{
  "status": 400,
  "error": "Validation Failed",
  "message": "Request validation failed",
  "path": "/api/v1/accounts",
  "fieldErrors": {
    "name": "must not be blank",
    "openingBalance": "must be greater than or equal to 0.0"
  }
}
```

`fieldErrors` is only present for validation failures (400). Other errors omit it.

---

## 4. Common Enums

These strings are used in request/response bodies across multiple endpoints.

### `AccountType`
```
BANK | CREDIT_CARD | CASH | LOAN
```

### `TransactionType`
```
INCOME | EXPENSE | TRANSFER
```

### `CategoryType`
```
INCOME | EXPENSE
```
Describes what kind of transactions a category applies to.

### `RecurrenceFrequency`
```
DAILY | WEEKLY | MONTHLY | YEARLY
```

### `ExecutionStatus` (internal, on recurring logs)
```
SUCCESS | FAILED
```

### Budget `alertLevel` (computed, on BudgetPeriodResponse)
```
OK | WARNING | MILESTONE | ALERT
```
- `NEEDS` / `WANTS` goals: `WARNING` at ≥ 80%, `ALERT` at ≥ 100%
- `INVESTMENTS` goals: `MILESTONE` at ≥ 50%, `ALERT` at ≥ 100%
- `CUSTOM` goals: `ALERT` at ≥ 100% only

### Budget `budgetType`
```
NEEDS | WANTS | INVESTMENTS | CUSTOM
```

---

## 5. Service: User Service (port 8081)

Manages user identity, household records, and financial accounts.

Gateway routes: `/api/v1/users/**`, `/api/v1/auth/**`, `/api/v1/accounts/**`

---

### Models

#### `AppUser`

| Field | Type | Notes |
|-------|------|-------|
| `id` | UUID | PK, auto-generated |
| `householdId` | UUID | FK to `Household.id` — set at registration |
| `name` | String | max 100 chars |
| `email` | String | unique, max 255 chars |
| `passwordHash` | String | BCrypt hash, never returned in API |
| `baseCurrency` | String | default `"INR"` |
| `createdAt` | Instant | set on insert, never updated |

#### `Household`

| Field | Type | Notes |
|-------|------|-------|
| `id` | UUID | PK |
| `name` | String | Set to `"<UserName>'s Household"` at registration |
| `createdAt` | Instant | auditable |
| `updatedAt` | Instant | auditable |

#### `Account`

| Field | Type | Notes |
|-------|------|-------|
| `id` | UUID | PK |
| `userId` | UUID | owner |
| `householdId` | UUID | copied from user at creation |
| `name` | String | max 100 chars |
| `accountType` | `AccountType` | BANK / CREDIT_CARD / CASH / LOAN |
| `balance` | BigDecimal | NUMERIC(15,2); opening balance set on creation; **not auto-updated** — balance tracking is a UI concern for MVP1 |
| `currencyCode` | String | default `"INR"` |
| `billingStartDay` | Integer | 1–31, **only for CREDIT_CARD** |
| `billingEndDay` | Integer | 1–31, **only for CREDIT_CARD** |
| `isActive` | Boolean | default `true`; set to `false` to soft-delete |
| `createdAt` | Instant | auditable |
| `updatedAt` | Instant | auditable |

---

### Endpoints

#### `POST /api/v1/users/register` — PUBLIC (no token required)

Register a new user. Creates the user, a personal household, and returns a JWT.

**Request body:**
```json
{
  "name": "Alice",
  "email": "alice@example.com",
  "password": "SecurePass123"
}
```
All fields required. `password` min 1 char (no complexity rule enforced server-side — add on UI).

**Business logic:**
1. Check `email` not already taken → 400 `ValidationException` if duplicate.
2. Create `Household` with name `"Alice's Household"`.
3. Create `AppUser` with BCrypt-hashed password.
4. Issue JWT (`sub` = userId, `hid` = householdId, TTL 24h).
5. Return `AuthResponse`.

**Response (201):**
```json
{
  "success": true,
  "message": "User registered",
  "data": {
    "accessToken": "<jwt>",
    "userId": "uuid",
    "householdId": "uuid",
    "name": "Alice",
    "email": "alice@example.com"
  }
}
```

---

#### `POST /api/v1/auth/login` — PUBLIC (no token required)

**Request body:**
```json
{
  "email": "alice@example.com",
  "password": "SecurePass123"
}
```

**Business logic:**
1. Look up user by email → 400 "Invalid email or password" if not found (deliberately vague for security).
2. BCrypt verify password → 400 "Invalid email or password" if mismatch.
3. Issue JWT, return `AuthResponse`.

**Response (200):** Same shape as register.

---

#### `GET /api/v1/users/me` — AUTH REQUIRED

Fetch the authenticated user's profile.

**Response (200):**
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "userId": "uuid",
    "householdId": "uuid",
    "name": "Alice",
    "email": "alice@example.com",
    "baseCurrency": "INR"
  }
}
```

---

#### `POST /api/v1/accounts` — AUTH REQUIRED

Create a financial account/wallet.

**Request body:**
```json
{
  "name": "HDFC Savings",
  "accountType": "BANK",
  "openingBalance": 50000.00,
  "currencyCode": "INR",
  "billingStartDay": null,
  "billingEndDay": null
}
```

For `CREDIT_CARD` accounts, `billingStartDay` and `billingEndDay` are **required** (integers 1–31). For all other types, leave null.

**Business logic:**
1. Verify user exists.
2. If `CREDIT_CARD`, validate both billing days are present and in range 1–31.
3. Create account with `balance = openingBalance`.
4. Returns created account.

**Response (201):**
```json
{
  "success": true,
  "message": "Account created",
  "data": {
    "id": "uuid",
    "name": "HDFC Savings",
    "accountType": "BANK",
    "balance": 50000.00,
    "currencyCode": "INR",
    "billingStartDay": null,
    "billingEndDay": null,
    "isActive": true
  }
}
```

---

#### `GET /api/v1/accounts` — AUTH REQUIRED

List all active accounts for the authenticated user.

**Response (200):**
```json
{
  "success": true,
  "message": "Success",
  "data": [
    {
      "id": "uuid",
      "name": "HDFC Savings",
      "accountType": "BANK",
      "balance": 50000.00,
      "currencyCode": "INR",
      "billingStartDay": null,
      "billingEndDay": null,
      "isActive": true
    }
  ]
}
```

---

#### `PATCH /api/v1/accounts/{id}` — AUTH REQUIRED

Update account name or active status (soft-delete by setting `isActive: false`).

**Request body** (all fields optional — only send what changes):
```json
{
  "name": "New Account Name",
  "isActive": false
}
```

**Business logic:**
1. Load account by id → 404 if not found.
2. Verify `account.userId == authenticatedUserId` → 400 if not.
3. Apply only non-null fields.

**Response (200):** Updated `AccountResponse`.

---

## 6. Service: Metadata Service (port 8082)

Manages the global category dictionary, per-user category aliases, global tags, and personal tags.
Uses Redis to cache per-user category and tag lists.

Gateway routes: `/api/v1/categories/**`, `/api/v1/tags/**`

---

### Models

#### `Category` (global, immutable rows)

| Field | Type | Notes |
|-------|------|-------|
| `id` | UUID | PK |
| `name` | String | canonical name, e.g. "Food & Dining" |
| `parentCategoryId` | UUID | null = root category; set = sub-category |
| `categoryType` | `CategoryType` | INCOME or EXPENSE |
| `isGlobal` | Boolean | always `true` for seed data |

The category table is a **read-only dictionary** — no API to create/edit categories directly. The UI only ever reads categories through the user-mapping layer below.

#### `UserCategoryMapping`

| Field | Type | Notes |
|-------|------|-------|
| `userId` | UUID | composite PK part 1 |
| `categoryId` | UUID | composite PK part 2; FK to `category.id` |
| `customAlias` | String | optional user-defined rename, max 100 chars |
| `isActive` | Boolean | default `true` |

#### `Tag` (global or personal)

| Field | Type | Notes |
|-------|------|-------|
| `id` | UUID | PK |
| `name` | String | unique, max 50 chars |
| `isGlobal` | Boolean | `true` = system tag, `false` = personal tag |

#### `UserTagMapping`

| Field | Type | Notes |
|-------|------|-------|
| `userId` | UUID | composite PK part 1 |
| `tagId` | UUID | composite PK part 2; FK to `tag.id` |

Personal tags (`isGlobal = false`) are linked to a user via this table.

---

### Endpoints

#### `GET /api/v1/categories` — AUTH REQUIRED

Return all categories the user has access to, with any custom aliases applied.

**Auto-seeding behavior:** On first call for a user with no mappings, the service bulk-inserts `UserCategoryMapping` rows for every global category. This is transparent to the UI — the response is always a full list. Subsequent calls hit the Redis cache (key: userId).

**Response (200):**
```json
{
  "success": true,
  "message": "Success",
  "data": [
    {
      "id": "uuid",
      "parentCategoryId": null,
      "name": "Food & Dining",
      "categoryType": "EXPENSE",
      "customAlias": null,
      "isActive": true
    },
    {
      "id": "uuid",
      "parentCategoryId": "parent-uuid",
      "name": "Restaurants",
      "categoryType": "EXPENSE",
      "customAlias": "Eating Out",
      "isActive": true
    }
  ]
}
```

**UI usage:** Build a 2-level tree using `parentCategoryId`. Root categories have `parentCategoryId = null`. Display `customAlias` if not null, else `name`. Only show `isActive = true` entries.

---

#### `PUT /api/v1/categories/mapping/{categoryId}` — AUTH REQUIRED

Set or update the user's custom alias for a category. Invalidates the Redis cache.

**Request body:**
```json
{
  "customAlias": "Eating Out"
}
```

Pass `"customAlias": null` to remove an alias and revert to the system name.

**Response (200):** Updated `CategoryResponse`.

---

#### `GET /api/v1/tags` — AUTH REQUIRED

Return all tags available to the user: all global tags + user's personal tags. Cached in Redis.

**Response (200):**
```json
{
  "success": true,
  "message": "Success",
  "data": [
    { "id": "uuid", "name": "Online", "isGlobal": true },
    { "id": "uuid", "name": "My Custom Tag", "isGlobal": false }
  ]
}
```

---

#### `POST /api/v1/tags` — AUTH REQUIRED

Create a personal tag (visible only to this user). Invalidates the user's Redis tag cache.

**Request body:**
```json
{
  "name": "Weekend Spend"
}
```

**Business logic:** Tag name must be globally unique (across all users' personal tags and global tags). Returns 400 if name exists.

**Response (201):**
```json
{
  "success": true,
  "message": "Tag created",
  "data": { "id": "uuid", "name": "Weekend Spend", "isGlobal": false }
}
```

---

## 7. Service: Ledger Service (port 8083)

The core financial ledger. Records all income, expenses, and transfers with strict integrity constraints.

Gateway routes: `/api/v1/transactions/**`

---

### Models

#### `TransactionHeader`

| Field | Type | Notes |
|-------|------|-------|
| `id` | UUID | PK |
| `userId` | UUID | owner |
| `sourceAccountId` | UUID | account money leaves from |
| `destAccountId` | UUID | **required only for TRANSFER**, null for INCOME/EXPENSE |
| `transactionType` | `TransactionType` | INCOME / EXPENSE / TRANSFER |
| `totalAmount` | BigDecimal | NUMERIC(15,2), must be > 0 |
| `transactionDate` | Instant | when the transaction occurred |
| `note` | String | optional free text |
| `createdAt` | Instant | system timestamp |

#### `TransactionLineItem`

| Field | Type | Notes |
|-------|------|-------|
| `id` | UUID | PK |
| `transaction` | TransactionHeader | parent (FK with ON DELETE CASCADE) |
| `categoryId` | UUID | **required for INCOME/EXPENSE**, **must be null for TRANSFER** |
| `budgetGoalId` | UUID | optional — if set, routes this line item to a specific budget goal |
| `amount` | BigDecimal | NUMERIC(15,2), must be > 0 |
| `tagIds` | Set\<UUID\> | stored in `transaction_tag_mapping` junction table |

---

### Ledger Invariant (CRITICAL)

**Sum of all `lineItem.amount` values must exactly equal `header.totalAmount`.**

If they don't match, the entire request is rejected with HTTP 422 (`LedgerImbalanceException`). Nothing is persisted. This is always validated before any DB write.

---

### Endpoints

#### `POST /api/v1/transactions` — AUTH REQUIRED

Create a transaction (expense, income, or transfer).

**Request body:**
```json
{
  "sourceAccountId": "uuid",
  "destAccountId": null,
  "transactionType": "EXPENSE",
  "totalAmount": 1500.00,
  "transactionDate": "2026-06-17T10:00:00Z",
  "note": "Lunch at office",
  "lineItems": [
    {
      "categoryId": "uuid",
      "budgetGoalId": "uuid-or-null",
      "amount": 1000.00,
      "tagIds": ["tag-uuid-1"]
    },
    {
      "categoryId": "uuid",
      "budgetGoalId": null,
      "amount": 500.00,
      "tagIds": []
    }
  ]
}
```

**Validation rules (enforced server-side):**

| Rule | Condition |
|------|-----------|
| `totalAmount > 0` | Always |
| `sum(lineItems.amount) == totalAmount` | Always — 422 if violated |
| `destAccountId` required | When `transactionType == TRANSFER` |
| `lineItem.categoryId` required | When `transactionType != TRANSFER` (INCOME or EXPENSE) |
| `lineItem.categoryId` must be null | When `transactionType == TRANSFER` |
| `lineItem.amount > 0` | Always |

**Business logic:**
1. Validate all rules above.
2. Save `TransactionHeader` + all `TransactionLineItem` rows in a single `@Transactional` block.
3. If `EXPENSE`, fire expense event to budget-service (async best-effort, circuit-breaker protected — transaction is already committed before this call, so a budget-service failure does not roll back the ledger).
4. Return the full saved transaction.

**Expense event to budget-service:** For each EXPENSE line item, the budget-service is notified with `{ categoryId, amount, budgetGoalId }`. If `budgetGoalId` is provided on the line item, that goal is debited. Otherwise budget-service looks up the default category-to-goal mapping. If no mapping exists, the line item is silently skipped (logged as a warning).

**Response (201):**
```json
{
  "success": true,
  "message": "Transaction created",
  "data": {
    "id": "uuid",
    "userId": "uuid",
    "sourceAccountId": "uuid",
    "destAccountId": null,
    "transactionType": "EXPENSE",
    "totalAmount": 1500.00,
    "transactionDate": "2026-06-17T10:00:00Z",
    "note": "Lunch at office",
    "createdAt": "2026-06-17T10:01:00Z",
    "lineItems": [
      {
        "id": "uuid",
        "categoryId": "uuid",
        "budgetGoalId": "uuid",
        "amount": 1000.00,
        "tagIds": ["tag-uuid-1"]
      },
      {
        "id": "uuid",
        "categoryId": "uuid",
        "budgetGoalId": null,
        "amount": 500.00,
        "tagIds": []
      }
    ]
  }
}
```

---

#### `GET /api/v1/transactions/{id}` — AUTH REQUIRED

Fetch a single transaction by ID.

**Business logic:** Returns 404 if not found or doesn't belong to authenticated user.

**Response (200):** Full `TransactionResponse` as above.

---

#### `GET /api/v1/transactions` — AUTH REQUIRED

List transactions with optional month/year filter and pagination.

**Query parameters:**

| Param | Type | Required | Default | Notes |
|-------|------|----------|---------|-------|
| `month` | int | no | — | 1–12. Must be used with `year`. |
| `year` | int | no | — | e.g. 2026 |
| `page` | int | no | 0 | Zero-based page index |
| `size` | int | no | 20 | Items per page |

**Example:** `GET /api/v1/transactions?month=6&year=2026&page=0&size=20`

**Business logic:** Results ordered by `transactionDate DESC`. If month+year provided, filters by that calendar window (UTC midnight to midnight). Queries use the indexed `transaction_date` column.

**Response (200):** `ApiResponse<PageResponse<TransactionResponse>>`

---

#### `DELETE /api/v1/transactions/{id}` — AUTH REQUIRED

Delete a transaction and all its line items + tag mappings (ON DELETE CASCADE handles children).

**Business logic:** Returns 404 if not found or doesn't belong to user. Does **not** reverse the budget `spent_amount` (budget rollback is a MVP2 feature).

**Response (204):** No content.

---

## 8. Service: Budget Service (port 8084)

Tracks spending goals and computes monthly budget periods. Receives expense events from ledger-service.

Gateway routes: `/api/v1/budgets/**`

---

### Models

#### `BudgetGoal`

| Field | Type | Notes |
|-------|------|-------|
| `id` | UUID | PK |
| `userId` | UUID | owner |
| `name` | String | max 50 chars, e.g. "Monthly Wants" |
| `targetAmount` | BigDecimal | monthly target, NUMERIC(15,2) |
| `budgetType` | String | NEEDS / WANTS / INVESTMENTS / CUSTOM — controls alert thresholds |
| `isActive` | Boolean | default `true` |

#### `BudgetPeriod`

Auto-created when a goal is created (for the current month). Rolled over monthly by `RolloverService`.

| Field | Type | Notes |
|-------|------|-------|
| `id` | UUID | PK |
| `budgetGoal` | BudgetGoal | FK |
| `periodMonth` | Integer | 1–12 |
| `periodYear` | Integer | e.g. 2026 |
| `startingBalance` | BigDecimal | target ± rollover from previous month |
| `spentAmount` | BigDecimal | incremented on each matching expense event; starts at 0 |

Derived fields (computed in `BudgetPeriodResponse`):
- `remainingAmount = startingBalance - spentAmount`
- `percentageUsed = (spentAmount / startingBalance) * 100`
- `alertLevel` = threshold-based string (see §4)

#### `BudgetGoalCategoryMapping`

Maps categories to goals. Drives automatic budget routing when `budgetGoalId` is not set on a transaction line item.

| Field | Type | Notes |
|-------|------|-------|
| `budgetGoalId` | UUID | composite PK part 1 |
| `categoryId` | UUID | composite PK part 2 (references metadata-service, no DB FK) |
| `isDefault` | Boolean | if `true`, this goal is the fallback for this category |

**Rule:** Only one mapping per category should have `isDefault = true`. The budget-service picks the first default found; duplicates are not rejected by the DB but lead to non-deterministic routing.

---

### Endpoints

#### `POST /api/v1/budgets/goals` — AUTH REQUIRED

Create a budget goal. Also creates the current month's `BudgetPeriod` with `startingBalance = targetAmount`.

**Request body:**
```json
{
  "name": "Monthly Wants",
  "targetAmount": 10000.00,
  "budgetType": "WANTS"
}
```

`budgetType` defaults to `"CUSTOM"` if omitted.

**Response (201):**
```json
{
  "success": true,
  "message": "Budget goal created",
  "data": {
    "id": "uuid",
    "name": "Monthly Wants",
    "targetAmount": 10000.00,
    "budgetType": "WANTS",
    "isActive": true
  }
}
```

---

#### `GET /api/v1/budgets/goals` — AUTH REQUIRED

List all active budget goals for the user.

**Response (200):** `ApiResponse<List<BudgetGoalResponse>>`

---

#### `POST /api/v1/budgets/goals/{goalId}/categories` — AUTH REQUIRED

Map a category to this goal. If `isDefault: true`, expenses with this `categoryId` (and no explicit `budgetGoalId`) will automatically debit this goal.

**Request body:**
```json
{
  "categoryId": "uuid",
  "isDefault": true
}
```

**Business logic:** Returns 404 if `goalId` not found. Does not validate that `categoryId` exists in metadata-service (cross-service FK is intentional — the UI should only send valid category IDs).

**Response (201):**
```json
{
  "success": true,
  "message": "Mapping added",
  "data": {
    "budgetGoalId": "uuid",
    "categoryId": "uuid",
    "isDefault": true
  }
}
```

---

#### `GET /api/v1/budgets/goals/{goalId}/categories` — AUTH REQUIRED

List all category mappings for a goal.

**Response (200):** `ApiResponse<List<CategoryMappingResponse>>`

---

#### `DELETE /api/v1/budgets/goals/{goalId}/categories/{categoryId}` — AUTH REQUIRED

Remove a category mapping from a goal.

**Response (204):** No content.

---

#### `GET /api/v1/budgets/periods/current` — AUTH REQUIRED

Get all budget periods for the current calendar month, across all active goals.

**Response (200):**
```json
{
  "success": true,
  "message": "Success",
  "data": [
    {
      "id": "uuid",
      "budgetGoalId": "uuid",
      "goalName": "Monthly Wants",
      "periodMonth": 6,
      "periodYear": 2026,
      "startingBalance": 10000.00,
      "spentAmount": 3500.00,
      "remainingAmount": 6500.00,
      "percentageUsed": 35.00,
      "alertLevel": "OK"
    }
  ]
}
```

**UI usage:** This is the primary data source for the budget dashboard. Poll or refresh after every transaction creation. Use `alertLevel` to drive visual states:
- `OK` → normal
- `WARNING` → yellow (NEEDS/WANTS at 80%+)
- `MILESTONE` → green/celebratory (INVESTMENTS at 50%+)
- `ALERT` → red (any goal at 100%+)

---

### Budget Rollover (Automatic — no UI action needed)

Runs at **00:01 on the 1st of every month** via Spring `@Scheduled`. Idempotent — won't create a duplicate period if one already exists for the month.

**Logic per active goal:**
```
prev_net = prev_starting_balance - prev_spent_amount

new_starting_balance = target_amount + prev_net
if new_starting_balance < 0:
    new_starting_balance = 0
```

- Surplus month: `new_starting_balance > target_amount` — carries over unspent money
- Deficit month: `new_starting_balance < target_amount` — reduces next month's budget by overspend
- If no previous period exists (new goal), `prev_net = 0`

---

## 9. Service: Scheduler Service (port 8085)

Manages recurring transactions and their automated execution.

Gateway routes: `/api/v1/recurring-transactions/**`

---

### Models

#### `RecurringTransaction`

| Field | Type | Notes |
|-------|------|-------|
| `id` | UUID | PK |
| `userId` | UUID | owner |
| `frequency` | `RecurrenceFrequency` | DAILY / WEEKLY / MONTHLY / YEARLY |
| `nextExecutionDate` | LocalDate | date of next scheduled run |
| `headerTemplate` | `Map<String, Object>` | JSONB — mirrors `CreateTransactionRequest` fields (minus `lineItems`) |
| `lineItemsTemplate` | `List<Map<String, Object>>` | JSONB — list of line item objects |
| `isActive` | Boolean | `false` = soft-deleted / cancelled |

**Template format** — `headerTemplate` should contain the same fields as `CreateTransactionRequest` minus `lineItems` and `transactionDate` (date is injected at execution time):
```json
{
  "sourceAccountId": "uuid",
  "transactionType": "EXPENSE",
  "totalAmount": 999.00,
  "note": "Netflix subscription"
}
```

`lineItemsTemplate` format:
```json
[
  { "categoryId": "uuid", "amount": 999.00, "tagIds": [], "budgetGoalId": "uuid-or-null" }
]
```

#### `RecurringExecutionLog`

| Field | Type | Notes |
|-------|------|-------|
| `id` | UUID | PK |
| `recurringTransaction` | RecurringTransaction | FK |
| `status` | `ExecutionStatus` | SUCCESS / FAILED |
| `errorMessage` | String | null on success; exception message on failure |
| `executedAt` | Instant | auto-set |

---

### Endpoints

#### `POST /api/v1/recurring-transactions` — AUTH REQUIRED

Create a recurring transaction template.

**Request body:**
```json
{
  "frequency": "MONTHLY",
  "nextExecutionDate": "2026-07-01",
  "headerTemplate": {
    "sourceAccountId": "uuid",
    "transactionType": "EXPENSE",
    "totalAmount": 999.00,
    "note": "Netflix"
  },
  "lineItemsTemplate": [
    { "categoryId": "uuid", "amount": 999.00, "tagIds": [], "budgetGoalId": null }
  ]
}
```

**Business logic:** Saves the template. The scheduler cron picks it up on or after `nextExecutionDate`.

**Response (201):**
```json
{
  "success": true,
  "message": "Recurring transaction created",
  "data": {
    "id": "uuid",
    "frequency": "MONTHLY",
    "nextExecutionDate": "2026-07-01",
    "headerTemplate": { ... },
    "lineItemsTemplate": [ ... ],
    "isActive": true
  }
}
```

---

#### `GET /api/v1/recurring-transactions` — AUTH REQUIRED

List all active recurring transaction templates for the user.

**Response (200):** `ApiResponse<List<RecurringTransactionResponse>>`

---

#### `DELETE /api/v1/recurring-transactions/{id}` — AUTH REQUIRED

Cancel (soft-delete) a recurring transaction. Sets `isActive = false`. Does not delete history or previously executed transactions.

**Business logic:** Returns 404 if not found. Returns 400 if the recurring transaction belongs to a different user.

**Response (204):** No content.

---

### Automated Execution (no UI action needed)

Runs at **midnight (00:00:00) every day** via Spring cron, protected by a ShedLock distributed lock (prevents duplicate execution across multiple instances).

**Per due transaction (`nextExecutionDate <= today`):**
1. Build payload from `headerTemplate + lineItemsTemplate`, inject `transactionDate = today`.
2. Call `ledger-service POST /api/v1/transactions` (circuit-breaker protected).
3. On success: write `SUCCESS` to execution log, advance `nextExecutionDate` by the frequency interval.
4. On failure (ledger down, circuit open, etc.): write `FAILED` + error message to log. Does **not** retry — the transaction is skipped until the next scheduled date.

---

## 10. Service: Reporting Service (port 8086)

Read-only reporting layer. Aggregates data by calling ledger-service and user-service. Has **no database** of its own.

Gateway routes: `/api/v1/reports/**`

---

### Endpoints

#### `GET /api/v1/reports/ledger` — AUTH REQUIRED

Proxies the transaction list from ledger-service with the same pagination and filtering.

**Query parameters:** Same as `GET /api/v1/transactions` — `month`, `year`, `page`, `size`.

**Response (200):** Same paginated `TransactionResponse` shape from ledger-service.

**UI usage:** Use this endpoint instead of calling ledger-service directly for the transaction history screen.

---

#### `GET /api/v1/reports/breakdown` — AUTH REQUIRED

Returns spending totals grouped by category for a given month/year. Uses a SQL `GROUP BY` in ledger-service — not in-memory aggregation.

**Query parameters:**

| Param | Type | Required |
|-------|------|----------|
| `month` | int | yes |
| `year` | int | yes |

**Response (200):**
```json
{
  "success": false,
  "message": null,
  "data": [
    { "categoryId": "uuid", "total": 3500.00 },
    { "categoryId": "uuid", "total": 1200.00 }
  ]
}
```

Note: the response is the raw ledger-service internal response (a list of `{ categoryId, total }` objects). The UI must join against the category list (from `GET /api/v1/categories`) by `categoryId` to display names.

---

#### `GET /api/v1/reports/credit-card-bracket` — AUTH REQUIRED

Computes the total outstanding credit card liability (the "Future Expense Bracket" widget).

**Business logic:**
1. Fetch all active accounts from user-service.
2. Filter for `accountType == "CREDIT_CARD" && isActive == true`.
3. Sum their `balance` fields.
4. Return total and count.

**Response (200):**
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "totalCreditCardLiability": 15000.00,
    "accountCount": 2
  }
}
```

**UI usage:** Display as the "Future Expense Bracket" dashboard widget. This reflects total outstanding credit card debt. It decreases when the user logs a TRANSFER from a bank account to a credit card account.

> **Note:** Account balances are **not auto-updated** by the ledger-service in MVP1. The UI is responsible for updating displayed balances or prompting the user to update their account opening balance when needed.

---

## 11. Error Handling

### HTTP Status Codes

| Status | When |
|--------|------|
| `201 Created` | Resource created successfully |
| `204 No Content` | Delete succeeded |
| `400 Bad Request` | Validation failure or business rule violation |
| `401 Unauthorized` | Missing or invalid JWT (from gateway) |
| `404 Not Found` | Resource doesn't exist or doesn't belong to user |
| `422 Unprocessable Entity` | Ledger imbalance — line items don't sum to total_amount |
| `500 Internal Server Error` | Unexpected error |

### Error body shape

```json
{
  "status": 400,
  "error": "Validation Failed",
  "message": "Request validation failed",
  "path": "/api/v1/transactions",
  "fieldErrors": {
    "totalAmount": "must be greater than 0.01",
    "lineItems": "must not be empty"
  }
}
```

`fieldErrors` only present on validation failures (400 from `@Valid` constraints). All other errors omit it.

---

## 12. Cross-Service Flows

### Registration flow

```
POST /api/v1/users/register
  → user-service creates Household + AppUser
  → issues JWT
  → returns AuthResponse (token + user info)

UI next steps:
  → GET /api/v1/categories  (triggers auto-seeding of default category mappings)
  → POST /api/v1/accounts   (create at least one account to transact)
  → POST /api/v1/budgets/goals  (create goals)
  → POST /api/v1/budgets/goals/{id}/categories  (map categories to goals)
```

### Expense creation flow

```
POST /api/v1/transactions  (EXPENSE)
  → ledger-service validates + saves header + line items (atomic)
  → ledger-service calls budget-service POST /api/v1/internal/expense-event
      → budget-service routes each line item to a goal:
          if lineItem.budgetGoalId is set → use that goal
          else look up default category mapping → use that goal
          else → skip (log warning)
      → budget-service increments spentAmount on BudgetPeriod
  → (budget notification is best-effort — ledger commit is NOT rolled back if budget-service is down)

UI next steps:
  → re-fetch GET /api/v1/budgets/periods/current  (to refresh dashboard)
```

### Credit card payment flow

```
POST /api/v1/transactions  (TRANSFER)
  sourceAccountId = bank account UUID
  destAccountId   = credit card account UUID
  transactionType = "TRANSFER"
  lineItems       = [{ amount: total, categoryId: null }]

  Note: categoryId MUST be null for TRANSFER line items.
  Note: budget-service is NOT notified (transfers don't affect budget spending).
  Note: Account balances are not auto-updated in MVP1 — the credit card bracket
        (GET /api/v1/reports/credit-card-bracket) reads the stored balance field,
        which is set at account creation and not updated automatically.
```

### Recurring transaction execution flow (midnight cron)

```
scheduler-service (midnight):
  → find all recurring_transaction where isActive=true AND nextExecutionDate <= today
  → for each:
      build payload from headerTemplate + lineItemsTemplate
      inject transactionDate = today
      call ledger-service POST /api/v1/transactions (with user's X-User-Id)
      on SUCCESS: write log, advance nextExecutionDate
      on FAILURE: write FAILED log, leave nextExecutionDate unchanged
```

### Monthly rollover (1st of month, 00:01)

```
budget-service (00:01 on 1st):
  → for each active BudgetGoal:
      look up previous month's BudgetPeriod
      compute new_starting = targetAmount + (prevStarting - prevSpent)
      floor at 0
      create new BudgetPeriod for current month
      (idempotent — skips if current month period already exists)
```

---

## 13. Domain Rules & Invariants

These are hard constraints enforced server-side. The UI should validate these before submission to give better UX, but the backend will always reject violations.

### Monetary values
- All amounts are `NUMERIC(15,2)` — always 2 decimal places in JSON.
- **Never send floats.** Always send numbers with explicit decimal (e.g., `1500.00` not `1500`).
- Negative amounts are prohibited everywhere. Reversals/refunds are logged as opposite types (e.g., an income to an expense category = refund), never as negative numbers.

### Ledger imbalance (422)
- `sum(lineItems.amount)` must exactly equal `header.totalAmount`.
- If not, the entire request is rejected. Nothing is saved.

### TRANSFER rules
- `destAccountId` is required.
- All line items must have `categoryId = null`.
- Budget-service is NOT notified (transfers don't count as expenses).

### INCOME / EXPENSE rules
- `destAccountId` must be null.
- All line items must have a `categoryId`.

### CREDIT_CARD account
- `billingStartDay` and `billingEndDay` (integers 1–31) are required at creation.
- Other account types leave these null.

### Category hierarchy
- Categories are 2-level max: Root (parentCategoryId = null) → Sub-category (parentCategoryId = root.id).
- The UI must not create deeper nesting. Tags handle deeper granularity.

### Budget default mapping
- Set `isDefault: true` on at most ONE mapping per `categoryId` across all goals.
- Multiple defaults for the same category = non-deterministic budget routing (first match wins).

### Recurring transaction templates
- `lineItemsTemplate` amounts must sum to `headerTemplate.totalAmount` (not validated at template creation time — only validated by ledger-service at execution time; failures are logged).
