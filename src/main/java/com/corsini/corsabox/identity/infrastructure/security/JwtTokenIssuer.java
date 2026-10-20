package com.corsini.corsabox.identity.infrastructure.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import com.corsini.corsabox.identity.application.port.TokenIssuer;
import com.corsini.corsabox.identity.domain.User;

@Component
public class JwtTokenIssuer implements TokenIssuer {

    private final JwtEncoder encoder;

    public JwtTokenIssuer(JwtEncoder encoder) { this.encoder = encoder; }

    @Override
    public String issue(User user) {
        var now = Instant.now();
        var claims = JwtClaimsSet.builder()
                .subject(user.getId().id().toString())
                .issuedAt(now)
                .expiresAt(now.plus(1, ChronoUnit.HOURS))
                .build();
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
