package com.example.learning.springkafkaretries.handler.exceptions;

public class CustomRetryableException extends RuntimeException {
    public CustomRetryableException(String message) {
        super(message);
    }
}
