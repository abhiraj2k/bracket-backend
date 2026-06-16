# Expense Tracker MVP1

Personal finance ledger with strict double-entry-style ledger, zero-based budgeting, and credit card liability tracking.

## Stack
- Java 21, Spring Boot 3.5.3, Spring Cloud 2025.0.0
- PostgreSQL 16 (ACID, per-service schemas), Redis 7 (metadata cache)
- Maven multi-module

## Modules & Ports

| Module              | Port | Role                              |
|---------------------|------|-----------------------------------|
| discovery-service   | 8761 | Eureka server                     |
| config-service      | 8888 | Spring Cloud Config (native)      |
| gateway-service     | 8080 | Spring Cloud Gateway + CORS       |
| user-service        | 8081 | Identity, households, accounts    |
| metadata-service    | 8082 | Categories, tags (Redis cached)   |
| ledger-service      | 8083 | Strict transaction ledger         |
| budget-service      | 8084 | Budget goals + monthly rollover   |
| scheduler-service   | 8085 | Recurring transactions (ShedLock) |
| reporting-service   | 8086 | Read-only dashboards & reports    |

## Local Development

**1. Start infrastructure:**
```bash
docker-compose up -d
```

**2. Build all modules:**
```bash
mvn -DskipTests clean install
```

**3. Start services (in order):**
```bash
# Terminal 1 — Eureka
mvn -pl discovery-service spring-boot:run

# Terminal 2 — Config Server
mvn -pl config-service spring-boot:run

# Terminal 3-N — remaining services (any order after config is up)
mvn -pl user-service spring-boot:run
mvn -pl metadata-service spring-boot:run
mvn -pl ledger-service spring-boot:run
mvn -pl budget-service spring-boot:run
mvn -pl scheduler-service spring-boot:run
mvn -pl reporting-service spring-boot:run

# Terminal last — Gateway
mvn -pl gateway-service spring-boot:run
```

**Eureka dashboard:** http://localhost:8761  
**API entry point:** http://localhost:8080/api/v1/...

## Key Design Notes

- All monetary values stored as `NUMERIC(15, 2)` — no floats.
- Each domain service owns its own PostgreSQL schema; Flyway manages migrations.
- Cross-service references (e.g. `category_id` in ledger-service) are plain `UUID` columns — no DB-level FK across schemas. Integrity enforced at application layer.
- Config Server provides datasource/Redis config; local `application.yml` only contains bootstrap config (port, app name, config-server URL).
- Recurring transaction scheduler uses ShedLock to prevent duplicate execution across instances.

## Design Docs

All requirements, ER diagrams, LLD, and API specs live in `Expense Tracker/*.md`.
