package com.corsini.corsabox.identity.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

import com.corsini.corsabox.identity.application.port.VerificationCodeService;

@Component
public class Sha256VerificationCodeService implements VerificationCodeService {
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public String generate() {
        return "%06d".formatted(RANDOM.nextInt(1_000_000));
    }

    @Override
    public String hash(String email, String code) {
        try {
            var digest = MessageDigest.getInstance("SHA-256")
                    .digest((email + ":" + code).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean matches(String email, String code, String expectedHash) {
        return MessageDigest.isEqual(
                hash(email, code).getBytes(StandardCharsets.UTF_8),
                expectedHash.getBytes(StandardCharsets.UTF_8));
    }
}