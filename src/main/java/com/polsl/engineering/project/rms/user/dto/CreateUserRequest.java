package com.polsl.engineering.project.rms.user.dto;

import com.polsl.engineering.project.rms.user.Role;
import com.polsl.engineering.project.rms.validation.constraint.NotNullAndTrimmedLengthInRange;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import static com.polsl.engineering.project.rms.user.UserConstraints.*;

@Builder(toBuilder = true)
public record CreateUserRequest(
        @NotNullAndTrimmedLengthInRange(
                min = USERNAME_MIN_LENGTH,
                max = USERNAME_MAX_LENGTH,
                message = "must not be null and must have trimmed length between {min} and {max} characters"
        )
        String username,

        @NotNullAndTrimmedLengthInRange(
                min = PASSWORD_MIN_LENGTH,
                max = PASSWORD_MAX_LENGTH,
                message = "must not be null and must have trimmed length between {min} and {max} characters"
        )
        String password,

        @NotNullAndTrimmedLengthInRange(
                min = FIRST_NAME_MIN_LENGTH,
                max = FIRST_NAME_MAX_LENGTH,
                message = "must not be null and must have trimmed length between {min} and {max} characters"
        )
        String firstName,

        @NotNullAndTrimmedLengthInRange(
                min = LAST_NAME_MIN_LENGTH,
                max = LAST_NAME_MAX_LENGTH,
                message = "must not be null and must have trimmed length between {min} and {max} characters"
        )
        String lastName,

        @NotNullAndTrimmedLengthInRange(
                min = PHONE_NUMBER_MIN_LENGTH,
                max = PHONE_NUMBER_MAX_LENGTH,
                message = "must not be null and must have trimmed length between {min} and {max} characters"
        )
        String phoneNumber,

        @NotNull(message = "must not be null")
        Role role
) {
}
