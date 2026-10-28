package com.corsini.corsabox.identity.infrastructure.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.corsini.corsabox.identity.application.port.EmailVerificationMailer;

@Component
@Profile("!prod")
public class LoggingEmailVerificationMailer implements EmailVerificationMailer {
    private static final Logger log = LoggerFactory.getLogger(LoggingEmailVerificationMailer.class);

    @Override
    public void send(String email, String firstName, String code) {
        log.info("Development mail delivery skipped; configure a local mail provider to receive messages");
    }
}