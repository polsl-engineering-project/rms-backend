package com.polsl.engineering.project.rms.user.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class SettingAdminRoleIsNotAllowedException extends RuntimeException {
    public SettingAdminRoleIsNotAllowedException() {
        super("Adding users with ADMIN role is not allowed");
    }
}
