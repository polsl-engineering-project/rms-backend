package com.polsl.engineering.project.rms.menu.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class NotUniqueMenuNameException extends RuntimeException {

    private static final String MSG_TEMPLATE = "Name %s in %s is not unique.";

    public NotUniqueMenuNameException(String place, String username) {
        super(String.format(MSG_TEMPLATE, username, place));
    }

}
