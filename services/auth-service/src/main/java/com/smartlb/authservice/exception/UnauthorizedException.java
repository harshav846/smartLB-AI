package com.smartlb.authservice.exception;

/**
 * Exception thrown when a request arrives without valid authentication credentials,
 * or when authentication has failed due to an invalid or missing token.
 *
 * <p>Maps to HTTP {@code 401 Unauthorized}.</p>
 */
public class UnauthorizedException extends AuthException {

    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}
