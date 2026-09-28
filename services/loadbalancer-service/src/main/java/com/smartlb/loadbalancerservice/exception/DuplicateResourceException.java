package com.smartlb.loadbalancerservice.exception;

/**
 * Thrown when attempting to create a resource that violates uniqueness constraints.
 */
public class DuplicateResourceException extends BusinessException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
