# Expense Tracker MVP1 — Implementation Roadmap

## Context
Scaffold complete and verified (`mvn -DskipTests clean install` passes, all 10 modules build).
All service skeletons exist with empty `controller/`, `service/`, `repository/`, `entity/` packages and Flyway migrations applied.
Next work: fill in business logic service by service in dependency order.

## Dependency order
Every service stores `user_id` / `account_id` as plain UUIDs — those IDs come from user-service first.
ledger-service validates categories from metadata-service at the application layer.
budget-service listens to ledger events. scheduler-service delegates back to ledger-service.
reporting-service reads from all of the above.

---

## Step 1 — user-service ← START HERE

**Endpoints** (per `API Stub.md`):
- `POST /api/v1/users/register` → create `household` then `app_user` in one `@Transactional` block; default `base_currency = INR`
- `POST /api/v1/accounts` → validate `account_type` enum; if `CREDIT_CARD`, validate `billing_start_day`/`billing_end_day` (1–31); persist with `balance = opening_balance`
- `GET /api/v1/accounts` → list user's accounts
- `PATCH /api/v1/accounts/{id}` → update name / is_active

**Files to create** under `user-service/src/main/java/com/expensetracker/userservice/`:
- `entity/` — `Household`, `AppUser`, `Account` (extend `AuditableEntity`; use `AccountType` from `common`)
- `repository/` — `HouseholdRepository`, `AppUserRepository`, `AccountRepository`
- `dto/` — `RegisterRequest`, `UserResponse`, `CreateAccountRequest`, `AccountResponse`
- `service/` — `UserService`, `AccountService` (all `@Transactional`)
- `controller/` — `UserController`, `AccountController`

**Business rules:**
- `@Transactional` spans household + user creation atomically
- `CREDIT_CARD` → reject if billing days null or outside 1–31
- All monetary fields mapped as `BigDecimal` → `NUMERIC(15,2)`

---

## Step 2 — metadata-service

**Endpoints:**
- `GET /api/v1/categories` → user's mapped categories (alias-aware); cached in Redis (`@Cacheable`)
- `PUT /api/v1/categories/mapping/{id}` → update `custom_alias` only; never mutate global `category` row
- `GET /api/v1/tags` → global + user's personal tags
- `POST /api/v1/tags` → create personal tag (`is_global = FALSE`)

**Key work:**
- Flyway `V2__seed_global_categories.sql` — seed immutable global categories and tags
- On user registration: bulk-insert `user_category_mapping` rows (called by user-service via `WebClient`)
- `@Cacheable` on category/tag reads; `@CacheEvict` on alias update

---

## Step 3 — ledger-service

**Endpoints:**
- `POST /api/v1/transactions` — the critical path
- `DELETE /api/v1/transactions/{id}`
- `GET /api/v1/transactions/{id}`

**Business rules (highest complexity):**
- Entire save (header + line items + tag mappings) in one `@Transactional` block
- Before `save()`: sum all `line_item.amount`; if ≠ `header.total_amount` → throw `LedgerImbalanceException` (from `common`)
- `TRANSFER`: `dest_account_id` non-null; all line items `category_id = null`
- `INCOME`/`EXPENSE`: all line items must have non-null `category_id`
- On successful `EXPENSE`: call budget-service via `WebClient` to update `spent_amount`
- Deletion: target header only; DB `ON DELETE CASCADE` handles children

---

## Step 4 — budget-service

**Endpoints:**
- `POST /api/v1/budgets/goals` → create `budget_goal` + first `budget_period` for current month
- `GET /api/v1/budgets/periods/current` → active periods with `starting_balance` / `spent_amount`
- Internal `PATCH /internal/budgets/periods/current/spend` → called by ledger-service on EXPENSE

**Key work:**
- Zero-based rollover: `@Scheduled(cron = "1 0 1 * * ?")` — runs 00:01 on 1st of month
  - Deficit → subtract from new month `starting_balance`; log system expense under "Expense from last month"
  - Surplus → tag as "Rollover Balance" (not raw income)

---

## Step 5 — scheduler-service

**Endpoints:**
- `POST /api/v1/recurring-transactions` → save JSONB templates
- `GET /api/v1/recurring-transactions` → list user's setups

**Key work:**
- Midnight cron `@Scheduled(cron = "0 0 0 * * ?")` guarded by `@SchedulerLock` (ShedLock config already wired in `ShedLockConfig.java`)
- Query `recurring_transaction` where `next_execution_date <= CURRENT_DATE` and `is_active = true`
- Deserialize JSONB → call `POST /api/v1/transactions` on ledger-service via `WebClient`
- Write `SUCCESS`/`FAILED` + stack trace to `recurring_execution_log`
- Advance `next_execution_date` by frequency interval

---

## Step 6 — reporting-service

**Endpoints** (per `API Stub.md`):
- `GET /api/v1/reports/ledger?month=06&year=2026&page=0&size=20`
- `GET /api/v1/reports/breakdown?month=06&year=2026`
- Future Expense Bracket → sum of CREDIT_CARD `account.balance` from user-service

**Key work:**
- `WebClient` beans pointing at user-service, ledger-service, budget-service via Eureka `lb://`
- No JPA/Flyway; responses assembled purely from inter-service REST calls

---

## Verification per step

| Step | How to verify |
|------|--------------|
| 1 | `POST /register` → rows in `user_service.household` + `app_user`; `POST /accounts` with CREDIT_CARD missing billing day → 400 |
| 2 | `GET /categories` returns aliased names; hit twice → second served from Redis (check logs) |
| 3 | Split expense with wrong line item sum → 422; TRANSFER with category_id → 400 |
| 4 | Create goal; post expenses; GET periods shows correct `spent_amount`; advance date to 1st → rollover fires |
| 5 | Insert recurring with `next_execution_date = today`; trigger cron manually; verify SUCCESS row in log |
| 6 | GET /reports/ledger returns paginated transactions; GET /breakdown returns category totals |
