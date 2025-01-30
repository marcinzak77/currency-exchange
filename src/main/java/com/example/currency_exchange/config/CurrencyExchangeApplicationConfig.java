package com.example.currency_exchange.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class CurrencyExchangeApplicationConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
