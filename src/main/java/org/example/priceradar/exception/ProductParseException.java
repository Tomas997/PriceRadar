package org.example.priceradar.exception;

public class ProductParseException extends RuntimeException {
    public ProductParseException(String message, Throwable cause) {
        super(message, cause);
    }
}