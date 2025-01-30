package com.example.currency_exchange.mapper;

import com.example.currency_exchange.model.Account;
import com.example.currency_exchange.model.AccountResponse;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

    public AccountResponse toResponse(Account account) {
        AccountResponse response = new AccountResponse();
        response.setId(account.getId());
        response.setFirstName(account.getFirstName());
        response.setLastName(account.getLastName());
        response.setPlnBalance(account.getPlnBalance());
        response.setUsdBalance(account.getUsdBalance());
        return response;
    }
}