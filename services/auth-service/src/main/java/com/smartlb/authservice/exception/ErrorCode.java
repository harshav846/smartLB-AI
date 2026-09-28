package com.smartlb.authservice.exception;

/**
 * Centralised enumeration of application-level error codes returned in the
 * {@link com.smartlb.authservice.dto.response.ErrorResponse#getErrorCode()} field.
 *
 * <p>Error codes follow the pattern {@code CATEGORY_DESCRIPTION} in SCREAMING_SNAKE_CASE.
 * They allow API consumers to programmatically distinguish specific error conditions
 * without parsing human-readable messages.</p>
 */
public enum ErrorCode {

    // ── Validation ──────────────────────────────────────────────────────────
    /** One or more request fields failed bean validation constraints. */
    VALIDATION_FAILED,
    /** A required request field is absent or blank. */
    MISSING_REQUIRED_FIELD,
    /** A request field value is syntactically or semantically malformed. */
    INVALID_FIELD_VALUE,
    /** The HTTP request body could not be parsed (malformed JSON, wrong type). */
    MALFORMED_REQUEST_BODY,

    // ── Authentication ───────────────────────────────────────────────────────
    /** Supplied credentials (email / password) are incorrect. */
    INVALID_CREDENTIALS,
    /** The supplied JWT access or refresh token is absent, expired, or forged. */
    INVALID_TOKEN,
    /** The request is missing required authentication credentials. */
    AUTHENTICATION_REQUIRED,

    // ── Authorization ────────────────────────────────────────────────────────
    /** The authenticated principal lacks permission to perform this operation. */
    ACCESS_DENIED,

    // ── Account State ────────────────────────────────────────────────────────
    /** The user account has been locked or suspended due to security violations. */
    ACCOUNT_LOCKED,
    /** The user account is pending email verification. */
    EMAIL_NOT_VERIFIED,
    /** The user account has been deactivated or deleted. */
    ACCOUNT_INACTIVE,

    // ── Resource ─────────────────────────────────────────────────────────────
    /** A requested domain entity (user, organization) does not exist. */
    RESOURCE_NOT_FOUND,
    /** A resource with the same unique attribute already exists (e.g., email, slug). */
    RESOURCE_ALREADY_EXISTS,

    // ── General ──────────────────────────────────────────────────────────────
    /** The request is well-formed but violates a business rule. */
    BAD_REQUEST,
    /** The server encountered an unexpected internal error. */
    INTERNAL_SERVER_ERROR,
}
