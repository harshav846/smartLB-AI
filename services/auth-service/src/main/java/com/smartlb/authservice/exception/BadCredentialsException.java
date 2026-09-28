package com.smartlb.authservice.exception;

/**
 * Exception thrown when login credentials are invalid (wrong email or password).
 */
public class BadCredentialsException extends AuthException {

    public BadCredentialsException(String message) {
        super(message);
    }
}
