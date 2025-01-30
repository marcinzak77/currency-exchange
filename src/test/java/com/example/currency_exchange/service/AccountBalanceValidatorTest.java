package com.example.currency_exchange.service;

import com.example.currency_exchange.exceptions.InsufficientBalanceException;
import com.example.currency_exchange.model.Account;
import com.example.currency_exchange.model.CurrencyEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class AccountBalanceValidatorTest {

    private static final BigDecimal PLN_BALANCE = BigDecimal.valueOf(1000);
    private static final BigDecimal USD_BALANCE = BigDecimal.valueOf(100);

    private AccountBalanceValidator sut;
    private Account account;

    @BeforeEach
    void setUp() {
        sut = new AccountBalanceValidator();
        account = prepareAccount();
    }

    @ParameterizedTest(name = "should validate that balance {1} is less than available {2} when {0} amount is requested")
    @MethodSource("validParameters")
    void shouldValidateWhenBalanceIsSufficient(
            CurrencyEnum currency,
            BigDecimal amount,
            BigDecimal availableBalance
    ) {
        // given
        account.setPlnBalance(availableBalance);
        account.setUsdBalance(availableBalance);

        // when/then
        assertDoesNotThrow(() ->
                sut.validateBalance(account, amount, currency)
        );
    }

    private static Stream<Arguments> validParameters() {
        return Stream.of(
                arguments(CurrencyEnum.PLN, BigDecimal.valueOf(999.99), PLN_BALANCE),
                arguments(CurrencyEnum.PLN, BigDecimal.ZERO, PLN_BALANCE),
                arguments(CurrencyEnum.PLN, PLN_BALANCE, PLN_BALANCE),
                arguments(CurrencyEnum.USD, BigDecimal.valueOf(99.99), USD_BALANCE),
                arguments(CurrencyEnum.USD, BigDecimal.ZERO, USD_BALANCE),
                arguments(CurrencyEnum.USD, USD_BALANCE, USD_BALANCE)
        );
    }

    @ParameterizedTest(name = "should throw exception when the amount {1} in {0} exceeds the balance {2}")
    @MethodSource("invalidParameters")
    void shouldThrowExceptionWhenBalanceIsInsufficient(
            CurrencyEnum currency,
            BigDecimal amount,
            BigDecimal availableBalance
    ) {
        // given
        account.setPlnBalance(availableBalance);
        account.setUsdBalance(availableBalance);

        // when/then
        var exception = assertThrows(
                InsufficientBalanceException.class,
                () -> sut.validateBalance(account, amount, currency)
        );

        assertExceptionMessage(exception, currency);
    }

    private static Stream<Arguments> invalidParameters() {
        return Stream.of(
                arguments(CurrencyEnum.PLN, BigDecimal.valueOf(1000.01), PLN_BALANCE),
                arguments(CurrencyEnum.PLN, BigDecimal.valueOf(9999.99), PLN_BALANCE),
                arguments(CurrencyEnum.USD, BigDecimal.valueOf(100.01), USD_BALANCE),
                arguments(CurrencyEnum.USD, BigDecimal.valueOf(999.99), USD_BALANCE)
        );
    }

    @Test
    void shouldThrowExceptionWhenPlnBalanceIsZeroAndTryingToValidate() {
        // given
        account.setPlnBalance(BigDecimal.ZERO);
        var amount = BigDecimal.ONE;

        // when/then
        var exception = assertThrows(
                InsufficientBalanceException.class,
                () -> sut.validateBalance(account, amount, CurrencyEnum.PLN)
        );

        assertExceptionMessage(exception, CurrencyEnum.PLN);
    }

    @Test
    void shouldThrowExceptionWhenUsdBalanceIsZeroAndTryingToValidate() {
        // given
        account.setUsdBalance(BigDecimal.ZERO);
        var amount = BigDecimal.ONE;

        // when/then
        var exception = assertThrows(
                InsufficientBalanceException.class,
                () -> sut.validateBalance(account, amount, CurrencyEnum.USD)
        );

        assertExceptionMessage(exception, CurrencyEnum.USD);
    }

    private Account prepareAccount() {
        var account = new Account();
        account.setPlnBalance(PLN_BALANCE);
        account.setUsdBalance(USD_BALANCE);
        return account;
    }

    private void assertExceptionMessage(InsufficientBalanceException exception, CurrencyEnum currency) {
        var expectedMessage = String.format("Insufficient %s balance", currency.name());
        assertEquals(expectedMessage, exception.getMessage());
    }
}