package com.example.currency_exchange.service;

import com.example.currency_exchange.exceptions.ResourceNotFoundException;
import com.example.currency_exchange.mapper.AccountMapper;
import com.example.currency_exchange.model.Account;
import com.example.currency_exchange.model.AccountResponse;
import com.example.currency_exchange.model.CreateAccountRequest;
import com.example.currency_exchange.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AccountServiceTest {

    private static final String FIRST_NAME = "Jan";
    private static final String LAST_NAME = "Kowalski";
    private static final BigDecimal INITIAL_BALANCE = BigDecimal.valueOf(1000);

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountMapper accountMapper;

    @InjectMocks
    private AccountService sut;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldCreateAccount() {
        // given
        var request = prepareCreateRequest();
        var account = prepareAccount();
        var expectedResponse = prepareAccountResponse();

        when(accountRepository.save(any(Account.class))).thenReturn(account);
        when(accountMapper.toResponse(account)).thenReturn(expectedResponse);

        // when
        var result = sut.createAccount(request);

        // then
        verify(accountRepository).save(any(Account.class));
        verify(accountMapper).toResponse(account);
        verifyNoMoreInteractions(accountRepository, accountMapper);

        assertEquals(expectedResponse, result);
    }

    @Test
    void shouldGetExistingAccount() {
        // given
        var accountId = UUID.randomUUID();
        var account = prepareAccount();
        var expectedResponse = prepareAccountResponse();

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(accountMapper.toResponse(account)).thenReturn(expectedResponse);

        // when
        var result = sut.getAccount(accountId);

        // then
        verify(accountRepository).findById(accountId);
        verify(accountMapper).toResponse(account);
        verifyNoMoreInteractions(accountRepository, accountMapper);

        assertEquals(expectedResponse, result);
    }

    @Test
    void shouldThrowExceptionWhenAccountNotFound() {
        // given
        var accountId = UUID.randomUUID();
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        // when/then
        var exception = assertThrows(
                ResourceNotFoundException.class,
                () -> sut.getAccount(accountId)
        );

        assertEquals("Account not found", exception.getMessage());
        verify(accountRepository).findById(accountId);
        verifyNoInteractions(accountMapper);
        verifyNoMoreInteractions(accountRepository);
    }

    private CreateAccountRequest prepareCreateRequest() {
        var request = new CreateAccountRequest();
        request.setFirstName(FIRST_NAME);
        request.setLastName(LAST_NAME);
        request.setInitialBalance(INITIAL_BALANCE);
        return request;
    }

    private Account prepareAccount() {
        return new Account(FIRST_NAME, LAST_NAME, INITIAL_BALANCE);
    }

    private AccountResponse prepareAccountResponse() {
        var response = new AccountResponse();
        response.setId(UUID.randomUUID());
        response.setFirstName(FIRST_NAME);
        response.setLastName(LAST_NAME);
        response.setPlnBalance(INITIAL_BALANCE);
        response.setUsdBalance(BigDecimal.ZERO);
        return response;
    }
}