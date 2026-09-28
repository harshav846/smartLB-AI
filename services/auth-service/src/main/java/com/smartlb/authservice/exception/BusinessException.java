package com.smartlb.authservice.exception;

/**
 * Exception thrown when a request is well-formed but violates a specific domain or business rule.
 *
 * <p>Maps to HTTP {@code 422 Unprocessable Entity} or {@code 400 Bad Request}.</p>
 */
public class BusinessException extends AuthException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
