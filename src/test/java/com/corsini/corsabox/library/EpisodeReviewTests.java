package com.corsini.corsabox.library;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.corsini.corsabox.catalog.domain.MediaFormat;
import com.corsini.corsabox.catalog.domain.MediaKind;
import com.corsini.corsabox.identity.domain.User;
import com.corsini.corsabox.identity.domain.UserRepository;
import com.corsini.corsabox.library.application.port.EpisodeLookup;
import com.corsini.corsabox.library.application.service.ReviewInteractionService;
import com.corsini.corsabox.library.application.service.TrackEpisodesUseCase;
import com.corsini.corsabox.library.domain.*;
import jakarta.persistence.EntityManager;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:episode-review-test;MODE=PostgreSQL",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.docker.compose.enabled=false", "app.rate-limit.enabled=false"
})
@Transactional
class EpisodeReviewTests {
    @Autowired WebApplicationContext context;
    @Autowired LibraryRepository entries;
    @Autowired EpisodeLogRepository logs;
    @Autowired UserRepository users;
    @Autowired ReviewInteractionService interactions;
    @Autowired TrackEpisodesUseCase tracking;
    @Autowired EntityManager em;
    @MockitoBean EpisodeLookup episodes;
    MockMvc mvc;
    User author;
    User reader;
    LibraryEntry series;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        author = users.save(User.register("episodes@example.test", "episode_author", "Ana", "Silva", "hash"));
        reader = users.save(User.register("reader@example.test", "episode_reader", "Bia", "Silva", "hash"));
        series = entries.save(LibraryEntry.watching(author.getId(), new MediaSnapshot(10, MediaFormat.TV,
                MediaKind.ANIME, "Anime", 2026, null, null, List.of(), null, null)));
        when(episodes.season(10, 1)).thenReturn(List.of(new EpisodeLookup.Episode(1, null),
                new EpisodeLookup.Episode(2, null), new EpisodeLookup.Episode(3, LocalDate.now().plusDays(5))));
    }

    String route(int episode) { return "/api/library/progress/tv/10/seasons/1/episodes/" + episode; }
    EpisodeLog episode(int number, boolean published) {
        var log = EpisodeLog.create(series.getId(), 1, number, Rating.of(new java.math.BigDecimal("4.5")), null, "Review " + number);
        log.setPublication(published, true);
        return logs.save(log);
    }

    @Test
    void publicationIsExplicitAndOwnerPreferencesSurviveEditing() throws Exception {
        var owner = jwt().jwt(t -> t.subject(author.getId().id().toString()));
        mvc.perform(put(route(1)).with(owner).contentType(MediaType.APPLICATION_JSON).content("{\"review\":\"Privada\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.publishedAt").doesNotExist());
        mvc.perform(put(route(1)).with(owner).contentType(MediaType.APPLICATION_JSON)
                .content("{\"review\":\"Publicada\",\"publishToFeed\":true,\"commentsEnabled\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.publishedAt").isNotEmpty())
                .andExpect(jsonPath("$.commentsEnabled").value(false));
        var date = logs.find(series.getId(), 1, 1).orElseThrow().getPublishedAt();
        mvc.perform(put(route(1)).with(owner).contentType(MediaType.APPLICATION_JSON).content("{\"review\":\"Editada\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.commentsEnabled").value(false));
        assertEquals(date, logs.find(series.getId(), 1, 1).orElseThrow().getPublishedAt());
        mvc.perform(get("/api/library/progress/tv/10").with(owner))
                .andExpect(jsonPath("$[0].publishedAt").isNotEmpty());
        mvc.perform(put(route(1)).with(owner).contentType(MediaType.APPLICATION_JSON).content("{\"review\":\" \"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.publishedAt").doesNotExist());
        mvc.perform(put(route(1)).with(owner).contentType(MediaType.APPLICATION_JSON)
                .content("{\"review\":\" \",\"publishToFeed\":true}"))
                .andExpect(status().isBadRequest());
        // Another user's request cannot edit the author's log.
        entries.save(LibraryEntry.watching(reader.getId(), series.getMedia()));
        mvc.perform(put(route(1)).with(jwt().jwt(t -> t.subject(reader.getId().id().toString())))
                .contentType(MediaType.APPLICATION_JSON).content("{\"review\":\"Outra review\",\"publishToFeed\":true}"))
                .andExpect(status().isOk());
        assertNull(logs.find(series.getId(), 1, 1).orElseThrow().getPublishedAt());
        mvc.perform(put(route(3)).with(owner).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put(route(1)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void mixedFeedPaginatesAndFiltersByAuthorUsingEpisodeIdentity() throws Exception {
        episode(2, false);
        var movie = LibraryEntry.watched(author.getId(), new MediaSnapshot(20, MediaFormat.MOVIE, MediaKind.MOVIE,
                "Filme", 2026, null, null, List.of(), null, null), null, null, "Filme review");
        movie.restorePublication(Instant.parse("2026-01-01T00:00:00Z"), true);
        entries.save(movie);
        var log = episode(1, true);
        var first = entries.findPublished(PageRequest.of(0, 1));
        var second = entries.findPublished(PageRequest.of(1, 1));
        assertEquals(2, first.getTotalElements());
        assertEquals(log.getId(), first.getContent().getFirst().id());
        assertEquals(movie.getId(), second.getContent().getFirst().id());
        assertEquals(2, entries.findPublishedByAuthor(author.getId().id(), PageRequest.of(0, 20)).getTotalElements());
        assertTrue(entries.findPublishedByAuthor(reader.getId().id(), PageRequest.of(0, 20)).isEmpty());
        em.flush(); em.clear();
        mvc.perform(get("/api/feed").with(jwt().jwt(t -> t.subject(reader.getId().id().toString()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].entry.id").value(log.getId().toString()))
                .andExpect(jsonPath("$.content[0].entry.media.kind").value("anime"))
                .andExpect(jsonPath("$.content[0].entry.review").value("Review 1"))
                .andExpect(jsonPath("$.content[0].entry.rating").value(4.5))
                .andExpect(jsonPath("$.content[0].episode.season").value(1))
                .andExpect(jsonPath("$.content[0].episode.episode").value(1))
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void episodeInteractionsAreIsolatedAndRespectPublicationAndComments() throws Exception {
        var one = episode(1, true);
        var two = episode(2, true);
        var comment = interactions.addComment(one.getId(), reader.getId().id(), "Comentário").comment();
        interactions.react(one.getId(), null, reader.getId().id(), Reaction.LIKE);
        interactions.react(one.getId(), comment.id(), author.getId().id(), Reaction.DISLIKE);
        em.flush(); em.clear();
        assertEquals(1, interactions.comments(one.getId(), author.getId().id(), 0, 20).getTotalElements());
        assertTrue(interactions.comments(two.getId(), author.getId().id(), 0, 20).isEmpty());
        var asReader = jwt().jwt(t -> t.subject(reader.getId().id().toString()));
        mvc.perform(get("/api/feed/" + one.getId() + "/comments").with(asReader))
                .andExpect(jsonPath("$.content[0].reactions.dislikes").value(1));
        mvc.perform(put("/api/feed/" + two.getId() + "/comments/" + comment.id() + "/reaction")
                .with(asReader).contentType(MediaType.APPLICATION_JSON).content("{\"reaction\":\"LIKE\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/feed/" + one.getId() + "/comments/" + comment.id())
                .with(jwt().jwt(t -> t.subject(author.getId().id().toString())))).andExpect(status().isForbidden());
        one.setPublication(true, false);
        logs.save(one);
        mvc.perform(post("/api/feed/" + one.getId() + "/comments").with(asReader)
                .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"Fechado\"}"))
                .andExpect(status().isConflict());
        one.setPublication(false, false);
        logs.save(one);
        mvc.perform(get("/api/feed/" + one.getId() + "/comments").with(asReader)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/feed/" + one.getId() + "/reaction").with(asReader)).andExpect(status().isNotFound());
        one.setPublication(true, true);
        logs.save(one);
        mvc.perform(get("/api/feed?authorId=" + author.getId().id()).with(asReader))
                .andExpect(jsonPath("$.content[0].commentCount").value(1))
                .andExpect(jsonPath("$.content[0].reactions.likes").value(1));
        mvc.perform(delete("/api/feed/" + one.getId() + "/comments/" + comment.id()).with(asReader))
                .andExpect(status().isNoContent());
    }

    @Test
    void removingEpisodeCascadesInteractionsAndBulkMarkingDoesNotPublish() throws Exception {
        var one = episode(1, true);
        var comment = interactions.addComment(one.getId(), reader.getId().id(), "Comentário").comment();
        interactions.react(one.getId(), null, reader.getId().id(), Reaction.LIKE);
        interactions.react(one.getId(), comment.id(), author.getId().id(), Reaction.LIKE);
        tracking.markSeason(author.getId().id(), 10, 1, null);
        assertNotNull(logs.find(series.getId(), 1, 1).orElseThrow().getPublishedAt());
        assertNull(logs.find(series.getId(), 1, 2).orElseThrow().getPublishedAt());
        assertTrue(logs.find(series.getId(), 1, 3).isEmpty());
        mvc.perform(delete(route(1)).with(jwt().jwt(t -> t.subject(author.getId().id().toString()))))
                .andExpect(status().isNoContent());
        em.flush(); em.clear();
        assertTrue(entries.findPublished(PageRequest.of(0, 20)).isEmpty());
        assertEquals(0L, em.createQuery("select count(c) from ReviewCommentEntity c", Long.class).getSingleResult());
        assertEquals(0L, em.createQuery("select count(r) from ReviewReactionEntity r", Long.class).getSingleResult());
        var two = logs.find(series.getId(), 1, 2).orElseThrow();
        two.update(null, null, "Review 2");
        two.setPublication(true, true);
        logs.save(two);
        interactions.addComment(two.getId(), reader.getId().id(), "Outro comentário");
        interactions.react(two.getId(), null, reader.getId().id(), Reaction.LIKE);
        mvc.perform(delete("/api/library/watching/tv/10").with(jwt().jwt(t -> t.subject(author.getId().id().toString()))))
                .andExpect(status().isNoContent());
        em.flush(); em.clear();
        assertTrue(logs.findByEntry(series.getId()).isEmpty());
        assertTrue(entries.findPublished(PageRequest.of(0, 20)).isEmpty());
        assertEquals(0L, em.createQuery("select count(c) from ReviewCommentEntity c", Long.class).getSingleResult());
        assertEquals(0L, em.createQuery("select count(r) from ReviewReactionEntity r", Long.class).getSingleResult());
    }
}
