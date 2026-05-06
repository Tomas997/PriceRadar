package com.example.parserservice.exception;

public class ProductParseException extends RuntimeException {
    public ProductParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
