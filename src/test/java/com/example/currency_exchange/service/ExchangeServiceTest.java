package com.example.currency_exchange.service;

import com.example.currency_exchange.mapper.AccountMapper;
import com.example.currency_exchange.model.Account;
import com.example.currency_exchange.model.CurrencyEnum;
import com.example.currency_exchange.model.ExchangeRequest;
import com.example.currency_exchange.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import javax.naming.ServiceUnavailableException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ExchangeServiceTest {

    @InjectMocks
    private ExchangeService sut;

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private ExchangeRateService exchangeRateService;
    @Mock
    private AccountBalanceValidator balanceValidator;
    @Mock
    private AccountMapper accountMapper;

    @Captor
    private ArgumentCaptor<Account> accountCaptor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        Mockito.reset(accountRepository, exchangeRateService, balanceValidator);
    }

    @Test
    void shouldThrowExceptionWhenAccountNotFound() {
        var accountId = UUID.randomUUID();
        var request = prepareRequest(BigDecimal.TEN, CurrencyEnum.PLN);
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> sut.exchangeCurrency(accountId, request));
        verifyNoInteractions(accountMapper, exchangeRateService);
    }

    @Test
    void shouldPerformExchangeFromPLNtoUSD() throws ServiceUnavailableException {
        var accountId = UUID.randomUUID();
        var amount = BigDecimal.valueOf(100);
        var account = prepareAccount(accountId);
        var request = prepareRequest(amount, CurrencyEnum.PLN);

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(exchangeRateService.getCurrentRate(CurrencyEnum.USD)).thenReturn(BigDecimal.valueOf(4));

        sut.exchangeCurrency(accountId, request);

        verify(balanceValidator).validateBalance(account, amount, CurrencyEnum.PLN);
        verify(accountRepository).findById(accountId);
        verify(exchangeRateService).getCurrentRate(CurrencyEnum.USD);
        verify(accountRepository).save(accountCaptor.capture());
        var capturedAccount = accountCaptor.getValue();
        // AccountMapper is intentionally not verified
        assertEquals(BigDecimal.valueOf(900).setScale(2, RoundingMode.HALF_UP), capturedAccount.getPlnBalance().setScale(2, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(75).setScale(2, RoundingMode.HALF_UP), capturedAccount.getUsdBalance().setScale(2, RoundingMode.HALF_UP));
        verifyNoMoreInteractions(accountRepository, balanceValidator, exchangeRateService);
    }

    private ExchangeRequest prepareRequest(BigDecimal amount, CurrencyEnum currencyEnum) {
        var request = new ExchangeRequest();
        request.setAmount(amount);
        request.setSourceCurrency(ExchangeRequest.SourceCurrencyEnum.fromValue(currencyEnum.name()));
        return request;
    }

    @Test
    void shouldPerformExchangeFromUSDtoPLN() throws ServiceUnavailableException {
        var accountId = UUID.randomUUID();
        var amount = BigDecimal.valueOf(10);
        var account = prepareAccount(accountId);
        var request = prepareRequest(amount, CurrencyEnum.USD);

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(exchangeRateService.getCurrentRate(CurrencyEnum.USD)).thenReturn(BigDecimal.valueOf(4));

        sut.exchangeCurrency(accountId, request);

        verify(balanceValidator).validateBalance(any(), any(), any());
        verify(accountRepository).findById(accountId);
        verify(exchangeRateService).getCurrentRate(CurrencyEnum.USD);
        verify(accountRepository).save(accountCaptor.capture());
        var capturedAccount = accountCaptor.getValue();
        // AccountMapper is intentionally not verified
        assertEquals(BigDecimal.valueOf(1040).setScale(2, RoundingMode.HALF_UP), capturedAccount.getPlnBalance().setScale(2, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.valueOf(40).setScale(2, RoundingMode.HALF_UP), capturedAccount.getUsdBalance().setScale(2, RoundingMode.HALF_UP));
        verifyNoMoreInteractions(accountRepository, balanceValidator, exchangeRateService);
    }

    private Account prepareAccount(UUID accountId) {
        var account = new Account();
        account.setPlnBalance(BigDecimal.valueOf(1000));
        account.setUsdBalance(BigDecimal.valueOf(50));
        account.setId(accountId);
        account.setFirstName("Jan");
        account.setLastName("Kowalski");
        return account;
    }

}