package com.example.currency_exchange;

import com.example.currency_exchange.dto.ExchangeRateResponse;
import com.example.currency_exchange.dto.Rate;
import com.example.currency_exchange.model.*;
import com.example.currency_exchange.repository.AccountRepository;
import com.example.currency_exchange.service.ExchangeRateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import javax.naming.ServiceUnavailableException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@EnableCaching
class CurrencyEnumExchangeApplicationTests {
    private static final String NBP_USD_RATE_URL = "http://api.nbp.pl/api/exchangerates/rates/A/USD";
    private static final BigDecimal INITIAL_BALANCE = BigDecimal.valueOf(1000);
    private static final BigDecimal EXCHANGE_RATE = BigDecimal.valueOf(4.0);
    private static final String TEST_FIRST_NAME = "Jan";
    private static final String TEST_LAST_NAME = "Kowalski";

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ExchangeRateService exchangeRateService;

    @Autowired
    private CacheManager cacheManager;

    @MockBean
    private RestTemplate restTemplate;

    private ExchangeRateResponse mockExchangeRateResponse;

    @BeforeEach
    void setUp() {
        mockExchangeRateResponse = createMockExchangeRateResponse(EXCHANGE_RATE);
        Mockito.reset(restTemplate);
        cacheManager.getCache("exchangeRates").clear();
    }


    @Test
    void shouldCreateAccount() {
        // given
        var request = createAccountRequest();

        // when
        var response = testRestTemplate.postForEntity("/api/accounts", request, AccountResponse.class);

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        var account = accountRepository.findById(response.getBody().getId());
        assertTrue(account.isPresent());
        assertEquals(INITIAL_BALANCE.setScale(2), account.get().getPlnBalance());
    }

    @Test
    void shouldGetAccount() {
        // given
        var accountId = createTestAccount();

        // when
        var response = testRestTemplate.getForEntity("/api/accounts/" + accountId, AccountResponse.class);

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(accountId, response.getBody().getId());
    }

    @Test
    void shouldExchangeCurrency() {
        // given
        var accountId = createTestAccount();
        setupMockNbpResponse();
        var exchangeRequest = new ExchangeRequest(BigDecimal.valueOf(100), ExchangeRequest.SourceCurrencyEnum.PLN);

        // when
        var response = testRestTemplate.postForEntity(
                "/api/accounts/" + accountId + "/exchange",
                exchangeRequest,
                AccountResponse.class
        );

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        var account = accountRepository.findById(response.getBody().getId());
        assertTrue(account.isPresent());
        assertEquals(BigDecimal.valueOf(900).setScale(2), account.get().getPlnBalance());
        assertEquals(BigDecimal.valueOf(25).setScale(2), account.get().getUsdBalance());
    }

    @Test
    void shouldUseCacheForExchangeRates() throws ServiceUnavailableException {
        // given
        setupMockNbpResponse();

        // when
        var firstCall = exchangeRateService.getCurrentRate(CurrencyEnum.USD);
        var secondCall = exchangeRateService.getCurrentRate(CurrencyEnum.USD);

        // then
        assertEquals(EXCHANGE_RATE, firstCall);
        assertEquals(EXCHANGE_RATE, secondCall);
        verify(restTemplate, times(1)).getForObject(
                eq(NBP_USD_RATE_URL),
                eq(ExchangeRateResponse.class));
    }

    @Test
    void shouldRejectAccountCreationWithNegativeBalance() {
        // given
        var request = new CreateAccountRequest(TEST_FIRST_NAME, TEST_LAST_NAME, BigDecimal.valueOf(-100));

        // when
        var response = testRestTemplate.postForEntity("/api/accounts", request, ErrorResponse.class);

        // then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getMessage().contains("Invalid request parameters"));
    }

