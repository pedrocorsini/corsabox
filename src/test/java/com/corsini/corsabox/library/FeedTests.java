package com.corsini.corsabox.library;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.corsini.corsabox.catalog.domain.MediaFormat;
import com.corsini.corsabox.catalog.domain.MediaKind;
import com.corsini.corsabox.identity.domain.User;
import com.corsini.corsabox.identity.domain.UserRepository;
import com.corsini.corsabox.library.domain.LibraryEntry;
import com.corsini.corsabox.library.domain.LibraryRepository;
import com.corsini.corsabox.library.domain.MediaSnapshot;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:feed-test;MODE=PostgreSQL",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.docker.compose.enabled=false", "app.rate-limit.enabled=false"
})
@Transactional
class FeedTests {
    @Autowired WebApplicationContext context;
    @Autowired LibraryRepository entries;
    @Autowired UserRepository users;
    MockMvc mvc;
    User author;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        author = users.save(User.register("review@example.test", "reviewer", "Ana", "Silva", "hash"));
    }

    LibraryEntry review(int id, String text) {
        return LibraryEntry.watched(author.getId(), new MediaSnapshot(id, MediaFormat.MOVIE, MediaKind.MOVIE,
                "Filme " + id, 2026, null, null, List.of(), null, null), null, null, text);
    }

    @Test
    void publicationIsExplicitAndEditingKeepsItsDate() {
        var entry = review(1, "Gostei");
        assertNull(entry.getPublishedAt());
        assertTrue(entry.isCommentsEnabled());
        entry.setPublication(true, false);
        var publishedAt = entry.getPublishedAt();
        entry.markWatched(null, null, "Gostei muito");
        entry.setPublication(true, false);
        assertEquals(publishedAt, entry.getPublishedAt());
        assertFalse(entry.isCommentsEnabled());
        entry.markWatched(null, null, " ");
        assertNull(entry.getPublishedAt());
        assertThrows(IllegalArgumentException.class, () -> entry.setPublication(true, true));
    }

    @Test
    void feedFiltersPrivateReviewsAndPaginatesNewestFirst() throws Exception {
        entries.save(review(1, "Privada"));
        var older = review(2, "Primeira");
        older.restorePublication(java.time.Instant.parse("2026-01-01T00:00:00Z"), true);
        entries.save(older);
        var newer = review(3, "Segunda");
        newer.setPublication(true, false);
        entries.save(newer);
        var page = entries.findPublished(PageRequest.of(0, 1));
        assertEquals(2, page.getTotalElements());
        assertEquals(newer.getId(), page.getContent().getFirst().entry().getId());
        assertFalse(page.getContent().getFirst().entry().isCommentsEnabled());
        mvc.perform(get("/api/feed").with(jwt().jwt(token -> token.subject(author.getId().id().toString())))).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].author.username").value("reviewer"))
                .andExpect(jsonPath("$.content[0].author.email").doesNotExist())
                .andExpect(jsonPath("$.content.length()").value(2));
        newer.setPublication(false, true);
        entries.save(newer);
        assertEquals(1, entries.findPublished(PageRequest.of(0, 20)).getTotalElements());
        entries.deleteById(older.getId());
        assertTrue(entries.findPublished(PageRequest.of(0, 20)).isEmpty());
    }

    @Test
    void publishingUsesAuthenticatedOwnerAndPreservesOmittedPreferences() throws Exception {
        entries.save(review(1, "Minha review"));
        var owner = jwt().jwt(token -> token.subject(author.getId().id().toString()));
        mvc.perform(put("/api/library/watched/movie/1").with(owner).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"review":"Publicada","publishToFeed":true,"commentsEnabled":false}
                    """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.publishedAt").isNotEmpty())
                .andExpect(jsonPath("$.commentsEnabled").value(false));
        mvc.perform(put("/api/library/watched/movie/1").with(owner).contentType(MediaType.APPLICATION_JSON)
                .content("{\"review\":\"Correção\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.publishedAt").isNotEmpty())
                .andExpect(jsonPath("$.commentsEnabled").value(false));
        var other = users.save(User.register("other@example.test", "other_user", "Bia", "Souza", "hash"));
        entries.save(LibraryEntry.watched(other.getId(), review(1, "Outra").getMedia(), null, null, "Outra"));
        mvc.perform(put("/api/library/watched/movie/1").with(owner).contentType(MediaType.APPLICATION_JSON)
                .content("{\"review\":\"Privada\",\"publishToFeed\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.publishedAt").doesNotExist());
        assertNull(entries.find(other.getId(), 1, MediaFormat.MOVIE).orElseThrow().getPublishedAt());
        mvc.perform(put("/api/library/watched/movie/1").with(owner).contentType(MediaType.APPLICATION_JSON)
                .content("{\"review\":\" \",\"publishToFeed\":true}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void feedRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/feed")).andExpect(status().isUnauthorized());
    }
}
