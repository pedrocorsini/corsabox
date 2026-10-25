package com.corsini.corsabox.identity.domain.exception;

public class InvalidVerificationCodeException extends RuntimeException {
    public InvalidVerificationCodeException() { 
        super("Invalid or expired verification code");
    }
}