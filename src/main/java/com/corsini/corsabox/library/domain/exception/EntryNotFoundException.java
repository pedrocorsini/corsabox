package com.corsini.corsabox.library.domain.exception;

public class EntryNotFoundException extends RuntimeException {
    public EntryNotFoundException() {
        super("Title not found in your library");
    }
}