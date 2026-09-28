package com.smartlb.authservice.validation.validator;

import com.smartlb.authservice.validation.annotation.ValidOrganizationSlug;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

/**
 * Validator implementation validating organization slug formatting rules.
 */
public class OrganizationSlugValidator implements ConstraintValidator<ValidOrganizationSlug, String> {

    // Enforces that slug contains only lowercase alphanumeric characters and hyphens.
    private static final String SLUG_PATTERN = "^[a-z0-9]+(?:-[a-z0-9]+)*$";
    private static final Pattern PATTERN = Pattern.compile(SLUG_PATTERN);

    @Override
    public void initialize(ValidOrganizationSlug constraintAnnotation) {
        // No custom initialization needed
    }

    @Override
    public boolean isValid(String slug, ConstraintValidatorContext context) {
        if (slug == null) {
            return true; // Let @NotBlank handle null checking for composed validation
        }
        return PATTERN.matcher(slug).matches();
    }
}
