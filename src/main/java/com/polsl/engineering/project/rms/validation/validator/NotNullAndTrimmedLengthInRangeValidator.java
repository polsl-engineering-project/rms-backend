package com.polsl.engineering.project.rms.validation.validator;

import com.polsl.engineering.project.rms.validation.constraint.NotNullAndTrimmedLengthInRange;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NotNullAndTrimmedLengthInRangeValidator implements ConstraintValidator<NotNullAndTrimmedLengthInRange, String> {
    private int min;
    private int max;

    @Override
    public void initialize(NotNullAndTrimmedLengthInRange constraintAnnotation) {
        this.min = constraintAnnotation.min();
        this.max = constraintAnnotation.max();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return false;

        var trimmed = value.trim();
        var length = trimmed.length();

        return length >= min && length <= max;
    }
}