- In all interactions and commit messages, be extremely concise and sacrifice grammar for the sake of concision.

# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository status

This repository currently contains **only design/planning documents** (an Obsidian vault under `Expense Tracker/`) for an Expense Tracker backend that has not been implemented yet. There is no source code, build tooling, or test suite yet. When implementation begins, this file should be updated with real build/lint/test commands and a description of the actual code layout.

## Document map

| File | Contents |
| --- | --- |
| `Expense Tracker/Functional Requirement.md` | FRD for MVP1 — the authoritative description of scope, in-scope vs. out-of-scope features |
| `Expense Tracker/Features.md` | Agile epics/user stories with acceptance criteria, organized by feature |
| `Expense Tracker/System Design.md` | High-level architecture + per-feature implementation notes (APIs, service logic, DB usage) |
| `Expense Tracker/Service Level LLD.md` | Low-level design per backend service (responsibilities, validation rules) |
| `Expense Tracker/Service level business logic.md` | Step-by-step business logic per service — most precise spec for implementing service methods |
| `Expense Tracker/API Stub.md` | Concrete REST endpoint contracts (request/response JSON examples) |
| `Expense Tracker/ER Data.md` | Full PostgreSQL schema (tables, columns, types, constraints, indexes) |
| `Expense Tracker/Design flowchart.md` | Decision-tree walkthroughs of the onboarding, transaction, rollover, and scheduler flows |
| `Expense Tracker/Chat with AI.md` | Original brainstorming transcript — useful for *why* decisions were made, but superseded by the docs above where they conflict |

When implementing a feature, prefer **`ER Data.md`** for schema, **`Service level business logic.md`** for exact validation/processing steps, and **`API Stub.md`** for endpoint shapes. The other docs are higher-level restatements of the same design.

## Target architecture (per System Design.md)

- **Clients:** React (web) and React Native (mobile), talking to the backend over REST/HTTPS.
- **Gateway:** Spring Cloud Gateway (routing, CORS, rate limiting).
- **Backend:** Spring Boot, organized into services/modules: User & Account Service, Metadata Service, Ledger Service, Budget Service, Scheduler Service, Reporting Service.
- **Database:** PostgreSQL, strict ACID, all monetary values as `NUMERIC(15, 2)` (no floats, ever).

## Core domain model — non-obvious rules

These rules come from the FRD/LLD/business-logic docs and are easy to get wrong; they should be enforced in code wherever applicable:

- **Strict ledger invariant:** A `transaction_header` (total amount, source/dest account, type) must always have `transaction_line_item` rows whose `amount` sums exactly to `total_amount`. If not, throw `LedgerImbalanceException` and roll back the whole `@Transactional` operation — never partially persist.
- **Transaction types:** `INCOME`, `EXPENSE`, `TRANSFER`.
  - `TRANSFER` requires `dest_account_id IS NOT NULL` and **every** line item's `category_id IS NULL` (prevents double-counting against budgets).
  - `INCOME`/`EXPENSE` line items must have a `category_id`.
  - Negative amounts are prohibited (`CHECK (total_amount > 0)`, `CHECK (amount > 0)`); reversals/refunds are logged as the opposite transaction type, not negative entries.
- **Accounts:** `account_type` ∈ `BANK | CREDIT_CARD | CASH | LOAN`. Only `CREDIT_CARD` accounts populate `billing_start_day`/`billing_end_day` (integers 1–31); these stay `NULL` otherwise.
- **Future Expense Bracket** (credit card liability widget): derived, not stored — it's a read-query summing `balance` across the user's `CREDIT_CARD` accounts. Paying a credit card bill is a `TRANSFER` from a bank account to the credit card account; if the transfer amount exceeds the bracket (e.g. late fees), prompt the user to log the excess as a separate `EXPENSE`.
- **Categories/tags are a global, immutable dictionary** (`is_global = TRUE`). Per-user customization happens only via mapping tables:
  - `user_category_mapping` (user_id + category_id PK) holds `custom_alias` — never mutate the global `category` row.
  - `user_tag_mapping` links users to global tags; users can also create personal tags (`is_global = FALSE`).
  - On registration, default category mappings are bulk-inserted asynchronously (event-driven).
- **Category hierarchy:** stored as an adjacency list (`parent_category_id`) but the **UI is capped at 2 levels** (Root → Sub-category). Deeper granularity belongs in tags, not categories.
- **Cascading deletes:** `transaction_header` deletion relies on `ON DELETE CASCADE` to wipe `transaction_line_item` and `transaction_tag_mapping` — service code should not manually delete children.
- **Zero-Based Budget Rollover** (runs 1st of month, 00:01, via scheduled job):
  - For each `budget_goal`, compare previous month's `spent_amount` vs `starting_balance`.
  - **Deficit** (overspent): subtract the deficit from the new month's `target_amount` to compute `starting_balance`, and log the deficit as a system-generated `EXPENSE` under "Expense from last month".
  - **Surplus**: carry forward as a "Rollover Balance" — must NOT be reported as regular `INCOME`.
- **Budget sync:** the Ledger Service publishes an internal event on every successful `EXPENSE`; the Budget Service listens and updates `spent_amount` on the current `budget_period`. Threshold UI cues: 80% of a "Wants/Needs"-style bracket → warning, 100% → alert; 50%/100% of an "Investments"-style bracket → encouragement/reward.
- **Recurring transactions:** stored as JSONB templates (`header_template`, `line_items_template`) on `recurring_transaction`, processed by a midnight `@Scheduled(cron = "0 0 0 * * ?")` job guarded by a distributed lock (ShedLock/Quartz) to avoid duplicate execution across instances. Every run writes a `SUCCESS`/`FAILED` row (with error message) to `recurring_execution_log` and advances `next_execution_date`.
- **Reporting:** ledger queries must be paginated (`Pageable`) and hit the indexed `transaction_date` column; category breakdowns must use SQL `GROUP BY`/projections, not in-memory JVM aggregation. `budget_period` is indexed on `(period_month, period_year)`.
- **Household model:** every `app_user` belongs to a `household` (created alongside the user at registration) — this is future-proofing for MVP2 multi-user/family sharing, even though MVP1 is single-user.

## MVP1 explicit non-goals

Do not build these unless requirements change: complex loan/EMI principal-vs-interest splits, CSV export/import, multi-currency support with conversion rates, multi-user/family sharing UI (the `household` FK exists for forward-compatibility only).
