package com.corsini.corsabox.library;

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
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import com.corsini.corsabox.catalog.domain.MediaFormat;
import com.corsini.corsabox.catalog.domain.MediaKind;
import com.corsini.corsabox.identity.domain.User;
import com.corsini.corsabox.identity.domain.UserRepository;
import com.corsini.corsabox.library.application.service.ReviewInteractionService;
import com.corsini.corsabox.library.domain.*;
import jakarta.persistence.EntityManager;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:feed-test;MODE=PostgreSQL",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.docker.compose.enabled=false", "app.rate-limit.enabled=false"
})
@Transactional
class ReviewInteractionTests {
    @Autowired WebApplicationContext context;
    @Autowired LibraryRepository entries;
    @Autowired UserRepository users;
    @Autowired ReviewInteractionService interactions;
    @Autowired ReviewInteractionRepository repository;
    @Autowired EntityManager em;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
    MockMvc mvc;
    User author;
    User reader;
    LibraryEntry review;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        author = user();
        reader = user();
        review = newReview(1, true);
    }

    User user() {
        var suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return users.save(User.register(suffix + "@example.test", "u_" + suffix, "Ana", "Silva", "hash"));
    }

    LibraryEntry newReview(int tmdbId, boolean published) {
        var entry = LibraryEntry.watched(author.getId(), new MediaSnapshot(tmdbId, MediaFormat.MOVIE,
                MediaKind.MOVIE, "Filme", 2026, null, null, List.of(), null, null), null, null, "Review");
        entry.setPublication(published, true);
        return entries.save(entry);
    }

    JwtRequestPostProcessor as(User user) { return jwt().jwt(token -> token.subject(user.getId().id().toString())); }
    String path() { return "/api/feed/" + review.getId(); }

    @Test
    void commentsUseAuthenticatedAuthorAndValidateText() throws Exception {
        mvc.perform(post(path() + "/comments").with(as(reader)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"  Concordo!  \",\"authorId\":\"" + author.getId().id() + "\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.text").value("Concordo!"))
                .andExpect(jsonPath("$.author.id").value(reader.getId().id().toString()))
                .andExpect(jsonPath("$.author.username").value(reader.getUsername()))
                .andExpect(jsonPath("$.reactions.likes").value(0));
        for (var body : List.of("{}", "{\"text\":\"  \"}", "{\"text\":\"" + "a".repeat(2001) + "\"}")) {
            mvc.perform(post(path() + "/comments").with(as(reader)).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/feed").with(as(reader))).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].commentCount").value(1));
    }

    @Test
    void blockingNewCommentsPreservesExistingDiscussionAndReactions() throws Exception {
        var comment = interactions.addComment(review.getId(), reader.getId().id(), "Comentário").comment();
        review.setPublication(true, false);
        entries.save(review);
        mvc.perform(post(path() + "/comments").with(as(reader)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"Mais um\"}")).andExpect(status().isConflict());
        mvc.perform(get(path() + "/comments").with(as(reader))).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(comment.id().toString()));
        mvc.perform(put(path() + "/comments/" + comment.id() + "/reaction").with(as(author))
                .contentType(MediaType.APPLICATION_JSON).content("{\"reaction\":\"LIKE\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.likes").value(1));
    }

    @Test
    void reviewReactionsAreIdempotentSwitchableAndViewerSpecific() throws Exception {
        for (int i = 0; i < 2; i++) {
            mvc.perform(put(path() + "/reaction").with(as(reader)).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"reaction\":\"LIKE\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.likes").value(1))
                    .andExpect(jsonPath("$.myReaction").value("LIKE"));
        }
        mvc.perform(put(path() + "/reaction").with(as(reader)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"reaction\":\"DISLIKE\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.likes").value(0))
                .andExpect(jsonPath("$.dislikes").value(1));
        mvc.perform(get("/api/feed").with(as(reader))).andExpect(jsonPath("$.content[0].reactions.myReaction").value("DISLIKE"));
        mvc.perform(get("/api/feed").with(as(author))).andExpect(jsonPath("$.content[0].reactions.dislikes").value(1))
                .andExpect(jsonPath("$.content[0].reactions.myReaction").isEmpty());
        for (int i = 0; i < 2; i++) mvc.perform(delete(path() + "/reaction").with(as(reader)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.dislikes").value(0));
        for (var body : List.of("{}", "{\"reaction\":null}", "{\"reaction\":\"INVALID\"}"))
            mvc.perform(put(path() + "/reaction").with(as(reader)).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
    }

    @Test
    void commentReactionsAreSeparateAndCannotCrossReviews() throws Exception {
        var comment = interactions.addComment(review.getId(), reader.getId().id(), "Comentário").comment();
        var other = newReview(2, true);
        var route = path() + "/comments/" + comment.id() + "/reaction";
        for (var reaction : List.of("LIKE", "DISLIKE")) mvc.perform(put(route).with(as(author))
                .contentType(MediaType.APPLICATION_JSON).content("{\"reaction\":\"" + reaction + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.myReaction").value(reaction));
        mvc.perform(get(path() + "/comments").with(as(author)))
                .andExpect(jsonPath("$.content[0].reactions.dislikes").value(1))
                .andExpect(jsonPath("$.content[0].reactions.likes").value(0));
        mvc.perform(get("/api/feed").with(as(author))).andExpect(jsonPath("$.content[0].reactions.dislikes").value(0));
        mvc.perform(put("/api/feed/" + other.getId() + "/comments/" + comment.id() + "/reaction").with(as(author))
                .contentType(MediaType.APPLICATION_JSON).content("{\"reaction\":\"LIKE\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(delete(route).with(as(author))).andExpect(status().isOk()).andExpect(jsonPath("$.dislikes").value(0));
    }

    @Test
    void onlyCommentOwnerCanDeleteAndReactionsCascade() throws Exception {
        var comment = interactions.addComment(review.getId(), reader.getId().id(), "Comentário").comment();
        interactions.react(review.getId(), comment.id(), author.getId().id(), Reaction.LIKE);
        var route = path() + "/comments/" + comment.id();
        mvc.perform(delete(route).with(as(author))).andExpect(status().isForbidden());
        mvc.perform(delete(route).with(as(reader))).andExpect(status().isNoContent());
        em.flush(); em.clear();
        assertEquals(0L, em.createQuery("select count(r) from ReviewReactionEntity r", Long.class).getSingleResult());
        mvc.perform(get(path() + "/comments").with(as(reader))).andExpect(jsonPath("$.content").isEmpty());
        mvc.perform(get("/api/feed").with(as(reader))).andExpect(jsonPath("$.content[0].commentCount").value(0));
    }

    @Test
    void privateReviewsHideAllInteractionsAndDeletingReviewCascades() throws Exception {
        var comment = interactions.addComment(review.getId(), reader.getId().id(), "Comentário").comment();
        interactions.react(review.getId(), null, reader.getId().id(), Reaction.LIKE);
        interactions.react(review.getId(), comment.id(), author.getId().id(), Reaction.DISLIKE);
        review.setPublication(false, true);
        entries.save(review);
        mvc.perform(get(path() + "/comments").with(as(reader))).andExpect(status().isNotFound());
        mvc.perform(post(path() + "/comments").with(as(reader)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"Tentativa\"}")).andExpect(status().isNotFound());
        mvc.perform(delete(path() + "/reaction").with(as(reader))).andExpect(status().isNotFound());
        mvc.perform(delete(path() + "/comments/" + comment.id()).with(as(reader))).andExpect(status().isNotFound());
        mvc.perform(put(path() + "/comments/" + comment.id() + "/reaction").with(as(reader))
                .contentType(MediaType.APPLICATION_JSON).content("{\"reaction\":\"LIKE\"}"))
                .andExpect(status().isNotFound());
        entries.deleteById(review.getId());
        em.flush(); em.clear();
        assertEquals(0L, em.createQuery("select count(c) from ReviewCommentEntity c", Long.class).getSingleResult());
        assertEquals(0L, em.createQuery("select count(r) from ReviewReactionEntity r", Long.class).getSingleResult());
    }

    @Test
    void commentsPaginateAndRequireAuthentication() throws Exception {
        interactions.addComment(review.getId(), reader.getId().id(), "Primeiro");
        interactions.addComment(review.getId(), reader.getId().id(), "Segundo");
        var first = interactions.comments(review.getId(), reader.getId().id(), 0, 1);
        var second = interactions.comments(review.getId(), reader.getId().id(), 1, 1);
        assertEquals(2, first.getTotalElements());
        assertNotEquals(first.getContent().getFirst().comment().id(), second.getContent().getFirst().comment().id());
        mvc.perform(get(path() + "/comments")).andExpect(status().isUnauthorized());
        mvc.perform(post(path() + "/comments").contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"Teste\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(put(path() + "/reaction").contentType(MediaType.APPLICATION_JSON).content("{\"reaction\":\"LIKE\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void simultaneousVotesDoNotCreateDuplicates() throws Exception {
        // Setup runs outside the test transaction for this method, so commit the review first.
        var tx = new TransactionTemplate(transactions);
        tx.executeWithoutResult(status -> entries.save(review));
        try (var executor = Executors.newFixedThreadPool(6)) {
            var start = new CountDownLatch(1);
            var jobs = java.util.stream.IntStream.range(0, 6).mapToObj(i -> executor.submit(() -> {
                start.await();
                return interactions.react(review.getId(), null, reader.getId().id(), i % 2 == 0 ? Reaction.LIKE : Reaction.DISLIKE);
            })).toList();
            start.countDown();
            for (var job : jobs) job.get(15, TimeUnit.SECONDS);
            var result = tx.execute(status -> repository.activities(List.of(review.getId()), reader.getId().id()).get(review.getId()).reactions());
            assertNotNull(result);
            assertEquals(1, result.likes() + result.dislikes());
        } finally {
            tx.executeWithoutResult(status -> {
                entries.deleteById(review.getId());
                em.remove(em.getReference(com.corsini.corsabox.identity.infrastructure.persistence.UserEntity.class, author.getId().id()));
                em.remove(em.getReference(com.corsini.corsabox.identity.infrastructure.persistence.UserEntity.class, reader.getId().id()));
            });
        }
    }
}
