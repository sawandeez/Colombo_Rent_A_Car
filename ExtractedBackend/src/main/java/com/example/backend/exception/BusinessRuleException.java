package com.example.backend.exception;

/**
 * Simple domain exception used for validation/business rule failures.
 */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
