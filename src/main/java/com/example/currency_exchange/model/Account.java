package com.example.currency_exchange.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
public class Account {

    @Id
    @GeneratedValue
    private UUID id;

    private String firstName;
    private String lastName;
    private BigDecimal plnBalance;
    private BigDecimal usdBalance;

    public Account(String firstName, String lastName, BigDecimal initialPlnBalance) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.plnBalance = initialPlnBalance;
        this.usdBalance = BigDecimal.ZERO;
    }
}
