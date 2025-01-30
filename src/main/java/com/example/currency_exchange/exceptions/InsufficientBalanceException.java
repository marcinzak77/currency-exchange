package com.example.currency_exchange.exceptions;

public class InsufficientBalanceException extends RuntimeException{
    public InsufficientBalanceException(String message) {
        super(message);
    }
}
