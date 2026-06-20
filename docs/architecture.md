# Expense Tracker — Architecture

## System overview

```
Browser (React / Vite)
        │
        │  HTTP (dev: Vite proxy  /api → :8080)
        │  HTTP (prod: Nginx      /api → app:8080 , / → static)
        ▼
┌──────────────────────────────┐
│   Spring Boot Monolith :8080 │
│                              │
│  JwtAuthFilter               │  ← validates JWT, injects X-User-Id header
│  SecurityConfig              │  ← permits /auth/**, authenticates all else
│  CorsConfig                  │  ← allowed-origins from env
│                              │
│  ┌──────────┐  ┌──────────┐  │
│  │  user    │  │ metadata │  │
│  ├──────────┤  ├──────────┤  │
│  │  ledger  │  │  budget  │  │
│  ├──────────┤  ├──────────┤  │
│  │scheduler │  │reporting │  │
│  └──────────┘  └──────────┘  │
└──────────────┬───────────────┘
               │
       ┌───────┴────────┐
       │                │
  PostgreSQL 16     Redis 7
  (5 schemas,       (category
   Flyway)           cache)
```

In production a GCP e2-micro VM runs all four containers via Docker Compose: `postgres`, `redis`, `app`, `nginx`. The React build is served by Nginx at `/`; all `/api/` calls are proxied to the Spring Boot container.

---

## Frontend

**Stack:** React 18, Vite, React Router v7, React Query, Zustand, Axios  
**Repo:** `/Users/abhishekbhavsar/Work/Frontend/ExpenseTracker`  
**Monorepo tool:** pnpm workspaces

```
apps/
  web/             — Vite app (port 3000 in dev)
    src/
      pages/app/   — Dashboard, Transactions, Budget, GoalDetail, Reports
      pages/auth/  — Login, Register
      pages/settings/ — Accounts, Categories, Tags, Recurring
      components/layout/ — AppShell (sidebar + outlet)
    vite.config.ts — proxy: /api → http://localhost:8080

packages/
  api/    — axios client + all endpoint functions (auth, accounts, ledger …)
  store/  — Zustand stores (auth: token + isAuthenticated)
  utils/  — shared helpers
```

### Auth / token flow

1. `initApiClient` called at app startup; gives the client a `getToken` callback pointing at the Zustand auth store.
2. Request interceptor reads the current JWT and attaches `Authorization: Bearer <token>`.
3. Response interceptor catches `401` → calls `onUnauthorized` → clears store, redirects to `/login`.
4. `ProtectedRoute` / `PublicRoute` wrappers in `App.tsx` guard navigation.

### API client base URL

In development the base URL is `''` (empty string). Vite's dev server proxy forwards every `/api/…` request to `http://localhost:8080`, eliminating CORS issues. In production `VITE_API_URL` is set in `apps/web/.env.production` to the VM's public IP/domain.

---

## Backend

**Stack:** Spring Boot 3.5.3, Java 21, Spring Security 6, Spring Data JPA (Hibernate 6), Flyway, Caffeine/Redis cache, ShedLock  
**Single deployable:** `app/` module, depends on `common/`.

```
app/src/main/java/com/expensetracker/
  config/       — CorsConfig, SecurityConfig, JwtAuthFilter, RedisConfig …
  common/       — (re-exported from common module: ApiResponse, PageResponse, enums, exceptions)
  user/         — household, app_user, account
  metadata/     — category, tag, user mappings
  ledger/       — transaction_header, transaction_line_item
  budget/       — budget_goal, budget_period, rollover
  scheduler/    — recurring_transaction, execution_log
  reporting/    — read-only queries across ledger + budget
```

Every domain package follows: `entity/ → repository/ → service/ → controller/ → dto/ → exception/`.

### Dependency flow

The monolith's key advantage over the previous microservice layout: service classes can call other service classes directly inside a single `@Transactional` boundary.

```
TransactionService.create()
  ├── headerRepo.save()           ← ledger write
  ├── accountService.adjustBalance() ← user domain — same tx
  └── budgetPeriodService.handleExpenseEvent() ← budget domain — same tx
```

If any step throws, the entire operation rolls back — no partial state, no compensating sagas needed.

---

## Security

