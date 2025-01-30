package com.example.currency_exchange.exceptions;

public class ServiceUnavailableHttpException extends RuntimeException {
    public ServiceUnavailableHttpException(String message) {
        super(message);
    }
}
