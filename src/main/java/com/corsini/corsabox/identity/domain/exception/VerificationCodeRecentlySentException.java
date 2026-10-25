package com.corsini.corsabox.identity.domain.exception;

public class VerificationCodeRecentlySentException extends RuntimeException {
    public VerificationCodeRecentlySentException(){ 
        super("Wait before requesting another code");
    }
}