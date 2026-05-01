package org.example.priceradar.Exeptions;

public class ProductParseException extends RuntimeException {
    public ProductParseException(String message, Throwable cause) {
        super(message, cause);
    }
}