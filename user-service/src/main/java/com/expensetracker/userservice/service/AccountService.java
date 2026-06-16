package com.expensetracker.userservice.service;

import com.expensetracker.common.enums.AccountType;
import com.expensetracker.common.exception.ResourceNotFoundException;
import com.expensetracker.common.exception.ValidationException;
import com.expensetracker.userservice.dto.AccountResponse;
import com.expensetracker.userservice.dto.CreateAccountRequest;
import com.expensetracker.userservice.dto.UpdateAccountRequest;
import com.expensetracker.userservice.entity.Account;
import com.expensetracker.userservice.entity.AppUser;
import com.expensetracker.userservice.repository.AccountRepository;
import com.expensetracker.userservice.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountService {

    private final AccountRepository accountRepo;
    private final AppUserRepository userRepo;

    public AccountResponse create(UUID userId, CreateAccountRequest request) {
        AppUser user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (request.getAccountType() == AccountType.CREDIT_CARD) {
            validateBillingDays(request.getBillingStartDay(), request.getBillingEndDay());
        }

        Account account = new Account();
        account.setUserId(userId);
        account.setHouseholdId(user.getHouseholdId());
        account.setName(request.getName());
        account.setAccountType(request.getAccountType());
        account.setBalance(request.getOpeningBalance());
        account.setCurrencyCode(request.getCurrencyCode() != null ? request.getCurrencyCode() : "INR");
        account.setBillingStartDay(request.getBillingStartDay());
        account.setBillingEndDay(request.getBillingEndDay());

        return AccountResponse.from(accountRepo.save(account));
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> listActive(UUID userId) {
        return accountRepo.findByUserIdAndIsActiveTrue(userId).stream()
                .map(AccountResponse::from)
                .toList();
    }

    public AccountResponse update(UUID accountId, UUID userId, UpdateAccountRequest request) {
        Account account = accountRepo.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountId));

        if (!account.getUserId().equals(userId)) {
            throw new ValidationException("Account does not belong to user");
        }

        if (request.getName() != null) {
            account.setName(request.getName());
        }
        if (request.getIsActive() != null) {
            account.setIsActive(request.getIsActive());
        }

        return AccountResponse.from(accountRepo.save(account));
    }

    private void validateBillingDays(Integer start, Integer end) {
        if (start == null || end == null) {
            throw new ValidationException("billing_start_day and billing_end_day required for CREDIT_CARD accounts");
        }
        if (start < 1 || start > 31 || end < 1 || end > 31) {
            throw new ValidationException("billing_start_day and billing_end_day must be between 1 and 31");
        }
    }
}
