package com.polsl.engineering.project.rms.general.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidUUIDFormatException extends RuntimeException {

    private static final String MSG = "provided %s is not valid UUID string";

    public InvalidUUIDFormatException(String invalidUuidString) {
        super(String.format(MSG, invalidUuidString));
    }

}
