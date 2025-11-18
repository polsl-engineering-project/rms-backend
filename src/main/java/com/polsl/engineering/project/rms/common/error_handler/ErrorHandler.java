package com.polsl.engineering.project.rms.common.error_handler;

import com.polsl.engineering.project.rms.bill.exception.InvalidBillActionException;
import com.polsl.engineering.project.rms.bill.exception.MenuItemVersionMismatchException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(InvalidBillActionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidBillAction(InvalidBillActionException ex) {
        var response = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "Invalid bill action",
                ex.getMessage()
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(MenuItemVersionMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMenuItemVersionMismatchException(MenuItemVersionMismatchException ex) {
        var response = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "Item version mismatch",
                ex.getMessage()
        );

        return ResponseEntity.badRequest().body(response);
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        var errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fieldError -> Optional.ofNullable(fieldError.getDefaultMessage()).orElse(""),
                        (msg1, msg2) -> msg1
                ));

        var response = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                "Validation error occurred",
                errors
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        var errors = ex.getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(
                        v -> v.getPropertyPath().toString(),
                        ConstraintViolation::getMessage,
                        (msg1, msg2) -> msg1
                ));

        var response = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "Constraint violation",
                "Validation error occurred",
                errors
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        var response = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "Malformed JSON",
                "Request body is improperly formatted"
        );

        return ResponseEntity.badRequest().body(response);
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllExceptions(Exception ex) {
        var responseStatus = Optional.ofNullable(
                ex.getClass().getAnnotation(ResponseStatus.class)
        );

        log.error(ex.getMessage(), ex);

        var status = responseStatus
                .map(ResponseStatus::value)
                .orElse(HttpStatus.INTERNAL_SERVER_ERROR);

        var message = responseStatus.isPresent()
                ? ex.getMessage()
                : "Wystąpił nieoczekiwany błąd serwera.";
        // jeśli wyjątek ma @ResponseStatus (nasz wyjątek) to wyświetlamy naszą wiadomość, jeśli wyjątek jest od Springa to wyświetlamy bezpieczną wiadomosć

        var response = ErrorResponse.of(
                status.value(),
                status.getReasonPhrase(),
                message
        );

        return ResponseEntity.status(status).body(response);
    }
}
