package com.example.currency_exchange.controller;

import com.example.currency_exchange.api.AccountsApi;
import com.example.currency_exchange.exceptions.ServiceUnavailableHttpException;
import com.example.currency_exchange.model.AccountResponse;
import com.example.currency_exchange.model.CreateAccountRequest;
import com.example.currency_exchange.model.ExchangeRequest;
import com.example.currency_exchange.service.AccountService;
import com.example.currency_exchange.service.ExchangeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import javax.naming.ServiceUnavailableException;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AccountController implements AccountsApi {
    private final AccountService accountService;
    private final ExchangeService exchangeService;

    @Override
    public ResponseEntity<AccountResponse> createAccount(CreateAccountRequest createAccountRequest) {
        return ResponseEntity.ok(accountService.createAccount(createAccountRequest));
    }

    @Override
    public ResponseEntity<AccountResponse> getAccount(UUID accountId) {
        return ResponseEntity.ok(accountService.getAccount(accountId));
    }

    @Override
    public ResponseEntity<AccountResponse> exchangeCurrency(UUID accountId, ExchangeRequest request) {
        try {
            return ResponseEntity.ok(exchangeService.exchangeCurrency(accountId, request));
        } catch (ServiceUnavailableException e) {
            throw new ServiceUnavailableHttpException(e.getMessage());
        }
    }
}
