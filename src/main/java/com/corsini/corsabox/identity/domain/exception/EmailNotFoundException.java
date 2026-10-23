package com.corsini.corsabox.identity.domain.exception;

public class EmailNotFoundException extends RuntimeException {
    public EmailNotFoundException() {
        super("Não existe conta com este email");
    }
}