package com.corsini.corsabox.library.domain.exception;

public class AlreadyWatchedException extends RuntimeException {
    public AlreadyWatchedException() {
        super("Title is already marked as watched");
    }
}