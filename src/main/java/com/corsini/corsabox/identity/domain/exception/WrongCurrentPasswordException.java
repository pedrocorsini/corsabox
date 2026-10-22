package com.corsini.corsabox.identity.domain.exception;

public class WrongCurrentPasswordException extends RuntimeException {
    public WrongCurrentPasswordException() {
        super("Current password is incorrect");
    }
}