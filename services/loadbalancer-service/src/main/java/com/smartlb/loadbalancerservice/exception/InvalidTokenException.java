package com.smartlb.loadbalancerservice.exception;

/**
 * Exception thrown when a JWT token is invalid, expired, or malformed.
 */
public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException(String message) {
        super(message);
    }
}
