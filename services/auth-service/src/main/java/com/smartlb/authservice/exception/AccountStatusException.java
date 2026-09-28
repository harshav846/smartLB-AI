package com.smartlb.authservice.exception;

/**
 * Exception thrown when a user account is locked, suspended, or not yet activated.
 */
public class AccountStatusException extends AuthException {

    public AccountStatusException(String message) {
        super(message);
    }
}
