package com.smartlb.authservice.validation;

/**
 * Centrally defined validation messages constants class.
 */
public final class ValidationMessages {

    private ValidationMessages() {
        // Prevent instantiation
    }

    public static final String INVALID_PASSWORD = 
        "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character, and be at least 8 characters long.";

    public static final String INVALID_ORGANIZATION_SLUG = 
        "Slug must be lowercase alphanumeric with hyphens only.";

    public static final String INVALID_ORGANIZATION_NAME = 
        "Organization name must start with an alphanumeric character, contain only alphanumeric characters, spaces, hyphens or underscores, and be between 3 and 100 characters.";
}
