package com.smartlb.authservice.exception;

/**
 * Exception thrown when a user's email address has not been verified.
 */
public class EmailNotVerifiedException extends AuthException {

    public EmailNotVerifiedException(String message) {
        super(message);
    }
}
