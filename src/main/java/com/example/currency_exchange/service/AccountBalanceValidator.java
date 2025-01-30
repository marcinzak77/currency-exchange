package com.example.currency_exchange.service;

import com.example.currency_exchange.exceptions.InsufficientBalanceException;
import com.example.currency_exchange.model.Account;
import com.example.currency_exchange.model.CurrencyEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
public class AccountBalanceValidator {

    public void validateBalance(Account account, BigDecimal amount, CurrencyEnum sourceCurrencyEnum) {
        if (sourceCurrencyEnum == CurrencyEnum.PLN && account.getPlnBalance().compareTo(amount) < 0) {
            log.error("Insufficient PLN balance");
            throw new InsufficientBalanceException("Insufficient PLN balance");
        }
        if (sourceCurrencyEnum == CurrencyEnum.USD && account.getUsdBalance().compareTo(amount) < 0) {
            log.error("Insufficient USD balance");
            throw new InsufficientBalanceException("Insufficient USD balance");
        }
    }
}
