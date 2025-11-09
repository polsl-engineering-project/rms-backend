package com.polsl.engineering.project.rms.order.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class OrderOutboxPersistenceException extends RuntimeException {
    public OrderOutboxPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}

