package com.smartlb.authservice.exception;

/**
 * Exception thrown when a request is syntactically valid but semantically incorrect
 * or violates a business rule that is not covered by bean validation.
 *
 * <p>Maps to HTTP {@code 400 Bad Request}.</p>
 */
public class BadRequestException extends AuthException {

    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
