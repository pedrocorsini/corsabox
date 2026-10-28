package com.corsini.corsabox.identity.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.*;

@Configuration
@ConditionalOnProperty(name = "app.google.enabled", havingValue = "true")
public class GoogleOAuthConfig {
    @Bean
    ClientRegistrationRepository googleRegistration(
            @Value("${app.google.client-id}") String clientId,
            @Value("${app.google.client-secret}") String clientSecret,
            @Value("${app.google.redirect-uri}") String redirectUri) {
        return new InMemoryClientRegistrationRepository(CommonOAuth2Provider.GOOGLE.getBuilder("google")
                .clientId(clientId).clientSecret(clientSecret).redirectUri(redirectUri)
                .scope("openid", "profile", "email").build());
    }
}
