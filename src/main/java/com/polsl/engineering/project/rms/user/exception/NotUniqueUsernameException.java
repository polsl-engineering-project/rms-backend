package com.polsl.engineering.project.rms.user.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class NotUniqueUsernameException extends RuntimeException {

    private static final String MSG_TEMPLATE = "Username %s is already taken";

    public NotUniqueUsernameException(String username) {
        super(String.format(MSG_TEMPLATE, username));
    }

}
