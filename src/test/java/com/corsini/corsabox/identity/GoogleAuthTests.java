package com.corsini.corsabox.identity;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.Instant;
import java.util.UUID;
import java.util.Date;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.*;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import com.corsini.corsabox.identity.api.GoogleLoginFlow;
import com.corsini.corsabox.identity.application.dto.GoogleIdentity;
import com.corsini.corsabox.identity.domain.*;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.*;
import com.sun.net.httpserver.HttpServer;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:google-test;MODE=PostgreSQL",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.docker.compose.enabled=false", "app.google.enabled=true",
    "app.google.client-id=test-client", "app.google.client-secret=test-secret",
    "app.frontend-url=http://localhost:5173"
})
@Import(GoogleAuthTests.LocalProviderConfig.class)
class GoogleAuthTests {
    @Autowired WebApplicationContext context;
    @Autowired FilterChainProxy security;
    @Autowired UserRepository users;
    @Autowired GoogleAccountRepository accounts;
    @Autowired PasswordEncoder passwords;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
    @Autowired LocalProvider provider;
    MockMvc mvc;
    GoogleIdentity identity;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
        identity = new GoogleIdentity(UUID.randomUUID().toString(), UUID.randomUUID() + "@example.test", "Ana", "Silva");
    }

    MockHttpSession pending(Instant expiration) {
        var session = new MockHttpSession();
        session.setAttribute(GoogleLoginFlow.ATTRIBUTE, new GoogleLoginFlow(identity, expiration));
        return session;
    }

    @Test
    void registrationIssuesExistingSessionFormatAndConsumesPendingIdentity() throws Exception {
        var session = pending(Instant.now().plusSeconds(600));
        var username = "u" + UUID.randomUUID().toString().substring(0, 12).replace("-", "_");
        mvc.perform(get("/api/auth/google/pending").session(session))
            .andExpect(status().isOk()).andExpect(jsonPath("$.step").value("register"));
        mvc.perform(post("/api/auth/google/complete").session(session).header("Origin", "http://localhost:5173")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"firstName\":\"Ana\",\"lastName\":\"Silva\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("corsabox_refresh=")));
        assertTrue(session.isInvalid());
        var user = users.findByEmail(identity.email()).orElseThrow();
        assertNull(user.getPasswordHash());
        assertFalse(com.corsini.corsabox.identity.api.response.UserResponse.from(user).hasPassword());
        assertEquals(user.getId(), accounts.findUserId(identity.subject()).orElseThrow());
        mvc.perform(get("/api/auth/google/pending")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void linkingRequiresCurrentPasswordAndPreservesUser() throws Exception {
        var user = users.save(User.register(identity.email(), "u" + UUID.randomUUID().toString().substring(0, 8),
                "Existing", "User", passwords.encode("correct-password")));
        var session = pending(Instant.now().plusSeconds(600));
        mvc.perform(get("/api/auth/google/pending").session(session))
            .andExpect(jsonPath("$.step").value("link"));
        mvc.perform(post("/api/auth/google/complete").session(session).header("Origin", "http://localhost:5173")
                .contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"wrong\"}"))
            .andExpect(status().isUnauthorized());
        assertTrue(accounts.findUserId(identity.subject()).isEmpty());
        mvc.perform(post("/api/auth/google/complete").session(session).header("Origin", "http://localhost:5173")
                .contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"correct-password\"}"))
            .andExpect(status().isOk());
        assertEquals(user.getId(), accounts.findUserId(identity.subject()).orElseThrow());
        assertEquals("Existing", users.findById(user.getId()).orElseThrow().getFirstName());
        assertTrue(com.corsini.corsabox.identity.api.response.UserResponse.from(user).hasPassword());
    }

    @Test
    void expiredMissingAndCrossOriginCompletionsAreRejected() throws Exception {
        mvc.perform(post("/api/auth/google/complete").header("Origin", "http://localhost:5173")
            .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/google/complete").session(pending(Instant.now().minusSeconds(1)))
            .header("Origin", "http://localhost:5173").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/google/complete").session(pending(Instant.now().plusSeconds(600)))
            .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/google/complete").header("Origin", "https://attacker.example")
            .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
        assertTrue(users.findByEmail(identity.email()).isEmpty());
    }

    @Test
    void oauthStartHasStateAndNonceAndUnsolicitedCallbackFails() throws Exception {
        mvc.perform(get("/oauth2/authorization/google"))
            .andExpect(status().is3xxRedirection())
            .andExpect(header().string("Location", org.hamcrest.Matchers.allOf(
                org.hamcrest.Matchers.containsString("https://accounts.google.com/"),
                org.hamcrest.Matchers.containsString("state="), org.hamcrest.Matchers.containsString("nonce="))));
        mvc.perform(get("/login/oauth2/code/google").param("code", "fake").param("state", "fake"))
            .andExpect(redirectedUrl("http://localhost:5173/auth/google?error=google"));
    }

    @Test
    void databaseNeverReassignsExistingGoogleSubject() {
        var first = users.save(User.register(identity.email(), "u" + UUID.randomUUID().toString().substring(0, 8), "A", "B", null));
        var second = users.save(User.register(UUID.randomUUID() + "@example.test", "u" + UUID.randomUUID().toString().substring(0, 8), "C", "D", null));
        var tx = new TransactionTemplate(transactions);
        tx.executeWithoutResult(s -> accounts.link(identity.subject(), first.getId()));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
            () -> tx.executeWithoutResult(s -> accounts.link(identity.subject(), second.getId())));
        assertEquals(first.getId(), accounts.findUserId(identity.subject()).orElseThrow());
    }

    @Test
    void validOidcCallbackCreatesPendingIdentityButDoesNotAuthenticateApi() throws Exception {
        var session = callback(true, false);
        mvc.perform(get("/api/auth/google/pending").session(session))
            .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(identity.email()));
        mvc.perform(get("/api/users/me").session(session)).andExpect(status().isUnauthorized());
        assertNull(session.getAttribute("SPRING_SECURITY_CONTEXT"));
    }

    @Test
    void unverifiedEmailAndWrongNonceCannotCreatePendingIdentity() throws Exception {
        assertTrue(callback(false, false).isInvalid());
        assertTrue(callback(true, true).isInvalid());
        assertTrue(users.findByEmail(identity.email()).isEmpty());
    }

    MockHttpSession callback(boolean verified, boolean wrongNonce) throws Exception {
        var start = mvc.perform(get("/oauth2/authorization/google")).andReturn();
        var session = (MockHttpSession) start.getRequest().getSession(false);
        var query = org.springframework.web.util.UriComponentsBuilder
            .fromUriString(start.getResponse().getRedirectedUrl()).build().getQueryParams();
        var nonce = URLDecoder.decode(query.getFirst("nonce"), StandardCharsets.UTF_8);
        var state = URLDecoder.decode(query.getFirst("state"), StandardCharsets.UTF_8);
        provider.identity = identity;
        var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("local-key").build(),
            new JWTClaimsSet.Builder().issuer("https://accounts.google.com").subject(identity.subject())
                .audience("test-client").issueTime(new Date()).expirationTime(Date.from(Instant.now().plusSeconds(300)))
                .claim("nonce", wrongNonce ? "wrong" : nonce).claim("email", identity.email())
                .claim("email_verified", verified).claim("given_name", "Ana").claim("family_name", "Silva").build());
        jwt.sign(new RSASSASigner(provider.key));
        provider.token = jwt.serialize();
        mvc.perform(get("/login/oauth2/code/google").session(session).param("code", "local-code").param("state", state))
            .andExpect(redirectedUrl("http://localhost:5173/auth/google" + (verified && !wrongNonce ? "" : "?error=google")));
        return session;
    }

    @TestConfiguration
    static class LocalProviderConfig {
        @Bean(destroyMethod = "close")
        LocalProvider localProvider() throws Exception { return new LocalProvider(); }

        @Bean
        @Primary
        ClientRegistrationRepository localRegistration(LocalProvider provider) {
            return new InMemoryClientRegistrationRepository(CommonOAuth2Provider.GOOGLE.getBuilder("google")
                .clientId("test-client").clientSecret("test-secret")
                .tokenUri(provider.url("/token")).jwkSetUri(provider.url("/jwks"))
                .userInfoUri(provider.url("/userinfo"))
                .redirectUri("http://localhost:8080/login/oauth2/code/google").build());
        }
    }

    /** A signed local OIDC provider; no Google calls or real credentials. */
    static class LocalProvider implements AutoCloseable {
        final RSAKey key = new RSAKeyGenerator(2048).keyID("local-key").generate();
        final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        volatile String token;
        volatile GoogleIdentity identity;

        LocalProvider() throws Exception {
            server.createContext("/jwks", exchange -> respond(exchange, new JWKSet(key.toPublicJWK()).toString()));
            server.createContext("/token", exchange -> {
                exchange.getRequestBody().readAllBytes();
                respond(exchange, "{\"access_token\":\"local-access\",\"token_type\":\"Bearer\",\"expires_in\":300,\"id_token\":\"" + token + "\"}");
            });
            server.createContext("/userinfo", exchange -> respond(exchange,
                "{\"sub\":\"" + identity.subject() + "\",\"email\":\"" + identity.email() + "\"}"));
            server.start();
        }

        String url(String path) { return "http://127.0.0.1:" + server.getAddress().getPort() + path; }
        static void respond(com.sun.net.httpserver.HttpExchange exchange, String json) throws java.io.IOException {
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (var body = exchange.getResponseBody()) { body.write(bytes); }
        }
        public void close() { server.stop(0); }
    }
}
