package com.smartlb.authservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Standard error response envelope returned by the {@code GlobalExceptionHandler}
 * for every exception condition in the authentication service.
 *
 * <p>All fields except {@code validationErrors} are always present.
 * {@code validationErrors} is only populated for {@code 422 Unprocessable Entity}
 * responses arising from bean-validation failures.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    /** ISO-8601 timestamp at which the error occurred. */
    @Builder.Default
    private OffsetDateTime timestamp = OffsetDateTime.now();

    /** HTTP status code (e.g., 400, 401, 404). */
    private int status;

    /** Short human-readable HTTP reason phrase (e.g., "Bad Request"). */
    private String error;

    /** Application-level error code for machine-readable categorisation. */
    private String errorCode;

    /** Human-readable error message describing the root cause. */
    private String message;

    /** Request URI path that triggered the error. */
    private String path;

    /**
     * Field-level validation errors.
     * Key: field name (or constraint path). Value: violation message.
     * Only present on validation failures ({@code 422}).
     */
    private Map<String, String> validationErrors;
}

