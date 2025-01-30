package com.example.currency_exchange.service;

import com.example.currency_exchange.exceptions.ResourceNotFoundException;
import com.example.currency_exchange.mapper.AccountMapper;
import com.example.currency_exchange.model.Account;
import com.example.currency_exchange.model.AccountResponse;
import com.example.currency_exchange.model.CreateAccountRequest;
import com.example.currency_exchange.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        Account account = new Account(
                request.getFirstName(),
                request.getLastName(),
                request.getInitialBalance()
        );
        Account savedAccount = accountRepository.save(account);
        return accountMapper.toResponse(savedAccount);
    }

    public AccountResponse getAccount(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        return accountMapper.toResponse(account);
    }
}