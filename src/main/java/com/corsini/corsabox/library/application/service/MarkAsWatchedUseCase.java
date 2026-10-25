package com.corsini.corsabox.library.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.corsini.corsabox.catalog.domain.MediaFormat;
import com.corsini.corsabox.identity.domain.UserId;
import com.corsini.corsabox.library.application.dto.MarkWatchedCommand;
import com.corsini.corsabox.library.application.port.MediaLookup;
import com.corsini.corsabox.library.domain.EntryStatus;
import com.corsini.corsabox.library.domain.LibraryEntry;
import com.corsini.corsabox.library.domain.LibraryRepository;
import com.corsini.corsabox.library.domain.Rating;

@Service
public class MarkAsWatchedUseCase {
    private static final Logger log = LoggerFactory.getLogger(MarkAsWatchedUseCase.class);

    private final LibraryRepository repository;
    private final MediaLookup mediaLookup;
    private final TrackEpisodesUseCase trackEpisodes;

    public MarkAsWatchedUseCase(LibraryRepository repository, MediaLookup mediaLookup,
                                TrackEpisodesUseCase trackEpisodes) {
        this.repository = repository;
        this.mediaLookup = mediaLookup;
        this.trackEpisodes = trackEpisodes;
    }

    // Sem @Transactional pelo mesmo motivo do AddToWatchlistUseCase.
    public LibraryEntry execute(MarkWatchedCommand command) {
        var uid = new UserId(command.userId());
        var rating = command.rating() == null ? null : Rating.of(command.rating());

        var existing = repository.find(uid, command.tmdbId(), command.format());
        var alreadyWatched = existing.map(e -> e.getStatus() == EntryStatus.WATCHED).orElse(false);

        LibraryEntry saved;
        if (existing.isPresent()) {
            var entry = existing.get();
            entry.markWatched(rating, command.watchedOn(), command.review());
            saved = repository.save(entry);
        } else {
            var media = mediaLookup.find(command.format(), command.tmdbId());
            saved = repository.save(LibraryEntry.watched(uid, media, rating, command.watchedOn(), command.review()));
        }

        if (command.format() == MediaFormat.TV && !alreadyWatched) fillEpisodes(saved);
        return saved;
    }

    // Se a TMDB falhar, a série continua marcada como assistida e os episódios podem ser marcados depois.
    private void fillEpisodes(LibraryEntry entry) {
        try {
            trackEpisodes.markAllSeasons(entry.getId(), entry.getMedia().tmdbId());
        } catch (RuntimeException e) {
            log.warn("Não foi possível registrar os episódios de {}", entry.getMedia().tmdbId(), e);
        }
    }
}