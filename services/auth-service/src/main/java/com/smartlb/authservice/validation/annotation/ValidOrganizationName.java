package com.smartlb.authservice.validation.annotation;

import com.smartlb.authservice.validation.ValidationMessages;
import com.smartlb.authservice.validation.validator.OrganizationNameValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom validation annotation for organization name formatting rules.
 */
@Documented
@Constraint(validatedBy = OrganizationNameValidator.class)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidOrganizationName {
    String message() default ValidationMessages.INVALID_ORGANIZATION_NAME;
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
