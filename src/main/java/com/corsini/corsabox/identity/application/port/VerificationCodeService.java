package com.corsini.corsabox.identity.application.port;

public interface VerificationCodeService {
    String generate();
    String hash(String email, String code);
    boolean matches(String email, String code, String expectedHash);
}