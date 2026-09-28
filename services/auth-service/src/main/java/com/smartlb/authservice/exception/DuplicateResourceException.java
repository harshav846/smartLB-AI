package com.smartlb.authservice.exception;

/**
 * Exception thrown when attempting to create a resource with a unique attribute that already exists (e.g., email or slug).
 */
public class DuplicateResourceException extends AuthException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
