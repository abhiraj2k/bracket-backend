package com.expensetracker.ledgerservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public interface CategoryBreakdownItem {
    UUID getCategoryId();
    BigDecimal getTotal();
}
