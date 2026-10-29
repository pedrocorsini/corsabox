package com.corsini.corsabox.identity.api.controller;

import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import com.corsini.corsabox.identity.application.service.PublicProfileService;
import com.corsini.corsabox.library.api.response.LibraryEntryResponse;

@RestController
@RequestMapping("/api/users")
public class PublicProfileController {
    private final PublicProfileService profiles;
    public PublicProfileController(PublicProfileService profiles) { this.profiles = profiles; }
    public record Request(@NotNull @Size(max = 300) String bio,
        @NotNull @Size(max = 400000) String photo, @NotNull @Size(max = 15) List<@NotNull UUID> favorites) {}
    public record PresentationRequest(@NotNull @Size(max = 300) String bio, @NotNull @Size(max = 400000) String photo) {}
    public record FavoritesRequest(@NotNull @Size(max = 15) List<@NotNull UUID> favorites) {}
    public record FavoriteResponse(UUID id, com.corsini.corsabox.catalog.api.response.CatalogItemResponse media) {}
    public record CatalogFavoritesRequest(@NotNull @Size(max = 15) List<@NotNull @Valid MediaRequest> favorites) {}
    public record MediaRequest(@NotNull @Pattern(regexp = "movie|tv") String format, @Min(1) int id) {}
    @PutMapping("/me/catalog-favorites")
    public Response saveCatalogFavorites(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CatalogFavoritesRequest request) {
        return Response.from(profiles.saveCatalogFavorites(UUID.fromString(jwt.getSubject()), request.favorites().stream()
            .map(ref -> new PublicProfileService.MediaReference(ref.format(), ref.id())).toList()));
    }
    public record Response(String bio, String photo, List<FavoriteResponse> favorites) {
        static Response from(PublicProfileService.Details d) {
            return new Response(d.bio(), d.photo(), d.favorites().stream().map(f -> new FavoriteResponse(f.id(),
                com.corsini.corsabox.catalog.api.response.CatalogItemResponse.from(f.media()))).toList());
        }
    }
    @GetMapping("/me/favorite-candidates")
    public org.springframework.data.domain.Page<LibraryEntryResponse> candidates(@AuthenticationPrincipal Jwt jwt,
        @RequestParam String kind, @RequestParam(defaultValue = "") String q,
        @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return profiles.candidates(UUID.fromString(jwt.getSubject()), kind, q, page, size).map(LibraryEntryResponse::from);
    }
    @GetMapping("/{id}/customization")
    public Response get(@PathVariable UUID id) { return Response.from(profiles.get(id)); }
    @PutMapping("/me/customization")
    public Response save(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody Request request) {
        return Response.from(profiles.save(UUID.fromString(jwt.getSubject()), request.bio(), request.photo(), request.favorites()));
    }
    @PutMapping("/me/presentation")
    public Response savePresentation(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PresentationRequest request) {
        return Response.from(profiles.savePresentation(UUID.fromString(jwt.getSubject()), request.bio(), request.photo()));
    }
    @PutMapping("/me/favorites")
    public Response saveFavorites(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody FavoritesRequest request) {
        return Response.from(profiles.saveFavorites(UUID.fromString(jwt.getSubject()), request.favorites()));
    }
}
