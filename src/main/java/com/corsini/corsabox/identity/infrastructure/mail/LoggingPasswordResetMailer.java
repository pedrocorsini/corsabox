package com.corsini.corsabox.identity.infrastructure.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.corsini.corsabox.identity.application.port.PasswordResetMailer;
import com.corsini.corsabox.identity.domain.User;

@Component
@Profile("!prod")
public class LoggingPasswordResetMailer implements PasswordResetMailer {
    private static final Logger log = LoggerFactory.getLogger(LoggingPasswordResetMailer.class);

    private final String frontendUrl;

    public LoggingPasswordResetMailer(@Value("${app.frontend-url}") String frontendUrl) {
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void send(User user, String rawToken) {
        log.info("Link de redefinição para {}: {}/redefinir-senha?token={}",
                user.getEmail(), frontendUrl, rawToken);
    }
}