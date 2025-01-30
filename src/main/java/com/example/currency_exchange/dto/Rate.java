package com.example.currency_exchange.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class Rate {
    String no;
    String effectiveDate;
    BigDecimal mid;
}
