package com.smartlb.authservice.exception;

/**
 * Exception thrown when an organization tenant cannot be located.
 */
public class OrganizationNotFoundException extends ResourceNotFoundException {

    public OrganizationNotFoundException(String message) {
        super(message);
    }
}
