package com.example.currency_exchange.service;

import com.example.currency_exchange.dto.ExchangeRateResponse;
import com.example.currency_exchange.model.CurrencyEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.naming.ServiceUnavailableException;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateService {
    private static final String NBP_API_URL = "http://api.nbp.pl/api/exchangerates/rates/A/%s";
    private final RestTemplate restTemplate;

    @Cacheable(value = "exchangeRates", key = "#currencyEnum")
    public BigDecimal getCurrentRate(CurrencyEnum currencyEnum) throws ServiceUnavailableException {
        try {
            var url = String.format(NBP_API_URL, currencyEnum.name());
            var response = restTemplate.getForObject(url, ExchangeRateResponse.class);
            if (response == null || response.getRates() == null || response.getRates().isEmpty()) {
                log.error("Exchange rate response is null or empty!");
                throw new ServiceUnavailableException("Unable to get exchange rate from NBP API");
            }
            return response.getRates().get(0).getMid();
        } catch (RestClientException ex) {
            log.error("NBP API request failed", ex);
            throw new ServiceUnavailableException("NBP API service is unavailable");
        }
    }
}

