package com.corsini.corsabox.identity.domain;

import java.util.List;
import java.util.UUID;

public record PublicProfile(UUID userId, String bio, String photo, List<UUID> favorites,
                            List<com.corsini.corsabox.catalog.domain.CatalogItem> catalogFavorites) {
    public PublicProfile(UUID userId, String bio, String photo, List<UUID> favorites) {
        this(userId, bio, photo, favorites, List.of());
    }
    public PublicProfile {
        catalogFavorites = List.copyOf(catalogFavorites);
        if (catalogFavorites.size() > 15) throw new IllegalArgumentException("Escolha até 5 títulos por categoria.");
        if (catalogFavorites.stream().map(item -> item.format() + ":" + item.id()).distinct().count() != catalogFavorites.size()
                || catalogFavorites.stream().collect(java.util.stream.Collectors.groupingBy(item -> item.kind(),
                    java.util.stream.Collectors.counting())).values().stream().anyMatch(count -> count > 5))
            throw new IllegalArgumentException("Escolha até 5 títulos diferentes por categoria.");
        bio = bio == null ? "" : bio.strip();
        photo = photo == null ? "" : photo;
        favorites = favorites == null ? List.of() : List.copyOf(favorites);
        if (bio.length() > 300) throw new IllegalArgumentException("A bio deve ter até 300 caracteres.");
        if (favorites.size() > 15 || favorites.stream().distinct().count() != favorites.size())
            throw new IllegalArgumentException("Escolha até 5 títulos diferentes por categoria.");
    }
}
