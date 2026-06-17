package com.expensetracker.userservice.service;

import com.expensetracker.common.exception.ResourceNotFoundException;
import com.expensetracker.common.exception.ValidationException;
import com.expensetracker.userservice.dto.AuthResponse;
import com.expensetracker.userservice.dto.LoginRequest;
import com.expensetracker.userservice.dto.RegisterRequest;
import com.expensetracker.userservice.dto.UserResponse;
import com.expensetracker.userservice.entity.AppUser;
import com.expensetracker.userservice.entity.Household;
import com.expensetracker.userservice.repository.AppUserRepository;
import com.expensetracker.userservice.repository.HouseholdRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final HouseholdRepository householdRepo;
    private final AppUserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest request) {
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
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user = userRepo.save(user);

        String token = jwtService.issueToken(user.getId(), user.getHouseholdId());
        return new AuthResponse(token, user.getId(), user.getHouseholdId(), user.getName(), user.getEmail());
    }

    public AuthResponse login(LoginRequest request) {
        AppUser user = userRepo.findByEmail(request.getEmail())
                .orElseThrow(() -> new ValidationException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ValidationException("Invalid email or password");
        }

        String token = jwtService.issueToken(user.getId(), user.getHouseholdId());
        return new AuthResponse(token, user.getId(), user.getHouseholdId(), user.getName(), user.getEmail());
    }

    @Transactional(readOnly = true)
    public UserResponse getById(UUID userId) {
        AppUser user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return UserResponse.from(user);
    }
}
