package com.polsl.engineering.project.rms.general.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidPaginationParamsException extends RuntimeException {

    private static final String MSG = "Page index must not be less than zero and size must be greater than zero";

    public InvalidPaginationParamsException() {
        super(MSG);
    }
}
