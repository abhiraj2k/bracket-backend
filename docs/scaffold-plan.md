# Expense Tracker MVP1 — Spring Cloud Microservices Scaffold

## Phases

- [x] Phase 0 — Root project & infra
- [x] Phase 1 — `common` module
- [x] Phase 2 — `discovery-service` + `config-service`
- [x] Phase 3 — `gateway-service`
- [x] Phase 4 — `user-service`
- [x] Phase 5 — `metadata-service`
- [x] Phase 6 — `ledger-service`
- [x] Phase 7 — `budget-service`
- [x] Phase 8 — `scheduler-service`
- [x] Phase 9 — `reporting-service`
- [ ] Phase 10 — Full build verification (`mvn clean install`) — requires Java 21 + Maven

## Conventions

- Group ID: `com.expensetracker`
- Spring Boot 3.5.3, Spring Cloud 2025.0.0, ShedLock 6.6.0, Java 21, Maven
- One schema per domain service in a shared `expense_tracker` Postgres DB
- Cross-schema FK replaced by plain UUID (integrity at application layer)
- Config Server native profile → `config-repo/` directory
- Ports: discovery 8761, config 8888, gateway 8080, user 8081, metadata 8082, ledger 8083, budget 8084, scheduler 8085, reporting 8086

## Modules

| Module             | Owns Tables                                                       |
|--------------------|-------------------------------------------------------------------|
| common             | (library: enums, exceptions, DTOs)                                |
| discovery-service  | —                                                                 |
| config-service     | —                                                                 |
| gateway-service    | —                                                                 |
| user-service       | household, app_user, account                                      |
| metadata-service   | category, user_category_mapping, tag, user_tag_mapping            |
| ledger-service     | transaction_header, transaction_line_item, transaction_tag_mapping|
| budget-service     | budget_goal, budget_period                                        |
| scheduler-service  | recurring_transaction, recurring_execution_log, shedlock          |
| reporting-service  | none (read-only)                                                  |

## Cross-service FK note

ER Data.md defines FKs across all tables (single-schema assumption). In this microservices layout, cross-service references are stored as plain UUID with no DB-level FK constraint. Example: `ledger_service.transaction_line_item.category_id` references `metadata_service.category(id)` but without a FOREIGN KEY clause.
