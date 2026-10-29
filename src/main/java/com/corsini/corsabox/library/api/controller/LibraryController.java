package com.corsini.corsabox.library.api.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.corsini.corsabox.catalog.domain.MediaFormat;
import com.corsini.corsabox.library.api.request.WatchedRequest;
import com.corsini.corsabox.library.api.response.EpisodeLogResponse;
import com.corsini.corsabox.library.api.response.LibraryEntryResponse;
import com.corsini.corsabox.library.application.dto.LogEpisodeCommand;
import com.corsini.corsabox.library.application.dto.MarkWatchedCommand;
import com.corsini.corsabox.library.application.service.AddToWatchlistUseCase;
import com.corsini.corsabox.library.application.service.GetEntryUseCase;
import com.corsini.corsabox.library.application.service.ListEntriesUseCase;
import com.corsini.corsabox.library.application.service.MarkAsWatchedUseCase;
import com.corsini.corsabox.library.application.service.RemoveEntryUseCase;
import com.corsini.corsabox.library.application.service.TrackEpisodesUseCase;
import com.corsini.corsabox.library.domain.EntryStatus;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/library")
public class LibraryController {
    private final AddToWatchlistUseCase addToWatchlist;
    private final MarkAsWatchedUseCase markAsWatched;
    private final RemoveEntryUseCase removeEntry;
    private final ListEntriesUseCase listEntries;
    private final GetEntryUseCase getEntry;
    private final TrackEpisodesUseCase track;

    public LibraryController(AddToWatchlistUseCase addToWatchlist, MarkAsWatchedUseCase markAsWatched,
            RemoveEntryUseCase removeEntry, ListEntriesUseCase listEntries,
            GetEntryUseCase getEntry, TrackEpisodesUseCase track) {
        this.addToWatchlist = addToWatchlist;
        this.markAsWatched = markAsWatched;
        this.removeEntry = removeEntry;
        this.listEntries = listEntries;
        this.getEntry = getEntry;
        this.track = track;
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    private Page<LibraryEntryResponse> list(Jwt jwt, EntryStatus status, int page, int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        return listEntries.execute(userId(jwt), status, pageable)
                .map(i -> LibraryEntryResponse.from(i.entry(), i.progress()));
    }

    // Watchlist

    @GetMapping("/watchlist")
    public Page<LibraryEntryResponse> watchlist(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return list(jwt, EntryStatus.WATCHLIST, page, size);
    }

    @PutMapping("/watchlist/{format}/{tmdbId}")
    public LibraryEntryResponse addToWatchlist(@AuthenticationPrincipal Jwt jwt,
            @PathVariable String format, @PathVariable int tmdbId) {
        return LibraryEntryResponse.from(
                addToWatchlist.execute(userId(jwt), MediaFormat.from(format), tmdbId));
    }

    @DeleteMapping("/watchlist/{format}/{tmdbId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFromWatchlist(@AuthenticationPrincipal Jwt jwt,
            @PathVariable String format, @PathVariable int tmdbId) {
        removeEntry.execute(userId(jwt), MediaFormat.from(format), tmdbId, EntryStatus.WATCHLIST);
    }

    // Assistidos

    @GetMapping("/watched")
    public Page<LibraryEntryResponse> watched(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return list(jwt, EntryStatus.WATCHED, page, size);
    }

    @PutMapping("/watched/{format}/{tmdbId}")
    public LibraryEntryResponse markWatched(@AuthenticationPrincipal Jwt jwt,
            @PathVariable String format, @PathVariable int tmdbId,
            @Valid @RequestBody WatchedRequest request) {
        var command = new MarkWatchedCommand(userId(jwt), MediaFormat.from(format), tmdbId,
                request.rating(), request.watchedOn(), request.review(),
                request.publishToFeed(), request.commentsEnabled());
        return LibraryEntryResponse.from(markAsWatched.execute(command));
    }

    @DeleteMapping("/watched/{format}/{tmdbId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFromWatched(@AuthenticationPrincipal Jwt jwt,
            @PathVariable String format, @PathVariable int tmdbId) {
        removeEntry.execute(userId(jwt), MediaFormat.from(format), tmdbId, EntryStatus.WATCHED);
    }

    // Situação de um título para o usuário (para a página do título). 404 se não
    // estiver em nenhuma lista.

    @GetMapping("/entries/{format}/{tmdbId}")
    public LibraryEntryResponse entry(@AuthenticationPrincipal Jwt jwt,
            @PathVariable String format, @PathVariable int tmdbId) {
        return LibraryEntryResponse.from(getEntry.execute(userId(jwt), MediaFormat.from(format), tmdbId));
    }

    // Assistindo

    @GetMapping("/watching")
    public Page<LibraryEntryResponse> watching(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return list(jwt, EntryStatus.WATCHING, page, size);
    }

    @DeleteMapping("/watching/tv/{tmdbId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFromWatching(@AuthenticationPrincipal Jwt jwt, @PathVariable int tmdbId) {
        removeEntry.execute(userId(jwt), MediaFormat.TV, tmdbId, EntryStatus.WATCHING);
    }

    // Episódios

    @GetMapping("/progress/tv/{tmdbId}")
    public List<EpisodeLogResponse> progress(@AuthenticationPrincipal Jwt jwt, @PathVariable int tmdbId) {
        return track.progress(userId(jwt), tmdbId).stream().map(EpisodeLogResponse::from).toList();
    }

    @PutMapping("/progress/tv/{tmdbId}/seasons/{season}/episodes/{episode}")
    public EpisodeLogResponse logEpisode(@AuthenticationPrincipal Jwt jwt, @PathVariable int tmdbId,
            @PathVariable int season, @PathVariable int episode, @Valid @RequestBody WatchedRequest r) {
        var command = new LogEpisodeCommand(userId(jwt), tmdbId, season, episode,
                r.rating(), r.watchedOn(), r.review());
        return EpisodeLogResponse.from(track.log(command));
    }

    @DeleteMapping("/progress/tv/{tmdbId}/seasons/{season}/episodes/{episode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeEpisode(@AuthenticationPrincipal Jwt jwt, @PathVariable int tmdbId,
            @PathVariable int season, @PathVariable int episode) {
        track.remove(userId(jwt), tmdbId, season, episode);
    }

    @PutMapping("/progress/tv/{tmdbId}/seasons/{season}")
    public List<EpisodeLogResponse> markSeason(@AuthenticationPrincipal Jwt jwt, @PathVariable int tmdbId,
            @PathVariable int season, @RequestParam(required = false) Integer until) {
        return track.markSeason(userId(jwt), tmdbId, season, until).stream()
                .map(EpisodeLogResponse::from).toList();
    }
}
