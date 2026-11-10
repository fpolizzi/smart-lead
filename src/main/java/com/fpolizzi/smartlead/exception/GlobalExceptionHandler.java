package com.fpolizzi.smartlead.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableBody(HttpMessageNotReadableException e) {
        return problem(HttpStatus.BAD_REQUEST, "Request body is missing or malformed");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return problem(HttpStatus.BAD_REQUEST, "Validation failed: " + detail);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid value for parameter '" + e.getName() + "'");
    }

    @ExceptionHandler(LeadNotFoundException.class)
    public ProblemDetail handleLeadNotFound(LeadNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResource(NoResourceFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "Resource not found");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ProblemDetail handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return problem(HttpStatus.METHOD_NOT_ALLOWED, "Method " + e.getMethod() + " is not supported for this resource");
    }

    @ExceptionHandler(AiUnavailableException.class)
    public ProblemDetail handleAiUnavailable(AiUnavailableException e) {
        log.warn("AI providers unavailable", e);
        return problem(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
    }

    @ExceptionHandler(AiResponseParseException.class)
    public ProblemDetail handleAiResponseParse(AiResponseParseException e) {
        log.error("Unparseable AI response", e);
        return problem(HttpStatus.BAD_GATEWAY, "AI provider returned an invalid response");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception e) {
        log.error("Unhandled exception", e);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    private ProblemDetail problem(HttpStatus status, String detail) {
        return ProblemDetail.forStatusAndDetail(status, detail);
    }
}
