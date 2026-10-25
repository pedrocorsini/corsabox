package com.corsini.corsabox.identity.infrastructure.mail;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.corsini.corsabox.identity.application.port.EmailVerificationMailer;


@Component
@Profile("prod")
public class BrevoEmailVerificationMailer implements EmailVerificationMailer {
    private static final Logger log = LoggerFactory.getLogger(BrevoEmailVerificationMailer.class);

    private final RestClient http;
    private final String senderEmail;
    private final String senderName;

    public BrevoEmailVerificationMailer(@Value("${app.mail.api-key}") String apiKey,
                                        @Value("${app.mail.sender-email}") String senderEmail,
                                        @Value("${app.mail.sender-name}") String senderName) {
        this.http = RestClient.builder()
                .baseUrl("https://api.brevo.com/v3")
                .defaultHeader("api-key", apiKey)
                .build();
        this.senderEmail = senderEmail;
        this.senderName = senderName;
    }

    @Async
    @Override
    public void send(String email, String firstName, String code) {
        var html = """
            <p>Olá, %s.</p>
            <p>Seu código para criar a conta na CorsaBox é:</p>
            <p style="font-size:28px;font-weight:bold;letter-spacing:6px">%s</p>
            <p>Ele vale por 10 minutos. Se não foi você, ignore este email.</p>
            """.formatted(firstName, code);
        try {
            http.post().uri("/smtp/email")
                .body(Map.of(
                    "sender", Map.of("name", senderName, "email", senderEmail),
                    "to", List.of(Map.of("email", email)),
                    "subject", "Seu código da CorsaBox: " + code,
                    "htmlContent", html))
                .retrieve()
                .toBodilessEntity();
        } catch (Exception e) {
            log.error("Falha ao enviar código de verificação", e);
        }
    }
}