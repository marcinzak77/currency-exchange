package com.example.currency_exchange.service;

import com.example.currency_exchange.dto.ExchangeRateResponse;
import com.example.currency_exchange.dto.Rate;
import com.example.currency_exchange.model.CurrencyEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.naming.ServiceUnavailableException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class ExchangeRateServiceTest {

    private static final String NBP_API_URL = "http://api.nbp.pl/api/exchangerates/rates/A/USD";
    private static final BigDecimal EXCHANGE_RATE = BigDecimal.valueOf(4.0);

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ExchangeRateService sut;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldReturnCurrentExchangeRate() throws ServiceUnavailableException {
        // given
        var response = prepareValidResponse();
        when(restTemplate.getForObject(eq(NBP_API_URL), eq(ExchangeRateResponse.class)))
                .thenReturn(response);

        // when
        var result = sut.getCurrentRate(CurrencyEnum.USD);

        // then
        assertEquals(EXCHANGE_RATE, result);
    }

    @Test
    void shouldThrowExceptionWhenResponseIsNull() {
        // given
        when(restTemplate.getForObject(eq(NBP_API_URL), eq(ExchangeRateResponse.class)))
                .thenReturn(null);

        // when/then
        var exception = assertThrows(
                ServiceUnavailableException.class,
                () -> sut.getCurrentRate(CurrencyEnum.USD)
        );
        assertEquals("Unable to get exchange rate from NBP API", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenRatesAreNull() {
        // given
        var response = new ExchangeRateResponse();
        response.setRates(null);
        when(restTemplate.getForObject(eq(NBP_API_URL), eq(ExchangeRateResponse.class)))
                .thenReturn(response);

        // when/then
        var exception = assertThrows(
                ServiceUnavailableException.class,
                () -> sut.getCurrentRate(CurrencyEnum.USD)
        );
        assertEquals("Unable to get exchange rate from NBP API", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenRatesAreEmpty() {
        // given
        var response = new ExchangeRateResponse();
        response.setRates(Collections.emptyList());
        when(restTemplate.getForObject(eq(NBP_API_URL), eq(ExchangeRateResponse.class)))
                .thenReturn(response);

        // when/then
        var exception = assertThrows(
                ServiceUnavailableException.class,
                () -> sut.getCurrentRate(CurrencyEnum.USD)
        );
        assertEquals("Unable to get exchange rate from NBP API", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenNbpApiIsUnavailable() {
        // given
        when(restTemplate.getForObject(eq(NBP_API_URL), eq(ExchangeRateResponse.class)))
                .thenThrow(new RestClientException("Connection refused"));

        // when/then
        var exception = assertThrows(
                ServiceUnavailableException.class,
                () -> sut.getCurrentRate(CurrencyEnum.USD)
        );
        assertEquals("NBP API service is unavailable", exception.getMessage());
    }

    @Test
    void shouldHandleNetworkTimeout() {
        // given
        when(restTemplate.getForObject(eq(NBP_API_URL), eq(ExchangeRateResponse.class)))
                .thenThrow(new RestClientException("Read timed out"));

        // when/then
        var exception = assertThrows(
                ServiceUnavailableException.class,
                () -> sut.getCurrentRate(CurrencyEnum.USD)
        );
        assertEquals("NBP API service is unavailable", exception.getMessage());
    }

    private ExchangeRateResponse prepareValidResponse() {
        var rate = new Rate();
        rate.setMid(EXCHANGE_RATE);

        var response = new ExchangeRateResponse();
        response.setRates(List.of(rate));
        return response;
    }
}