package com.peatroxd.mtprototest.common.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ApiErrorResponse handleResponseStatusException(ResponseStatusException exception, HttpServletRequest request) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        return buildResponse(
                status,
                resolveCode(status),
                resolveMessage(exception.getReason(), status),
                request.getRequestURI(),
                List.of()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiErrorResponse handleMethodArgumentNotValid(MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<String> details = exception.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .toList();
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "Request validation failed",
                request.getRequestURI(),
                details
        );
    }

    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            HttpMessageNotReadableException.class,
            ConstraintViolationException.class,
            IllegalArgumentException.class
    })
    public ApiErrorResponse handleBadRequest(Exception exception, HttpServletRequest request) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "BAD_REQUEST",
                exception.getMessage() != null ? exception.getMessage() : "Bad request",
                request.getRequestURI(),
                List.of()
        );
    }

    @ExceptionHandler(Exception.class)
    public ApiErrorResponse handleUnexpected(Exception exception, HttpServletRequest request) {
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "Unexpected internal server error",
                request.getRequestURI(),
                List.of()
        );
    }

    private ApiErrorResponse buildResponse(
            HttpStatus status,
            String code,
            String message,
            String path,
            List<String> details
    ) {
        return new ApiErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                path,
                details
        );
    }

    private String resolveCode(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "BAD_REQUEST";
            case NOT_FOUND -> "NOT_FOUND";
            case CONFLICT -> "CONFLICT";
            case FORBIDDEN -> "FORBIDDEN";
            case TOO_MANY_REQUESTS -> "RATE_LIMITED";
            default -> "REQUEST_FAILED";
        };
    }

    private String resolveMessage(String reason, HttpStatus status) {
        if (reason != null && !reason.isBlank()) {
            return reason;
        }
        return status.getReasonPhrase();
    }

    private String formatFieldError(FieldError fieldError) {
        String message = fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "Invalid value";
        return fieldError.getField() + ": " + message;
    }
}
