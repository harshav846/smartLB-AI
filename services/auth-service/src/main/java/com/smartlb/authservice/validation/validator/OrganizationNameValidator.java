package com.smartlb.authservice.validation.validator;

import com.smartlb.authservice.validation.annotation.ValidOrganizationName;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

/**
 * Validator implementation validating organization name formatting rules.
 */
public class OrganizationNameValidator implements ConstraintValidator<ValidOrganizationName, String> {

    // Allow alphanumeric characters, spaces, hyphens, or underscores, length 3 to 100, starting with alphanumeric character.
    private static final String NAME_PATTERN = "^[a-zA-Z0-9][a-zA-Z0-9\\s-_]{2,99}$";
    private static final Pattern PATTERN = Pattern.compile(NAME_PATTERN);

    @Override
    public void initialize(ValidOrganizationName constraintAnnotation) {
        // No custom initialization needed
    }

    @Override
    public boolean isValid(String name, ConstraintValidatorContext context) {
        if (name == null) {
            return true; // Let @NotBlank handle null checking for composed validation
        }
        return PATTERN.matcher(name).matches();
    }
}
