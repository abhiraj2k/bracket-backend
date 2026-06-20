package com.expensetracker.user.repository;

import com.expensetracker.user.entity.Household;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface HouseholdRepository extends JpaRepository<Household, UUID> {
}
