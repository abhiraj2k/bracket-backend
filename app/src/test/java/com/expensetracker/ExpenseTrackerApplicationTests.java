package com.expensetracker;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("integration test — requires running Postgres and Redis")
class ExpenseTrackerApplicationTests {

    @Test
    void contextLoads() {
    }
}
