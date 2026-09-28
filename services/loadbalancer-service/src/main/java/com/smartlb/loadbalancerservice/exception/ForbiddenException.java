package com.smartlb.loadbalancerservice.exception;

/**
 * Thrown when an authenticated user attempts an operation outside their permission or tenant boundaries.
 */
public class ForbiddenException extends BusinessException {
    public ForbiddenException(String message) {
        super(message);
    }
}
