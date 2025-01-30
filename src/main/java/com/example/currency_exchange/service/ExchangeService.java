package com.example.currency_exchange.service;

import com.example.currency_exchange.exceptions.ResourceNotFoundException;
import com.example.currency_exchange.mapper.AccountMapper;
import com.example.currency_exchange.model.Account;
import com.example.currency_exchange.model.AccountResponse;
import com.example.currency_exchange.model.CurrencyEnum;
import com.example.currency_exchange.model.ExchangeRequest;
import com.example.currency_exchange.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.naming.ServiceUnavailableException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExchangeService {
    private final AccountRepository accountRepository;
    private final ExchangeRateService exchangeRateService;
    private final AccountBalanceValidator balanceValidator;
    private final AccountMapper accountMapper;

    @Transactional
    public AccountResponse exchangeCurrency(UUID accountId, ExchangeRequest request) throws ServiceUnavailableException {
        var account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        var amount = request.getAmount();
        var sourceCurrency = CurrencyEnum.valueOf(request.getSourceCurrency().name());
        balanceValidator.validateBalance(account, amount, sourceCurrency);

        var rate = exchangeRateService.getCurrentRate(CurrencyEnum.USD);
        var updatedAccount = performExchange(account, amount, sourceCurrency, rate);
        var savedAccount = accountRepository.save(updatedAccount);
        return accountMapper.toResponse(savedAccount);
    }

    private Account performExchange(Account account, BigDecimal amount, CurrencyEnum sourceCurrencyEnum, BigDecimal rate) {
        if (sourceCurrencyEnum == CurrencyEnum.PLN) {
            var usdAmount = amount.divide(rate, 2, RoundingMode.HALF_UP);
            account.setPlnBalance(account.getPlnBalance().subtract(amount));
            account.setUsdBalance(account.getUsdBalance().add(usdAmount));
        } else {
            var plnAmount = amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
            account.setUsdBalance(account.getUsdBalance().subtract(amount));
            account.setPlnBalance(account.getPlnBalance().add(plnAmount));
        }
        return account;
    }
}
