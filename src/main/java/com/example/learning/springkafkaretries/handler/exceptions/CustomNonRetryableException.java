package com.example.learning.springkafkaretries.handler.exceptions;

public class CustomNonRetryableException extends RuntimeException {
    public CustomNonRetryableException(String message) {
        super(message);
    }
}