    @Test
    void shouldRejectAccountCreationWithMissingName() {
        // given
        var request = new CreateAccountRequest("", TEST_LAST_NAME, INITIAL_BALANCE);

        // when
        var response = testRestTemplate.postForEntity("/api/accounts", request, ErrorResponse.class);

        // then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getMessage().contains("Invalid request parameters"));
    }

    @Test
    void shouldReturnNotFoundForNonExistentAccount() {
        // given
        var nonExistentId = UUID.randomUUID();

        // when
        var response = testRestTemplate.getForEntity("/api/accounts/" + nonExistentId, ErrorResponse.class);

        // then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getMessage().contains("Account not found"));
    }

    @Test
    void shouldRejectExchangeWithInsufficientFunds() {
        // given
        var accountId = createTestAccount();
        setupMockNbpResponse();
        var exchangeRequest = new ExchangeRequest(
                BigDecimal.valueOf(2000),
                ExchangeRequest.SourceCurrencyEnum.PLN
        );

        // when
        var response = testRestTemplate.postForEntity(
                "/api/accounts/" + accountId + "/exchange",
                exchangeRequest,
                ErrorResponse.class
        );

        // then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getMessage().contains("Insufficient balance"));
    }

    @Test
    void shouldHandleExchangeRateServiceFailure() {
        // given
        var accountId = createTestAccount();
        when(restTemplate.getForObject(
                eq(NBP_USD_RATE_URL),
                eq(ExchangeRateResponse.class)))
                .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

        var exchangeRequest = new ExchangeRequest(
                BigDecimal.valueOf(100),
                ExchangeRequest.SourceCurrencyEnum.PLN
        );

        // when
        var response = testRestTemplate.postForEntity(
                "/api/accounts/" + accountId + "/exchange",
                exchangeRequest,
                ErrorResponse.class
        );

        // then
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getMessage().contains("Unable to fetch exchange rate"));
    }

    @Test
    void shouldRejectExchangeWithZeroAmount() {
        // given
        var accountId = createTestAccount();
        setupMockNbpResponse();
        var exchangeRequest = new ExchangeRequest(
                BigDecimal.ZERO,
                ExchangeRequest.SourceCurrencyEnum.PLN
        );

        // when
        var response = testRestTemplate.postForEntity(
                "/api/accounts/" + accountId + "/exchange",
                exchangeRequest,
                ErrorResponse.class
        );

        // then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getMessage().contains("Invalid request parameters"));
    }

    @Test
    void shouldRejectExchangeWithNegativeAmount() {
        // given
        var accountId = createTestAccount();
        setupMockNbpResponse();
        var exchangeRequest = new ExchangeRequest(
                BigDecimal.valueOf(-100),
                ExchangeRequest.SourceCurrencyEnum.PLN
        );

        // when
        var response = testRestTemplate.postForEntity(
                "/api/accounts/" + accountId + "/exchange",
                exchangeRequest,
                ErrorResponse.class
        );

        // then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getMessage().contains("Invalid request parameters"));
    }

    private CreateAccountRequest createAccountRequest() {
        return new CreateAccountRequest(TEST_FIRST_NAME, TEST_LAST_NAME, INITIAL_BALANCE);
    }

    private UUID createTestAccount() {
        var response = testRestTemplate.postForEntity("/api/accounts", createAccountRequest(), AccountResponse.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        return response.getBody().getId();
    }

    private ExchangeRateResponse createMockExchangeRateResponse(BigDecimal rate) {
        var rateObj = new Rate();
        rateObj.setMid(rate);

        var response = new ExchangeRateResponse();
        response.setTable("A");
        response.setCurrency("dolar amerykański");
        response.setCode("USD");
        response.setRates(Collections.singletonList(rateObj));

        return response;
    }

    private void setupMockNbpResponse() {
        when(restTemplate.getForObject(
                eq(NBP_USD_RATE_URL),
                eq(ExchangeRateResponse.class)))
                .thenReturn(mockExchangeRateResponse);
    }
}