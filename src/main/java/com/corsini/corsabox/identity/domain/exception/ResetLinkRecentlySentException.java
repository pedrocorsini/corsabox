package com.corsini.corsabox.identity.domain.exception;

public class ResetLinkRecentlySentException extends RuntimeException {
    public ResetLinkRecentlySentException() {
        super("Wait before requesting another reset link");
    }
}