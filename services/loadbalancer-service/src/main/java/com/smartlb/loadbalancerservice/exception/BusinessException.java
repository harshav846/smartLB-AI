package com.smartlb.loadbalancerservice.exception;

/**
 * Base custom runtime exception for business rule violations.
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
