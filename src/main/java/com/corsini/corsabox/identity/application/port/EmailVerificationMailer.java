package com.corsini.corsabox.identity.application.port;

public interface EmailVerificationMailer {
    void send(String email, String firstName, String code);
}