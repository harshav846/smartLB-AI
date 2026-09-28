package com.smartlb.authservice.validation.validator;

import com.smartlb.authservice.validation.annotation.ValidPassword;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

/**
 * Validator implementation validating password complexity requirements.
 */
public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    // Enforces minimum 8 characters, one uppercase, one lowercase, one number, and one special character.
    private static final String PASSWORD_PATTERN = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).{8,}$";
    private static final Pattern PATTERN = Pattern.compile(PASSWORD_PATTERN);

    @Override
    public void initialize(ValidPassword constraintAnnotation) {
        // No custom initialization needed
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) {
            return true; // Let @NotBlank handle null checking for composed validation
        }
        return PATTERN.matcher(password).matches();
    }
}
