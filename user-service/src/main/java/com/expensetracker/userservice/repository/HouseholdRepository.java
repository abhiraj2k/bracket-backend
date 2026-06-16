package com.expensetracker.userservice.repository;

import com.expensetracker.userservice.entity.Household;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface HouseholdRepository extends JpaRepository<Household, UUID> {
}
