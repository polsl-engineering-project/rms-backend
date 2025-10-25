package com.polsl.engineering.project.rms.user.dto;

import com.polsl.engineering.project.rms.validation.constraint.NotNullAndTrimmedLengthInRange;

import static com.polsl.engineering.project.rms.user.User.PASSWORD_MAX_LENGTH;
import static com.polsl.engineering.project.rms.user.User.PASSWORD_MIN_LENGTH;

public record ChangeUserPasswordRequest(
        @NotNullAndTrimmedLengthInRange(
                min = PASSWORD_MIN_LENGTH,
                max = PASSWORD_MAX_LENGTH,
                message = "must not be null and must have trimmed length between {min} and {max} characters"
        )
        String password
) {
}
