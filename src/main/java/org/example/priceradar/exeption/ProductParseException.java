package org.example.priceradar.exeption;

public class ProductParseException extends RuntimeException {
    public ProductParseException(String message, Throwable cause) {
        super(message, cause);
    }
}