package com.example.currency_exchange.service;

import com.example.currency_exchange.exceptions.ResourceNotFoundException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ExchangeServiceTest {
    private static final BigDecimal INITIAL_PLN_BALANCE = BigDecimal.valueOf(1000);
    private static final BigDecimal INITIAL_USD_BALANCE = BigDecimal.valueOf(50);
    private static final BigDecimal EXCHANGE_RATE = BigDecimal.valueOf(4);

    @InjectMocks
    private ExchangeService sut;

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private ExchangeRateService exchangeRateService;
    @Mock
    private AccountBalanceValidator balanceValidator;
    @Mock
    AccountMapper accountMapper;
    @Captor
    private ArgumentCaptor<Account> accountCaptor;

    private UUID accountId;

    @BeforeEach
    void setUp() throws ServiceUnavailableException {
        MockitoAnnotations.openMocks(this);
        accountId = UUID.randomUUID();
        setupCommonMocks();
    }

    @Test
    void shouldThrowExceptionWhenAccountNotFound() {
        // given
        var request = prepareExchangeRequest(BigDecimal.TEN, CurrencyEnum.PLN);
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        // when
        var exception = assertThrows(
                ResourceNotFoundException.class,
                () -> sut.exchangeCurrency(accountId, request)
        );

        // then
        assertEquals("Account not found", exception.getMessage());
        verify(accountRepository).findById(accountId);
        verifyNoMoreInteractions(accountRepository, exchangeRateService, balanceValidator, accountMapper);
    }

    @Test
    void shouldExchangeFromPLNtoUSD() throws ServiceUnavailableException {
        // given
        var amount = BigDecimal.valueOf(100);
        var request = prepareExchangeRequest(amount, CurrencyEnum.PLN);

        // when
        sut.exchangeCurrency(accountId, request);

        // then
        verifyCommonInteractions(amount, CurrencyEnum.PLN);
        assertBalances(
                BigDecimal.valueOf(900),  // 1000 PLN - 100 PLN
                BigDecimal.valueOf(75)    // 50 USD + (100 PLN / 4 USD/PLN) = 50 + 25 = 75 USD
        );
    }

    @Test
    void shouldExchangeFromUSDtoPLN() throws ServiceUnavailableException {
        // given
        var amount = BigDecimal.valueOf(10);
        var request = prepareExchangeRequest(amount, CurrencyEnum.USD);

        // when
        sut.exchangeCurrency(accountId, request);

        // then
        verifyCommonInteractions(amount, CurrencyEnum.USD);
        assertBalances(
                BigDecimal.valueOf(1040), // 1000 PLN + (10 USD * 4 PLN/USD) = 1000 + 40 = 1040 PLN
                BigDecimal.valueOf(40)    // 50 USD - 10 USD
        );
    }

    private void setupCommonMocks() throws ServiceUnavailableException {
        when(accountRepository.findById(accountId))
                .thenReturn(Optional.of(prepareAccount()));
        when(exchangeRateService.getCurrentRate(CurrencyEnum.USD))
                .thenReturn(EXCHANGE_RATE);
    }

    private Account prepareAccount() {
        var account = new Account();
        account.setId(accountId);
        account.setPlnBalance(INITIAL_PLN_BALANCE);
        account.setUsdBalance(INITIAL_USD_BALANCE);
        return account;
    }

    private ExchangeRequest prepareExchangeRequest(BigDecimal amount, CurrencyEnum currency) {
        var request = new ExchangeRequest();
        request.setAmount(amount);
        request.setSourceCurrency(ExchangeRequest.SourceCurrencyEnum.fromValue(currency.name()));
        return request;
    }

    private void verifyCommonInteractions(BigDecimal amount, CurrencyEnum currency) throws ServiceUnavailableException {
        verify(balanceValidator).validateBalance(any(), eq(amount), eq(currency));
        verify(accountRepository).findById(accountId);
        verify(exchangeRateService).getCurrentRate(CurrencyEnum.USD);
        verify(accountRepository).save(accountCaptor.capture());
        verify(accountMapper, times(1)).toResponse(any());
    }

    private void assertBalances(BigDecimal expectedPln, BigDecimal expectedUsd) {
        var capturedAccount = accountCaptor.getValue();
        assertEquals(
                expectedPln.setScale(2, RoundingMode.HALF_UP),
                capturedAccount.getPlnBalance().setScale(2, RoundingMode.HALF_UP)
        );
        assertEquals(
                expectedUsd.setScale(2, RoundingMode.HALF_UP),
                capturedAccount.getUsdBalance().setScale(2, RoundingMode.HALF_UP)
        );
    }
}