package com.polsl.engineering.project.rms.bill.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class BillOutboxPersistenceException extends RuntimeException {
    public BillOutboxPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}