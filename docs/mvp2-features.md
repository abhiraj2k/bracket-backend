# MVP2 Features

Features deferred from MVP1, plus new capabilities enabled by the MVP1 foundation.

---

## 1. Default Category Mapping Seeding on Registration

**Deferred from MVP1.** Currently users must manually call `POST /api/v1/budgets/goals/{id}/categories` to wire categories to goals.

**Goal:** On registration, auto-insert default `user_category_mapping` rows (28 global categories) and a default `budget_goal_category_mapping` set so the app is usable out of the box.

**Implementation notes:**
- user-service publishes a `UserRegisteredEvent` (e.g. via an internal REST call or a lightweight event)
- metadata-service listens and bulk-inserts `user_category_mapping` rows for all `is_global = TRUE` categories
- budget-service seeds a default mapping from global root categories → a "General" budget goal created for the user automatically
- No blocking on the registration response — fire-and-forget, async

---

## 2. Multi-User / Family Profile Sharing

**Explicitly deferred from MVP1.** The `household` table and `household_id` FK on `app_user` already exist — this is MVP2's primary unlock.

**Goal:** Multiple users share one household, see a unified ledger, and maintain per-user or shared budget goals.

**Stories:**
- Invite a family member by email → creates `app_user` linked to the same `household_id`
- Role model: `OWNER` vs `MEMBER` (owner can invite/remove, both can transact)
- Ledger queries scoped to `household_id` rather than `user_id` where applicable
- Budget goals can be marked `scope = HOUSEHOLD | PERSONAL`
- Shared dashboard aggregating all members' spending

**Backend changes:**
- `app_user` gets a `role` column (`OWNER | MEMBER`)
- Add `POST /api/v1/households/invite` in user-service
- Gateway propagates `X-Household-Id` header alongside `X-User-Id`
- Ledger + budget queries need household-scoped variants

---

## 3. Loan / EMI Principal vs. Interest Splits

**Explicitly deferred from MVP1.**

**Goal:** When logging an EMI payment, let the user split it into principal repayment and interest expense so net-worth tracking stays accurate.

**Stories:**
- Add `LOAN_REPAYMENT` as a sub-type of `EXPENSE`
- Line item has optional `principal_amount` and `interest_amount` fields; their sum must equal the line item `amount`
- Principal portion reduces the `LOAN` account balance (increases net worth); interest is logged as an `EXPENSE` against a "Loan Interest" category
- Dashboard widget: loan amortization progress (% of principal paid)

**Backend changes:**
- `transaction_line_item` gets `principal_amount NUMERIC(15,2)` and `interest_amount NUMERIC(15,2)` columns (both nullable)
- Validation: if either is non-null, both must be set and must sum to `amount`
- New Flyway migration in ledger-service (V3)
- Reporting-service: new `/api/v1/reports/net-worth` endpoint

---

## 4. CSV Export / Import

**Explicitly deferred from MVP1.**

**Goal:** Let users export their full ledger history and import historical data from bank statements.

**Stories:**
- `GET /api/v1/reports/export?format=csv&from=&to=` — streams a CSV of all transactions in the date range
- `POST /api/v1/transactions/import` — accepts a CSV, validates and bulk-inserts transactions; returns a summary report (success count, skipped rows with reasons)
- Import deduplication: reject rows whose `(date, amount, account, description)` already exist

**Backend changes:**
- Add Apache Commons CSV or OpenCSV to reporting-service and ledger-service
- Import endpoint needs a staging table or in-memory validation pass before committing
- Rate-limit the import endpoint (large files can stress the DB)

---

## 5. Multi-Currency Support

**Explicitly deferred from MVP1.**

**Goal:** Users transacting in multiple currencies (travel, foreign income) can log amounts in a foreign currency and see everything normalized to their `base_currency`.

**Stories:**
- Each transaction optionally carries `original_currency` and `original_amount`; `total_amount` always stored in `base_currency`
- Live FX rates fetched daily from an external provider (e.g. Open Exchange Rates) and cached in Redis
- Dashboard toggle: view spending in base currency or original currency
- Historical rate stored on the transaction at time of entry (immutable)

**Backend changes:**
- New `fx_rate` microservice (or extend metadata-service) that fetches + caches daily rates
- `transaction_header` gets `original_currency VARCHAR(3)`, `original_amount NUMERIC(15,2)`, `fx_rate NUMERIC(10,6)`
- New Flyway migration in ledger-service (V3 or V4 depending on loan migration)
- Circuit breaker on FX rate fetch; fall back to last known rate

---

## 6. Transaction Duplication via UI

**Mentioned in FRD §7 but not implemented in MVP1.**

**Goal:** User taps "Duplicate" on a past transaction → pre-fills the entry form with the same header + line items, with today's date.

**Backend changes:**
- `POST /api/v1/transactions/{id}/duplicate` in ledger-service
- Copies header + all line items, sets `transaction_date = now()`, returns the new draft for client-side confirmation before commit
- No new DB columns needed

---

## 7. Enhanced Reporting

**Goal:** Deeper analytics beyond the MVP1 category breakdown.

**Stories:**
- **Trend report:** month-over-month spending per category (`GET /api/v1/reports/trends?months=6`)
- **Net-worth snapshot:** sum of all `BANK + CASH` balances minus `CREDIT_CARD + LOAN` balances at a point in time
- **Budget goal history:** graph of `spent_amount` vs `starting_balance` across past periods per goal
- **Recurring transaction forecast:** next 30-day projection of scheduled expenses

**Backend changes:**
- All queries added to reporting-service (no new tables; reads from ledger + budget schemas)
- Paginated where result sets can be large

---

## 8. Push Notifications

**Goal:** Proactively alert the user without requiring them to open the app.

**Stories:**
- Budget threshold alerts (80% / 100%) pushed to device, not just surfaced in UI
- Recurring transaction success/failure notifications
- Monthly rollover summary notification on the 1st

**Backend changes:**
- New `notification-service` (port 8087) integrating Firebase Cloud Messaging
- budget-service and scheduler-service publish events to a notification queue (or direct REST call)
- `device_token` stored on `app_user`

---

## Summary

| # | Feature | Effort | Unlocked by |
|---|---------|--------|-------------|
| 1 | Default category seeding on registration | S | MVP1 `household` + category tables |
| 2 | Multi-user family sharing | L | `household_id` FK already in place |
| 3 | Loan / EMI principal-interest splits | M | Ledger line item extensibility |
| 4 | CSV export / import | M | Reporting + ledger services |
| 5 | Multi-currency support | L | FX service + ledger schema extension |
| 6 | Transaction duplication | S | Existing ledger endpoint pattern |
| 7 | Enhanced reporting | M | Existing reporting-service |
| 8 | Push notifications | M | New notification-service |
