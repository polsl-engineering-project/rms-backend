package com.polsl.engineering.project.rms.order.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidOrderActionException extends RuntimeException {
    public InvalidOrderActionException(String message) {
        super(message);
    }
}