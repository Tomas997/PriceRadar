package org.example.frontendservice.exception;

public class SessionExpiredException extends RuntimeException {
    public SessionExpiredException() {
        super("Session expired");
    }
}