### Request lifecycle

```
HTTP request
  → JwtAuthFilter (OncePerRequestFilter)
      → extract Bearer token from Authorization header
      → JwtService.parse() → userId, householdId
      → set SecurityContextHolder (UsernamePasswordAuthenticationToken)
      → wrap request as UserIdInjectedRequest (adds X-User-Id header)
  → SecurityConfig
      → /api/v1/auth/** → permitAll
      → everything else → authenticated()
  → Controller
      → reads X-User-Id header (already injected by filter)
```

### JWT

Issued at login/register. Payload: `sub=userId`, `householdId`, `exp` (24 h by default, configurable via `app.jwt.expiry-seconds`). Secret from `app.jwt.secret` (base64 env var). No refresh token in MVP1.

### Passwords

BCrypt hashed at registration, compared with `PasswordEncoder.matches()` at login.

---

## Domain modules

### user

| Entity | Table | Notes |
|--------|-------|-------|
| Household | `household` | created with each new user; MVP2 hook for family sharing |
| AppUser | `app_user` | `household_id FK`, bcrypt hash, email unique |
| Account | `account` | types: BANK, CREDIT_CARD, CASH, LOAN; NUMERIC(15,2) balance |

`AccountService.adjustBalance()` is called by `TransactionService` in the same transaction:
- `INCOME` → credit `source_account`
- `EXPENSE` → debit `source_account`
- `TRANSFER` → debit `source`, credit `dest`

Deletion reverses these operations (swaps EXPENSE↔INCOME, swaps source/dest for TRANSFER).

### metadata

| Entity | Table | Notes |
|--------|-------|-------|
| Category | `category` | global immutable dictionary, adjacency list hierarchy (max 2 levels in UI) |
| Tag | `tag` | global + per-user (`is_global=false`) |
| UserCategoryMapping | `user_category_mapping` | `custom_alias` per user; default mappings seeded on first access |
| UserTagMapping | `user_tag_mapping` | links users to tags |

`CategoryService.getUserCategories()` is annotated `@Cacheable("user-categories")`. Cache is backed by Redis and evicted on create/update. First call seeds default mappings for new users from the global category dictionary.

### ledger

| Entity | Table | Notes |
|--------|-------|-------|
| TransactionHeader | `transaction_header` | type, total_amount, source/dest account, date, note |
| TransactionLineItem | `transaction_line_item` | amount, category_id, budget_goal_id, tag_ids (ElementCollection) |

**Ledger invariant:** sum of all `line_item.amount` must equal `transaction_header.total_amount`. Enforced in `TransactionService.validate()` — mismatch throws `LedgerImbalanceException` before any write.

**Transaction types:**
- `INCOME` / `EXPENSE` — every line item must have `category_id`
- `TRANSFER` — `dest_account_id` required, line items must have `category_id = null` (prevents double-counting in budget)

Cascading deletes: `@OneToMany(cascade = CascadeType.ALL)` means deleting the header removes all line items without explicit child deletes in code.

### budget

| Entity | Table | Notes |
|--------|-------|-------|
| BudgetGoal | `budget_goal` | user's spending goal for a category bracket |
| BudgetPeriod | `budget_period` | per-month instance: `starting_balance`, `spent_amount`; indexed on `(period_month, period_year)` |
| BudgetGoalCategoryMapping | `budget_goal_category_mapping` | which categories count against this goal |

`BudgetPeriodService.handleExpenseEvent()` is called synchronously by `TransactionService` (same DB transaction). It finds the current period for each line item's `budget_goal_id` and increments `spent_amount`.

**Rollover** (runs `0 1 0 1 * ?` — 00:01 on the 1st): `RolloverService.rolloverGoal()` computes `net_balance = starting_balance − spent_amount` for the previous month and sets the new period's `starting_balance = target_amount + net_balance` (floored at 0). Each goal rolls over independently; a failure on one goal is caught and logged so others still roll.

### scheduler

| Entity | Table |
|--------|-------|
| RecurringTransaction | `recurring_transaction` — JSONB `header_template` + `line_items_template` |
| RecurringExecutionLog | `recurring_execution_log` — SUCCESS / FAILED + error message |

