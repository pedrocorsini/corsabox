package com.corsini.corsabox.config;

import java.util.List;
import java.time.Instant;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.context.NullSecurityContextRepository;
import com.corsini.corsabox.identity.api.GoogleLoginFlow;
import com.corsini.corsabox.identity.application.dto.GoogleIdentity;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http,
            ObjectProvider<ClientRegistrationRepository> registrations,
            @Value("${app.frontend-url}") String frontendUrl) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .securityContext(s -> s.securityContextRepository(new NullSecurityContextRepository()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/api/users").denyAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/oauth2/authorization/google", "/login/oauth2/code/google").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/auth/google/config", "/api/auth/google/pending").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/google/complete").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register", "/api/auth/register/confirm",
                                "/api/auth/forgot-password", "/api/auth/reset-password",
                                "/api/auth/refresh", "/api/auth/logout").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/catalog/trending", "/api/catalog/now-playing").permitAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html",
                                "/v3/api-docs/**", "/actuator/health").permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()));
        if (registrations.getIfAvailable() != null) {
            String destination = frontendUrl.replaceAll("/+$", "") + "/auth/google";
            http.oauth2Login(oauth -> oauth
                .successHandler((request, response, authentication) -> {
                    var principal = (OidcUser) authentication.getPrincipal();
                    var session = request.getSession();
                    if (!Boolean.TRUE.equals(principal.getEmailVerified())) {
                        session.invalidate();
                        response.sendRedirect(destination + "?error=google");
                        return;
                    }
                    request.changeSessionId();
                    session.setMaxInactiveInterval(600);
                    session.setAttribute(GoogleLoginFlow.ATTRIBUTE, new GoogleLoginFlow(
                        new GoogleIdentity(principal.getSubject(), principal.getEmail(),
                            principal.getGivenName(), principal.getFamilyName()), Instant.now().plusSeconds(600)));
                    response.sendRedirect(destination);
                })
                .failureHandler((request, response, exception) -> {
                    var session = request.getSession(false);
                    if (session != null) session.invalidate();
                    response.sendRedirect(destination + "?error=google");
                }));
        }
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
        var config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
