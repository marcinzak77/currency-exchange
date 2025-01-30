package com.example.currency_exchange.model;

import lombok.Getter;

@Getter
public enum CurrencyEnum {
    PLN("PLN", "Polski złoty"),
    USD("USD", "US Dollar");


    private final String code;
    private final String description;

    CurrencyEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
