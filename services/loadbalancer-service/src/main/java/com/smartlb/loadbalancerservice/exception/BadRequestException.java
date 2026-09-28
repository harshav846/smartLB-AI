package com.smartlb.loadbalancerservice.exception;

/**
 * Thrown when client request parameters fail business validation.
 */
public class BadRequestException extends BusinessException {
    public BadRequestException(String message) {
        super(message);
    }
}
