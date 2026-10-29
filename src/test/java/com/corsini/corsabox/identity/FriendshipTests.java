package com.corsini.corsabox.identity;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import com.corsini.corsabox.identity.application.service.PeopleService;
import com.corsini.corsabox.identity.domain.*;
import com.corsini.corsabox.identity.domain.Friendship.Relationship;
import com.corsini.corsabox.identity.domain.Friendship.RequestDirection;
import com.corsini.corsabox.identity.domain.exception.FriendshipException;
import com.corsini.corsabox.identity.infrastructure.persistence.UserEntity;
import com.corsini.corsabox.catalog.domain.MediaFormat;
import com.corsini.corsabox.catalog.domain.MediaKind;
import com.corsini.corsabox.library.domain.*;
import jakarta.persistence.EntityManager;

@SpringBootTest(properties = {
    "spring.datasource.url=${FRIENDSHIP_TEST_URL:jdbc:h2:mem:friendships-test;MODE=PostgreSQL}",
    "spring.datasource.username=${FRIENDSHIP_TEST_USER:sa}", "spring.datasource.password=${FRIENDSHIP_TEST_PASSWORD:}",
    "spring.docker.compose.enabled=false", "app.rate-limit.enabled=false"
})
@Transactional
class FriendshipTests {
    @Autowired WebApplicationContext context;
    @Autowired UserRepository users;
    @Autowired PeopleService people;
    @Autowired FriendshipRepository friendships;
    @Autowired LibraryRepository library;
    @Autowired EntityManager em;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
    MockMvc mvc;
    User alice;
    User bob;
    User carol;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        alice = user("Ana", "Silva"); bob = user("Bruno", "Costa"); carol = user("Carla", "Souza");
    }

    User user(String first, String last) {
        var suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return users.save(User.register(suffix + "@private-email.test", "u_" + suffix, first, last, "private-password-hash"));
    }
    JwtRequestPostProcessor as(User user) { return jwt().jwt(token -> token.subject(user.getId().id().toString())); }
    String friend(User target) { return "/api/friends/" + target.getId().id(); }
    String profile(User target) { return "/api/users/" + target.getId().id() + "/profile"; }

    @Test
    void requestAcceptAndRemoveAreSymmetricAndIdempotent() throws Exception {
        for (int i = 0; i < 2; i++) mvc.perform(post(friend(bob)).with(as(alice)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.relationship").value("REQUEST_SENT"));
        mvc.perform(get(profile(alice)).with(as(bob))).andExpect(jsonPath("$.relationship").value("REQUEST_RECEIVED"));
        mvc.perform(get("/api/friends/requests").with(as(bob))).andExpect(jsonPath("$.content[0].user.id").value(alice.getId().id().toString()));
        mvc.perform(get("/api/friends/requests?direction=SENT").with(as(alice))).andExpect(jsonPath("$.content[0].user.id").value(bob.getId().id().toString()));
        mvc.perform(post(friend(bob) + "/accept").with(as(alice))).andExpect(status().isForbidden());
        mvc.perform(post(friend(alice) + "/accept").with(as(carol))).andExpect(status().isNotFound());
        for (int i = 0; i < 2; i++) mvc.perform(post(friend(alice) + "/accept").with(as(bob)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.relationship").value("FRIENDS"));
        mvc.perform(get("/api/friends").with(as(alice))).andExpect(jsonPath("$.content[0].user.id").value(bob.getId().id().toString()))
                .andExpect(jsonPath("$.content[0].relationship").value("FRIENDS"));
        mvc.perform(get("/api/friends").with(as(bob))).andExpect(jsonPath("$.content[0].user.id").value(alice.getId().id().toString()));
        mvc.perform(get("/api/friends/requests").with(as(bob))).andExpect(jsonPath("$.content").isEmpty());
        mvc.perform(delete(friend(bob)).with(as(alice))).andExpect(jsonPath("$.relationship").value("NONE"));
        mvc.perform(get("/api/friends").with(as(bob))).andExpect(jsonPath("$.content").isEmpty());
        mvc.perform(post(friend(alice)).with(as(bob))).andExpect(jsonPath("$.relationship").value("REQUEST_SENT"));
    }

    @Test
    void requestsCanBeDeclinedOrCancelledWithoutChangingOtherRelationships() throws Exception {
        people.request(alice.getId().id(), bob.getId().id());
        people.request(alice.getId().id(), carol.getId().id());
        mvc.perform(delete(friend(alice)).with(as(bob))).andExpect(status().isOk());
        mvc.perform(post(friend(alice) + "/accept").with(as(bob))).andExpect(status().isNotFound());
        assertEquals(Relationship.REQUEST_RECEIVED, people.profile(carol.getId().id(), alice.getId().id()).relationship());
        mvc.perform(delete(friend(carol)).with(as(alice))).andExpect(status().isOk());
        mvc.perform(get("/api/friends/requests?direction=SENT").with(as(alice))).andExpect(jsonPath("$.content").isEmpty());
        mvc.perform(delete(friend(carol)).with(as(alice))).andExpect(status().isOk());
    }

    @Test
    void crossingRequestsNeedExplicitAcceptanceAndSelfRequestsAreRejected() throws Exception {
        mvc.perform(post(friend(bob)).with(as(alice))).andExpect(status().isOk());
        mvc.perform(post(friend(alice)).with(as(bob))).andExpect(status().isConflict());
        assertEquals(Relationship.REQUEST_SENT, people.profile(alice.getId().id(), bob.getId().id()).relationship());
        mvc.perform(post(friend(alice)).with(as(alice))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/friends/" + UUID.randomUUID()).with(as(alice))).andExpect(status().isNotFound());
        mvc.perform(get("/api/friends/requests?direction=INVALID").with(as(alice))).andExpect(status().isBadRequest());
    }

    @Test
    void searchSupportsFullNameAndUsernameWithoutExposingPrivateFields() throws Exception {
        mvc.perform(get("/api/users/search").param("q", "BRUNO COSTA").with(as(alice)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].user.username").value(bob.getUsername()))
                .andExpect(jsonPath("$.content[0].user.email").doesNotExist())
                .andExpect(jsonPath("$.content[0].user.passwordHash").doesNotExist());
        mvc.perform(get("/api/users/search").param("q", "@" + bob.getUsername()).with(as(alice)))
                .andExpect(jsonPath("$.content[0].user.id").value(bob.getId().id().toString()));
        mvc.perform(get("/api/users/search").param("q", alice.getUsername()).with(as(alice))).andExpect(jsonPath("$.content").isEmpty());
        mvc.perform(get("/api/users/search").param("q", "private-email").with(as(alice))).andExpect(jsonPath("$.content").isEmpty());
        for (var q : List.of("", "a", "%_")) mvc.perform(get("/api/users/search").param("q", q).with(as(alice)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty());
        mvc.perform(get("/api/users/search").param("q", "a".repeat(101)).with(as(alice))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/users").with(as(alice))).andExpect(status().isForbidden());
    }

    @Test
    void profileContainsOnlyPublicFieldsAndRequiresAuthentication() throws Exception {
        mvc.perform(get(profile(bob)).with(as(alice))).andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value(bob.getUsername()))
                .andExpect(jsonPath("$.user.firstName").value("Bruno"))
                .andExpect(jsonPath("$.user.email").doesNotExist()).andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.user.hasPassword").doesNotExist()).andExpect(jsonPath("$.relationship").value("NONE"));
        mvc.perform(get(profile(alice)).with(as(alice))).andExpect(jsonPath("$.relationship").value("SELF"));
        mvc.perform(get("/api/users/" + UUID.randomUUID() + "/profile").with(as(alice))).andExpect(status().isNotFound());
        mvc.perform(get(profile(bob))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/users/search?q=bruno")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/friends")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/friends/requests")).andExpect(status().isUnauthorized());
        mvc.perform(post(friend(bob))).andExpect(status().isUnauthorized());
        mvc.perform(post(friend(bob) + "/accept")).andExpect(status().isUnauthorized());
        mvc.perform(delete(friend(bob))).andExpect(status().isUnauthorized());
    }

    LibraryEntry review(User author, int id, boolean published) {
        var entry = LibraryEntry.watched(author.getId(), new MediaSnapshot(id, MediaFormat.MOVIE, MediaKind.MOVIE,
                "Filme " + id, 2026, null, null, List.of(), null, null), null, null, "Review");
        entry.setPublication(published, true);
        return library.save(entry);
    }

    @Test
    void profileReviewsAreFilteredAtTheServerAndDoNotExposePrivateEntries() throws Exception {
        var published = review(bob, 1, true);
        review(bob, 2, false);
        review(alice, 3, true);
        mvc.perform(get("/api/feed").param("authorId", bob.getId().id().toString()).with(as(alice)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].entry.id").value(published.getId().toString()))
                .andExpect(jsonPath("$.content[0].author.id").value(bob.getId().id().toString()));
        mvc.perform(get("/api/feed").with(as(alice))).andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void friendsAndRequestsPaginateWithoutRepeatingPeople() {
        people.request(alice.getId().id(), bob.getId().id());
        people.request(alice.getId().id(), carol.getId().id());
        var first = people.requests(alice.getId().id(), RequestDirection.SENT, 0, 1);
        var second = people.requests(alice.getId().id(), RequestDirection.SENT, 1, 1);
        assertEquals(2, first.getTotalElements());
        assertNotEquals(first.getContent().getFirst().user().getId(), second.getContent().getFirst().user().getId());
        people.accept(bob.getId().id(), alice.getId().id());
        people.accept(carol.getId().id(), alice.getId().id());
        assertEquals(2, people.friends(alice.getId().id(), 0, 1).getTotalElements());
        assertNotEquals(people.friends(alice.getId().id(), 0, 1).getContent().getFirst().user().getId(),
                people.friends(alice.getId().id(), 1, 1).getContent().getFirst().user().getId());
    }

    @Test
    void deletingAnAccountCascadesFriendships() {
        people.request(alice.getId().id(), bob.getId().id());
        people.accept(bob.getId().id(), alice.getId().id());
        em.remove(em.getReference(UserEntity.class, bob.getId().id()));
        em.flush(); em.clear();
        assertTrue(people.friends(alice.getId().id(), 0, 20).isEmpty());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentCrossingRequestsKeepOnePendingRelationship() throws Exception {
        var tx = new TransactionTemplate(transactions);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var start = new CountDownLatch(1);
            var tasks = List.of(executor.submit(() -> {
                start.await(); return attemptRequest(alice, bob);
            }), executor.submit(() -> {
                start.await(); return attemptRequest(bob, alice);
            }));
            start.countDown();
            var results = List.of(tasks.get(0).get(15, TimeUnit.SECONDS), tasks.get(1).get(15, TimeUnit.SECONDS));
            assertEquals(1, results.stream().filter(Boolean::booleanValue).count());
            tx.executeWithoutResult(status -> {
                var row = friendships.find(Friendship.first(alice.getId().id(), bob.getId().id()),
                        Friendship.second(alice.getId().id(), bob.getId().id())).orElseThrow();
                assertEquals(Friendship.Status.PENDING, row.status());
                assertEquals(1L, em.createQuery("select count(f) from FriendshipEntity f", Long.class).getSingleResult());
            });
        } finally {
            tx.executeWithoutResult(status -> {
                for (var user : List.of(alice, bob, carol)) em.remove(em.getReference(UserEntity.class, user.getId().id()));
            });
        }
    }

    boolean attemptRequest(User sender, User recipient) {
        try { people.request(sender.getId().id(), recipient.getId().id()); return true; }
        catch (FriendshipException ex) {
            assertEquals(FriendshipException.Reason.INCOMING_REQUEST, ex.getReason());
            return false;
        }
    }
}
