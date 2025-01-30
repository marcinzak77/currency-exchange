package com.example.currency_exchange.dto;

import lombok.Data;

import java.util.List;

@Data
public class ExchangeRateResponse {
    String table;
    String currency;
    String code;
    List<Rate> rates;
}

