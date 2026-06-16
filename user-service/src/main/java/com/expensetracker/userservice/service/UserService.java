package com.expensetracker.userservice.service;

import com.expensetracker.common.exception.ValidationException;
import com.expensetracker.userservice.dto.RegisterRequest;
import com.expensetracker.userservice.dto.UserResponse;
import com.expensetracker.userservice.entity.AppUser;
import com.expensetracker.userservice.entity.Household;
import com.expensetracker.userservice.repository.AppUserRepository;
import com.expensetracker.userservice.repository.HouseholdRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final HouseholdRepository householdRepo;
    private final AppUserRepository userRepo;

    public UserResponse register(RegisterRequest request) {
        if (userRepo.existsByEmail(request.getEmail())) {
            throw new ValidationException("Email already registered: " + request.getEmail());
        }

        Household household = new Household();
        household.setName(request.getName() + "'s Household");
        household = householdRepo.save(household);

        AppUser user = new AppUser();
        user.setHouseholdId(household.getId());
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user = userRepo.save(user);

        return UserResponse.from(user);
    }
}
