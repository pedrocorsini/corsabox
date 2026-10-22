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

import com.corsini.corsabox.identity.application.port.PasswordResetMailer;
import com.corsini.corsabox.identity.domain.User;

@Component
@Profile("prod")
public class BrevoPasswordResetMailer implements PasswordResetMailer {
    private static final Logger log = LoggerFactory.getLogger(BrevoPasswordResetMailer.class);

    private final RestClient http;
    private final String frontendUrl;
    private final String senderEmail;
    private final String senderName;

    public BrevoPasswordResetMailer(@Value("${app.mail.api-key}") String apiKey,
                                    @Value("${app.frontend-url}") String frontendUrl,
                                    @Value("${app.mail.sender-email}") String senderEmail,
                                    @Value("${app.mail.sender-name}") String senderName) {
        this.http = RestClient.builder()
                .baseUrl("https://api.brevo.com/v3")
                .defaultHeader("api-key", apiKey)
                .build();
        this.frontendUrl = frontendUrl;
        this.senderEmail = senderEmail;
        this.senderName = senderName;
    }

    @Async
    @Override
    public void send(User user, String rawToken) {
        var link = frontendUrl + "/redefinir-senha?token=" + rawToken;
        var html = """
            <p>Olá, %s.</p>
            <p>Recebemos um pedido para redefinir a sua senha da CorsaBox.
               O link vale por 30 minutos e só pode ser usado uma vez:</p>
            <p><a href="%s">Redefinir senha</a></p>
            <p>Se não foi você, ignore este email.</p>
            """.formatted(user.getFirstName(), link);

        try {
            http.post().uri("/smtp/email")
                .body(Map.of(
                    "sender", Map.of("name", senderName, "email", senderEmail),
                    "to", List.of(Map.of("email", user.getEmail())),
                    "subject", "Redefinição de senha da CorsaBox",
                    "htmlContent", html))
                .retrieve()
                .toBodilessEntity();
        } catch (Exception e) {
            // Nunca propaga: o endpoint responde 202 igual, exista o email ou não.
            log.error("Falha ao enviar email de reset para o usuário {}", user.getId().id(), e);
        }
    }
}