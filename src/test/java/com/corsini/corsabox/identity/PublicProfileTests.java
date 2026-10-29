package com.corsini.corsabox.identity;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import com.corsini.corsabox.identity.application.service.PublicProfileService;
import com.corsini.corsabox.identity.domain.*;
import com.corsini.corsabox.catalog.domain.*;
import com.corsini.corsabox.library.domain.*;
import jakarta.persistence.EntityManager;

@SpringBootTest(properties = {
    "spring.datasource.url=${PUBLIC_PROFILE_TEST_URL:jdbc:h2:mem:public-profiles;MODE=PostgreSQL}",
    "spring.datasource.username=${PUBLIC_PROFILE_TEST_USER:sa}", "spring.datasource.password=${PUBLIC_PROFILE_TEST_PASSWORD:}",
    "spring.docker.compose.enabled=false", "app.rate-limit.enabled=false",
    "spring.jpa.properties.hibernate.generate_statistics=true",
    "logging.level.org.hibernate.stat=OFF"
})
@Transactional
class PublicProfileTests {
    @Autowired PublicProfileService profiles;
    @Autowired UserRepository users;
    @Autowired LibraryRepository library;
    @Autowired EntityManager em;
    @Autowired PublicProfileRepository profileRepository;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.corsini.corsabox.catalog.application.port.CatalogProvider catalog;
    @Autowired WebApplicationContext context;
    User owner;
    MockMvc mvc;
    @BeforeEach void setup() {
        owner = user();
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
    User user() {
        var suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return users.save(User.register(suffix + "@example.test", "u_" + suffix, "Ana", "Silva", "hash"));
    }
    LibraryEntry watched(User user, int id, MediaKind kind) {
        var media = new MediaSnapshot(id, kind == MediaKind.MOVIE ? MediaFormat.MOVIE : MediaFormat.TV,
                kind, "Title " + id, 2026, null, null, List.of(), null, null);
        var entry = LibraryEntry.addToWatchlist(user.getId(), media);
        entry.markWatched(null, LocalDate.now(), null);
        return library.save(entry);
    }
    @Test void catalogFavoritesDoNotRequireOrModifyTheLibraryAndKeepPresentation() throws Exception {
        var id = owner.getId().id();
        profiles.savePresentation(id, "Minha bio", "");
        var items = List.of(
            new CatalogItem(123, "Filme fora da biblioteca", MediaKind.MOVIE, MediaFormat.MOVIE, 2026, null, null, null),
            new CatalogItem(456, "Uma série", MediaKind.TV, MediaFormat.TV, 2025, null, null, null),
            new CatalogItem(789, "Um anime", MediaKind.ANIME, MediaFormat.TV, 2024, null, null, null));
        for (var item : items) org.mockito.Mockito.when(catalog.details(item.format(), item.id()))
            .thenReturn(new MediaDetails(item, null, null, null, null, List.of(), List.of(), List.of(), List.of(), List.of(), null, List.of()));
        var refs = items.stream().map(item -> new PublicProfileService.MediaReference(item.format().path(), item.id())).toList();
        var auth = jwt().jwt(j -> j.subject(id.toString()));
        mvc.perform(put("/api/users/me/catalog-favorites").with(auth).contentType("application/json")
            .content("{\"favorites\":[{\"format\":\"movie\",\"id\":123},{\"format\":\"tv\",\"id\":456},{\"format\":\"tv\",\"id\":789}]}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.favorites.length()").value(3))
            .andExpect(jsonPath("$.bio").value("Minha bio"));
        em.flush(); em.clear();
        var stats = em.getEntityManagerFactory().unwrap(org.hibernate.SessionFactory.class).getStatistics();
        stats.clear();
        profiles.saveCatalogFavorites(id, List.of(refs.get(2), refs.get(0), refs.get(1)));
        em.flush(); em.clear();
        assertEquals(0, stats.getCollectionUpdateCount());
        assertEquals(0, stats.getCollectionRecreateCount());
        assertEquals(List.of(123, 456, 789), profiles.get(id).favorites().stream().map(f -> f.media().id()).toList());
        assertEquals(0L, em.createQuery("select count(e) from LibraryEntryEntity e where e.userId = :id", Long.class).setParameter("id", id).getSingleResult());
        assertThrows(IllegalArgumentException.class, () -> profiles.saveCatalogFavorites(id, List.of(refs.getFirst(), refs.getFirst())));
        profiles.savePresentation(id, "Bio editada", "");
        assertEquals(3, profiles.get(id).favorites().size());
        profiles.saveCatalogFavorites(id, List.of());
        em.flush(); em.clear();
        assertEquals(0, profiles.get(id).favorites().size());
        assertEquals("Bio editada", profiles.get(id).bio());
        mvc.perform(put("/api/users/me/catalog-favorites").contentType("application/json").content("{\"favorites\":[]}"))
            .andExpect(status().isUnauthorized());
    }

    @Test void resolvesPublicProfilesByUsernameWithoutUsingUuidInThePageRoute() throws Exception {
        var auth = jwt().jwt(j -> j.subject(owner.getId().id().toString()));
        mvc.perform(get("/api/users/by-username/" + owner.getUsername().toUpperCase(java.util.Locale.ROOT) + "/profile").with(auth))
            .andExpect(status().isOk()).andExpect(jsonPath("$.user.username").value(owner.getUsername()))
            .andExpect(jsonPath("$.relationship").value("SELF"));
        mvc.perform(get("/api/users/by-username/not_a_real_username/profile").with(auth)).andExpect(status().isNotFound());
    }
    @Test void persistsAllCategoriesInOrderAndFiltersRemovedTitles() {
        var entries = java.util.Arrays.stream(MediaKind.values()).flatMap(kind ->
            IntStream.range(1, 6).mapToObj(i -> watched(owner, kind.ordinal() * 100 + i, kind))).toList();
        var ids = entries.stream().map(LibraryEntry::getId).toList();
        profiles.save(owner.getId().id(), " Minha bio ", "", ids);
        em.flush(); em.clear();
        var result = profiles.get(owner.getId().id());
        assertEquals("Minha bio", result.bio());
        assertEquals(ids, result.favorites().stream().map(PublicProfileService.Favorite::id).toList());
        library.deleteById(ids.getFirst());
        em.flush(); em.clear();
        assertEquals(14, profiles.get(owner.getId().id()).favorites().size());
    }
    @Test void rejectsForeignUnwatchedDuplicateAndOverLimitSelections() {
        var foreign = watched(user(), 1, MediaKind.MOVIE);
        assertThrows(IllegalArgumentException.class, () -> profiles.save(owner.getId().id(), "", "", List.of(foreign.getId())));
        var entries = IntStream.range(1, 7).mapToObj(i -> watched(owner, i, MediaKind.MOVIE)).toList();
        var ids = entries.stream().map(LibraryEntry::getId).toList();
        assertThrows(IllegalArgumentException.class, () -> profiles.save(owner.getId().id(), "", "", ids));
        assertThrows(IllegalArgumentException.class, () -> profiles.save(owner.getId().id(), "", "", List.of(ids.getFirst(), ids.getFirst())));
        var unwatched = LibraryEntry.addToWatchlist(owner.getId(), new MediaSnapshot(1000, MediaFormat.MOVIE, MediaKind.MOVIE, "Pending", null, null, null, List.of(), null, null));
        library.save(unwatched);
        assertThrows(IllegalArgumentException.class, () -> profiles.save(owner.getId().id(), "", "", List.of(unwatched.getId())));
    }
    @Test void independentEditorsPreserveOtherFieldsAndApplyWatchedValidation() throws Exception {
        var entry = watched(owner, 1, MediaKind.MOVIE);
        var ownerId = owner.getId().id();
        profiles.savePresentation(ownerId, "Bio atual", "");
        mvc.perform(put("/api/users/me/favorites").with(jwt().jwt(j -> j.subject(ownerId.toString())))
            .contentType("application/json").content("{\"favorites\":[\"" + entry.getId() + "\"]}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.bio").value("Bio atual"))
            .andExpect(jsonPath("$.favorites.length()").value(1));
        mvc.perform(put("/api/users/me/presentation").with(jwt().jwt(j -> j.subject(ownerId.toString())))
            .contentType("application/json").content("{\"bio\":\"Nova bio\",\"photo\":\"\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.favorites.length()").value(1));
        assertEquals("Nova bio", profiles.get(ownerId).bio());
        var foreign = watched(user(), 2, MediaKind.TV);
        mvc.perform(put("/api/users/me/favorites").with(jwt().jwt(j -> j.subject(ownerId.toString())))
            .contentType("application/json").content("{\"favorites\":[\"" + foreign.getId() + "\"]}"))
            .andExpect(status().isBadRequest());
        mvc.perform(put("/api/users/me/favorites").contentType("application/json").content("{\"favorites\":[]}"))
            .andExpect(status().isUnauthorized());
        library.deleteById(entry.getId());
        assertEquals(0, profiles.savePresentation(ownerId, "Ainda posso editar", "").favorites().size());
    }
    @Test void restoresSelectionWithoutReorderingAndReplacesOverlappingPositions() throws Exception {
        var a = watched(owner, 1, MediaKind.MOVIE).getId();
        var b = watched(owner, 2, MediaKind.MOVIE).getId();
        var c = watched(owner, 3, MediaKind.MOVIE).getId();
        var id = owner.getId().id();
        var image = new java.awt.image.BufferedImage(2, 2, java.awt.image.BufferedImage.TYPE_INT_RGB);
        var output = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "png", output);
        var photo = profiles.save(id, "Bio", "data:image/png;base64," +
            java.util.Base64.getEncoder().encodeToString(output.toByteArray()), List.of(a, b, c)).photo();
        em.flush(); em.clear();
        var statistics = em.getEntityManagerFactory().unwrap(org.hibernate.SessionFactory.class).getStatistics();
        statistics.clear();
        profiles.saveFavorites(id, List.of(b, c, a));
        profiles.saveFavorites(id, List.of(c, a, b));
        em.flush(); em.clear();
        assertEquals(0, statistics.getCollectionUpdateCount());
        assertEquals(0, statistics.getCollectionRecreateCount());
        assertEquals(0, statistics.getCollectionRemoveCount());
        assertEquals(0, statistics.getEntityUpdateCount());
        assertEquals(List.of(a, b, c), profiles.get(id).favorites().stream().map(PublicProfileService.Favorite::id).toList());
        profiles.save(id, "Bio", photo, List.of(c, a, b));
        em.flush(); em.clear();
        assertEquals(List.of(a, b, c), profiles.get(id).favorites().stream().map(PublicProfileService.Favorite::id).toList());
        // Exercise the repository with a genuine position swap under the deployed unique constraint.
        profileRepository.save(new PublicProfile(id, "Bio", photo, List.of(c, a, b)));
        em.flush(); em.clear();
        assertEquals(List.of(c, a, b), profiles.get(id).favorites().stream().map(PublicProfileService.Favorite::id).toList());
        profiles.saveFavorites(id, List.of(a, b));
        em.flush(); em.clear();
        profiles.saveFavorites(id, List.of(c, a));
        em.flush(); em.clear();
        assertEquals(List.of(a, c), profiles.get(id).favorites().stream().map(PublicProfileService.Favorite::id).toList());
        assertEquals("Bio", profiles.get(id).bio());
        assertEquals(photo, profiles.get(id).photo());
        for (int i = 0; i < 3; i++) {
            profiles.saveFavorites(id, List.of(c, a));
            em.flush(); em.clear();
            assertEquals(List.of(a, c), profiles.get(id).favorites().stream().map(PublicProfileService.Favorite::id).toList());
            assertEquals(photo, profiles.get(id).photo());
        }
    }
    @Test void searchesWholeWatchedLibraryByOwnerCategoryAndLiteralQuery() throws Exception {
        IntStream.range(1, 106).forEach(i -> watched(owner, i, MediaKind.MOVIE));
        watched(owner, 200, MediaKind.TV);
        watched(user(), 106, MediaKind.MOVIE);
        var entry = watched(owner, 999, MediaKind.MOVIE);
        em.flush();
        em.createNativeQuery("update library_entries set title = 'ZZ Needle 100%_unique' where id = :id")
            .setParameter("id", entry.getId()).executeUpdate();
        em.clear();
        var auth = jwt().jwt(j -> j.subject(owner.getId().id().toString()));
        mvc.perform(get("/api/users/me/favorite-candidates").with(auth).param("kind", "movie").param("q", "needle"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1));
        mvc.perform(get("/api/users/me/favorite-candidates").with(auth).param("kind", "movie").param("q", "%_"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1));
        mvc.perform(get("/api/users/me/favorite-candidates").with(auth).param("kind", "movie").param("page", "5"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(6));
        mvc.perform(get("/api/users/me/favorite-candidates").with(auth).param("kind", "anime"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(0));
        mvc.perform(get("/api/users/me/favorite-candidates").with(auth).param("kind", "other"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/users/me/favorite-candidates").param("kind", "movie"))
            .andExpect(status().isUnauthorized());
    }
    @Test void validatesPhotoAndBio() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> profiles.save(owner.getId().id(), "x".repeat(301), "", List.of()));
        assertThrows(IllegalArgumentException.class, () -> profiles.save(owner.getId().id(), "", "data:image/svg+xml;base64,PHN2Zz4=", List.of()));
        assertThrows(IllegalArgumentException.class, () -> profiles.save(owner.getId().id(), "", "data:image/png;base64,YWJj", List.of()));
        var image = new java.awt.image.BufferedImage(20, 30, java.awt.image.BufferedImage.TYPE_INT_RGB);
        var output = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "png", output);
        var photo = "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(output.toByteArray());
        assertTrue(profiles.save(owner.getId().id(), "bio", photo, List.of()).photo().startsWith("data:image/jpeg;base64,"));
        assertEquals("", profiles.save(owner.getId().id(), "", "", List.of()).photo());
    }
    @Test void authenticatedOwnerIsTakenFromTokenAndPublicPayloadHasNoPrivateFields() throws Exception {
        mvc.perform(put("/api/users/me/customization").with(jwt().jwt(j -> j.subject(owner.getId().id().toString())))
            .contentType("application/json").content("{\"bio\":\"Hello\",\"photo\":\"\",\"favorites\":[]}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.bio").value("Hello"));
        mvc.perform(get("/api/users/" + owner.getId().id() + "/customization").with(jwt()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.email").doesNotExist()).andExpect(jsonPath("$.passwordHash").doesNotExist());
        mvc.perform(put("/api/users/me/customization").contentType("application/json").content("{}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(put("/api/users/me/customization").with(jwt()).contentType("application/json").content("{\"bio\":null,\"photo\":\"\",\"favorites\":[]}"))
            .andExpect(status().isBadRequest());
    }
}