`RecurringExecutionService.executeRecurringTransactions()` runs at midnight (`0 0 0 * * ?`) guarded by ShedLock (`lockAtMostFor = PT10M`, `lockAtLeastFor = PT1M`). The outer method has no `@Transactional` — each iteration calls `transactionService.create()` which owns its own transaction. This isolates failures: one bad template doesn't roll back the others.

### reporting

No owned tables. Read-only service that queries across ledger and budget:
- `getLedgerReport()` — paginated `transaction_header` by month/year, ordered by `transaction_date DESC`
- `getCategoryBreakdown()` — SQL `GROUP BY category_id` via `lineItemRepo.findBreakdown()` projection; no JVM aggregation
- `getBudgetSummary()` — active goals + current period spend

---

## Database

**Engine:** PostgreSQL 16  
**All monetary columns:** `NUMERIC(15, 2)` — no floating point anywhere.

Flyway manages schema creation. Five migration locations, one per domain:

| Location | Schema prefix |
|----------|--------------|
| `db/migration/user_service` | `V1__` |
| `db/migration/metadata_service` | `V1__` |
| `db/migration/ledger_service` | `V1__` |
| `db/migration/budget_service` | `V1__` |
| `db/migration/scheduler_service` | `V1__` |

All migrations live in `app/src/main/resources/`. `ddl-auto: validate` — Hibernate validates schema against entities at startup but never modifies it.

---

## Caching

Redis 7 is the cache store (`spring.cache.type: redis`).

- `@Cacheable("user-categories")` on `CategoryService.getUserCategories(userId)` — cached per user UUID
- `@CacheEvict` on create/update category mapping endpoints
- In production the Redis container runs with `--maxmemory 64mb --maxmemory-policy allkeys-lru`

---

## Production deployment

```
GCP e2-micro (us-central1, 1 vCPU, 1 GB RAM — free forever)
  └── Docker Compose (docker-compose.prod.yml)
        ├── postgres:16-alpine   — named volume, healthcheck
        ├── redis:7-alpine       — maxmemory 64mb
        ├── app                  — built from Dockerfile, JAVA_OPTS=-Xmx256m -Xms128m
        └── nginx:alpine         — ports 80+443, serves React build + proxies /api/
```

### Dockerfile (multi-stage)

```
Stage 1 — maven:3.9-eclipse-temurin-21-alpine
  mvn dependency:go-offline (layer-cached)
  mvn -pl common,app -DskipTests clean package

Stage 2 — eclipse-temurin:21-jre-alpine
  COPY app.jar
  ENTRYPOINT sh -c "java $JAVA_OPTS -jar app.jar"
```

JVM is tuned for the 1 GB VM: `-Xmx256m -Xms128m -XX:+UseContainerSupport`.

### Nginx routing

```
/api/      → proxy_pass http://app:8080
/actuator/ → proxy_pass http://app:8080
/          → root /usr/share/nginx/html; try_files $uri $uri/ /index.html
```

React's static build is volume-mounted (or baked into the Nginx image). All unknown paths fall back to `index.html` so React Router handles client-side navigation.

### Environment variables (`.env` on the VM)

```
DB_HOST=postgres
DB_NAME=expense_tracker
DB_USER=expense_user
DB_PASSWORD=<secret>
REDIS_HOST=redis
JWT_SECRET=<base64-secret>
ALLOWED_ORIGINS=http://<VM-IP>
```

CORS is controlled via `CorsConfig` which reads `${ALLOWED_ORIGINS}`. In development the default is `http://localhost:3000`.

---

## Key invariants (enforce everywhere)

1. **Ledger balance:** `SUM(line_item.amount) == transaction_header.total_amount` — `LedgerImbalanceException` + rollback on violation.
2. **TRANSFER:** `dest_account_id NOT NULL`, all line items `category_id = null`.
3. **INCOME/EXPENSE:** all line items `category_id NOT NULL`.
4. **No negative amounts:** `total_amount > 0` and `line_item.amount > 0` enforced at DB and service layer. Reversals use the opposite type.
5. **Account balance + ledger write + budget update** are always in one `@Transactional` — never call them from separate transactions.
6. **Batch jobs (recurring, rollover)** are NOT wrapped in an outer transaction — each item owns its own independent transaction.
