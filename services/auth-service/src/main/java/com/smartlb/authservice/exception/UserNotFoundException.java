package com.smartlb.authservice.exception;

/**
 * Exception thrown when a user entity cannot be located.
 */
public class UserNotFoundException extends ResourceNotFoundException {

    public UserNotFoundException(String message) {
        super(message);
    }
}
