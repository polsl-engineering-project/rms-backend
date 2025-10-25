package com.polsl.engineering.project.rms.validation.constraint;

import com.polsl.engineering.project.rms.validation.validator.NotNullAndTrimmedLengthInRangeValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = NotNullAndTrimmedLengthInRangeValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface NotNullAndTrimmedLengthInRange {
    String message() default "must not be null and must have trimmed length within range";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    int min() default 0;
    int max() default Integer.MAX_VALUE;
}