package com.expensetracker.reportingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

// No JPA/Flyway — reporting-service has no owned tables.
// Data access will be wired via REST calls to other services (to be implemented).
@SpringBootApplication(scanBasePackages = {"com.expensetracker.reportingservice", "com.expensetracker.common"})
@EnableDiscoveryClient
public class ReportingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReportingServiceApplication.class, args);
    }
}
