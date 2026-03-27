package com.example.backend.exception;

import java.util.List;

public class RequestValidationException extends RuntimeException {

    private final List<ApiFieldError> errors;

    public RequestValidationException(String message, List<ApiFieldError> errors) {
        super(message);
        this.errors = errors;
    }

    public RequestValidationException(String message, ApiFieldError error) {
        this(message, List.of(error));
    }

    public List<ApiFieldError> getErrors() {
        return errors;
    }
}

