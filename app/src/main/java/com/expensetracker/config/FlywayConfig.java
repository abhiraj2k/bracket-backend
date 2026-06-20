package com.expensetracker.config;

import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * One Flyway instance per DB schema. Each instance tracks its own flyway_schema_history
 * inside the target schema, so existing and fresh installs both work without baseline tricks.
 */
@Configuration
public class FlywayConfig {

    @Bean(initMethod = "migrate")
    public Flyway userServiceFlyway(DataSource ds) {
        return Flyway.configure()
                .dataSource(ds)
                .schemas("user_service")
                .createSchemas(true)
                .locations("classpath:db/migration/user")
                .load();
    }

    @Bean(initMethod = "migrate")
    public Flyway metadataServiceFlyway(DataSource ds) {
        return Flyway.configure()
                .dataSource(ds)
                .schemas("metadata_service")
                .createSchemas(true)
                .locations("classpath:db/migration/metadata")
                .load();
    }

    @Bean(initMethod = "migrate")
    public Flyway ledgerServiceFlyway(DataSource ds) {
        return Flyway.configure()
                .dataSource(ds)
                .schemas("ledger_service")
                .createSchemas(true)
                .locations("classpath:db/migration/ledger")
                .load();
    }

    @Bean(initMethod = "migrate")
    public Flyway budgetServiceFlyway(DataSource ds) {
        return Flyway.configure()
                .dataSource(ds)
                .schemas("budget_service")
                .createSchemas(true)
                .locations("classpath:db/migration/budget")
                .load();
    }

    @Bean(initMethod = "migrate")
    public Flyway schedulerServiceFlyway(DataSource ds) {
        return Flyway.configure()
                .dataSource(ds)
                .schemas("scheduler_service")
                .createSchemas(true)
                .locations("classpath:db/migration/scheduler")
                .load();
    }
}
