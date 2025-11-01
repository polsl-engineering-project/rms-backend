package com.polsl.engineering.project.rms.menu.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class UuidCorruptionException extends IllegalArgumentException {
    public UuidCorruptionException() {
        super("Mismatch between path UUID and request body UUID");
    }

}
