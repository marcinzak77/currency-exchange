package com.example.currency_exchange.exceptions;

import com.example.currency_exchange.model.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.RestClientException;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        var error = new ErrorResponse()
                .code("NOT_FOUND")
                .message(ex.getMessage())
                .details(Collections.emptyList());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ErrorResponse> handleValidation(InsufficientBalanceException ex) {
        var error = new ErrorResponse()
                .code("VALIDATION_ERROR")
                .message("Insufficient balance")
                .details(Collections.singletonList(ex.getMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler({RestClientException.class})
    public ResponseEntity<ErrorResponse> handleExternalService(RestClientException ex) {
        var error = new ErrorResponse()
                .code("EXTERNAL_SERVICE_ERROR")
                .message("Unable to fetch exchange rate")
                .details(Collections.singletonList("External service is unavailable"));
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }

    @ExceptionHandler(ServiceUnavailableHttpException.class)
    public ResponseEntity<ErrorResponse> handleServiceUnavailable(ServiceUnavailableHttpException ex) {
        var error = new ErrorResponse()
                .code("SERVICE_UNAVAILABLE")
                .message("Unable to fetch exchange rate")
                .details(List.of());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleServiceUnavailable(MethodArgumentNotValidException ex) {
        var errorMessages = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .collect(Collectors.toList());

        var error = new ErrorResponse()
                .code("VALIDATION_ERROR")
                .message("Invalid request parameters")
                .details(errorMessages);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}