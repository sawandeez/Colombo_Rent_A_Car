package com.example.backend.exception;

public record ApiFieldError(String field, Object rejectedValue, String message) {
}

