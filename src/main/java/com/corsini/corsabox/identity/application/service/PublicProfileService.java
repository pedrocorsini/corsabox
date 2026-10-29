package com.corsini.corsabox.identity.application.service;

import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.corsini.corsabox.identity.domain.*;
import com.corsini.corsabox.identity.domain.exception.UserNotFoundException;
import com.corsini.corsabox.library.domain.*;

@Service
@Transactional
public class PublicProfileService {
    private final PublicProfileRepository profiles;
    private final UserRepository users;
    private final LibraryRepository library;
    private final com.corsini.corsabox.catalog.application.port.CatalogProvider catalog;
    public PublicProfileService(PublicProfileRepository profiles, UserRepository users, LibraryRepository library,
            com.corsini.corsabox.catalog.application.port.CatalogProvider catalog) {
        this.profiles = profiles; this.users = users; this.library = library; this.catalog = catalog;
    }
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<LibraryEntry> candidates(UUID id, String kind, String query, int page, int size) {
        if (query.length() > 100) throw new IllegalArgumentException("A busca deve ter até 100 caracteres.");
        var category = com.corsini.corsabox.catalog.domain.MediaKind.valueOf(kind.toUpperCase(Locale.ROOT));
        var normalized = query.strip().toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return library.searchWatched(id, category, "%" + normalized + "%",
            org.springframework.data.domain.PageRequest.of(Math.max(0, page), Math.min(50, Math.max(1, size))));
    }
    public record Favorite(UUID id, com.corsini.corsabox.catalog.domain.CatalogItem media) {}
    public record Details(String bio, String photo, List<Favorite> favorites) {}
    public record MediaReference(String format, int id) {}

    public Details saveCatalogFavorites(UUID id, List<MediaReference> favorites) {
        if (favorites.size() > 15 || new HashSet<>(favorites).size() != favorites.size())
            throw new IllegalArgumentException("Escolha até 5 títulos diferentes por categoria.");
        // Resolve trusted catalog metadata before taking the profile lock.
        var requested = favorites.stream().map(ref -> {
            if (ref.id() < 1) throw new IllegalArgumentException("Título inválido.");
            var item = catalog.details(com.corsini.corsabox.catalog.domain.MediaFormat.from(ref.format()), ref.id()).item();
            return new com.corsini.corsabox.catalog.domain.CatalogItem(item.id(), item.title(), item.kind(), item.format(),
                item.year(), null, item.posterUrl(), null);
        }).toList();
        if (requested.stream().collect(Collectors.groupingBy(item -> item.kind(), Collectors.counting()))
                .values().stream().anyMatch(count -> count > 5))
            throw new IllegalArgumentException("Escolha até 5 títulos por categoria.");
        profiles.lockUser(id);
        var current = profiles.find(id).orElse(new PublicProfile(id, "", "", List.of()));
        var previous = details(current).favorites().stream().map(Favorite::media).toList();
        var keys = requested.stream().map(PublicProfileService::mediaKey).collect(Collectors.toSet());
        var ordered = new ArrayList<>(previous.stream().filter(item -> keys.contains(mediaKey(item))).toList());
        var retained = ordered.stream().map(PublicProfileService::mediaKey).collect(Collectors.toSet());
        requested.stream().filter(item -> !retained.contains(mediaKey(item))).forEach(ordered::add);
        if (!current.favorites().isEmpty() || !previous.stream().map(PublicProfileService::mediaKey).collect(Collectors.toSet()).equals(keys))
            profiles.save(new PublicProfile(id, current.bio(), current.photo(), List.of(), ordered));
        return details(profiles.find(id).orElse(current));
    }
    private static String mediaKey(com.corsini.corsabox.catalog.domain.CatalogItem item) {
        return item.format().path() + ":" + item.id();
    }
    @Transactional(readOnly = true)
    public Details get(UUID id) {
        if (users.findById(new UserId(id)).isEmpty()) throw new UserNotFoundException(id);
        var p = profiles.find(id).orElse(new PublicProfile(id, "", "", List.of()));
        return details(p);
    }
    public Details save(UUID id, String bio, String photo, List<UUID> favorites) {
        var p = new PublicProfile(id, bio, ProfilePhoto.normalize(photo), favorites);
        profiles.lockUser(id);
        var current = profiles.find(id).orElse(new PublicProfile(id, "", "", List.of()));
        return saveFavoritesValidated(p, current);
    }
    public Details savePresentation(UUID id, String bio, String photo) {
        var normalizedPhoto = ProfilePhoto.normalize(photo);
        profiles.lockUser(id);
        var current = profiles.find(id).orElse(new PublicProfile(id, "", "", List.of()));
        var p = new PublicProfile(id, bio, normalizedPhoto, current.favorites(), current.catalogFavorites());
        profiles.save(p);
        return details(p);
    }
    public Details saveFavorites(UUID id, List<UUID> favorites) {
        profiles.lockUser(id);
        var current = profiles.find(id).orElse(new PublicProfile(id, "", "", List.of()));
        var requested = new PublicProfile(id, current.bio(), current.photo(), favorites);
        return saveFavoritesValidated(requested, current);
    }
    private Details saveFavoritesValidated(PublicProfile requested, PublicProfile current) {
        validateFavorites(requested);
        var ids = new HashSet<>(requested.favorites());
        var ordered = new ArrayList<>(current.favorites().stream().filter(ids::contains).toList());
        requested.favorites().stream().filter(entry -> !ordered.contains(entry)).forEach(ordered::add);
        var updated = new PublicProfile(requested.userId(), requested.bio(), requested.photo(), ordered);
        if (!updated.equals(current)) profiles.save(updated);
        return details(updated);
    }
    private void validateFavorites(PublicProfile p) {
        var entries = library.findWatchedByIds(p.userId(), p.favorites());
        if (entries.size() != p.favorites().size())
            throw new IllegalArgumentException("Selecione somente títulos da sua biblioteca de assistidos.");
        var counts = entries.stream().collect(Collectors.groupingBy(e -> e.getMedia().kind(), Collectors.counting()));
        if (counts.values().stream().anyMatch(count -> count > 5))
            throw new IllegalArgumentException("Escolha até 5 títulos por categoria.");
    }
    private Details details(PublicProfile p) {
        if (!p.catalogFavorites().isEmpty()) return new Details(p.bio(), p.photo(), p.catalogFavorites().stream()
            .map(item -> new Favorite(UUID.nameUUIDFromBytes(mediaKey(item).getBytes(java.nio.charset.StandardCharsets.UTF_8)), item)).toList());
        var entries = library.findWatchedByIds(p.userId(), p.favorites()).stream()
                .collect(Collectors.toMap(LibraryEntry::getId, e -> e));
        return new Details(p.bio(), p.photo(), p.favorites().stream().map(entries::get).filter(Objects::nonNull).map(entry -> {
            var media = entry.getMedia();
            return new Favorite(entry.getId(), new com.corsini.corsabox.catalog.domain.CatalogItem(media.tmdbId(), media.title(),
                media.kind(), media.format(), media.year(), null, media.posterUrl(), null));
        }).toList());
    }
}
