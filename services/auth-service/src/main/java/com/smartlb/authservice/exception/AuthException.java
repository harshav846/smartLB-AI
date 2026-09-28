package com.smartlb.authservice.exception;

/**
 * Base runtime exception for domain and authentication service failures.
 */
public class AuthException extends RuntimeException {

    public AuthException(String message) {
        super(message);
    }

    public AuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
