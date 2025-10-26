package com.polsl.engineering.project.rms.common.error_handler;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        Map<String, String> details
) {

    public static ErrorResponse of(int status, String error, String message, Map<String, String> details) {
        return new ErrorResponse(Instant.now(), status, error, message, details);
    }

    public static ErrorResponse of(int status, String error, String message) {
        return new ErrorResponse(Instant.now(), status, error, message, Collections.emptyMap());
    }
}
