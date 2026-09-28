package com.smartlb.authservice.validation.annotation;

import com.smartlb.authservice.validation.ValidationMessages;
import com.smartlb.authservice.validation.validator.OrganizationSlugValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom validation annotation for organization slug formatting rules.
 */
@Documented
@Constraint(validatedBy = OrganizationSlugValidator.class)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidOrganizationSlug {
    String message() default ValidationMessages.INVALID_ORGANIZATION_SLUG;
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
