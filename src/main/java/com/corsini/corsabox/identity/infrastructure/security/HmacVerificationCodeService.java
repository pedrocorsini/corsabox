package com.corsini.corsabox.identity.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.corsini.corsabox.identity.application.port.VerificationCodeService;

@Component
public class HmacVerificationCodeService implements VerificationCodeService {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SecretKeySpec key;

    public HmacVerificationCodeService(@Value("${app.verification.secret}") String secret) {
        this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Override
    public String generate() {
        return "%06d".formatted(RANDOM.nextInt(1_000_000));
    }

    @Override
    public String hash(String email, String code) {
        try {
            // Mac não é thread-safe, então uma instância por chamada.
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            return HexFormat.of().formatHex(mac.doFinal((email + ":" + code).getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
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