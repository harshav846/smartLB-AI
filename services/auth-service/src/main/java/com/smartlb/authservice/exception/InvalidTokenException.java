package com.smartlb.authservice.exception;

/**
 * Exception thrown when a token is invalid, expired, or malformed.
 */
public class InvalidTokenException extends AuthException {

    public InvalidTokenException(String message) {
        super(message);
    }
}
