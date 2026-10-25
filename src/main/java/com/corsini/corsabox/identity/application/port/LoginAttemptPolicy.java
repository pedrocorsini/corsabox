package com.corsini.corsabox.identity.application.port;

public interface LoginAttemptPolicy {
    void checkAllowed(String email);
    void recordFailure(String email);
    void reset(String email);
}