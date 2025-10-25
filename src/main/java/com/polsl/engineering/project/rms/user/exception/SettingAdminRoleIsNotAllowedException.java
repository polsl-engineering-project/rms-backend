package com.polsl.engineering.project.rms.user.exception;

import com.polsl.engineering.project.rms.user.Role;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class SettingAdminRoleIsNotAllowedException extends RuntimeException {

    public static final String MSG_TEMPLATE = "Adding users with %s role is not allowed";

    public SettingAdminRoleIsNotAllowedException() {
        super(MSG_TEMPLATE.formatted(Role.ADMIN));
    }
}
