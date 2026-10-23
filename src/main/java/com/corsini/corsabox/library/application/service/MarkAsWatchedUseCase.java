package com.corsini.corsabox.library.application.service;

import org.springframework.stereotype.Service;

import com.corsini.corsabox.identity.domain.UserId;
import com.corsini.corsabox.library.application.dto.MarkWatchedCommand;
import com.corsini.corsabox.library.application.port.MediaLookup;
import com.corsini.corsabox.library.domain.LibraryEntry;
import com.corsini.corsabox.library.domain.LibraryRepository;
import com.corsini.corsabox.library.domain.Rating;

@Service
public class MarkAsWatchedUseCase {
    private final LibraryRepository repository;
    private final MediaLookup mediaLookup;

    public MarkAsWatchedUseCase(LibraryRepository repository, MediaLookup mediaLookup) {
        this.repository = repository;
        this.mediaLookup = mediaLookup;
    }

    // Sem @Transactional pelo mesmo motivo do AddToWatchlistUseCase.
    public LibraryEntry execute(MarkWatchedCommand command) {
        var uid = new UserId(command.userId());
        var rating = command.rating() == null ? null : Rating.of(command.rating());

        var existing = repository.find(uid, command.tmdbId(), command.format());
        if (existing.isPresent()) {
            var entry = existing.get();
            entry.markWatched(rating, command.watchedOn(), command.review());
            return repository.save(entry);
        }

        var media = mediaLookup.find(command.format(), command.tmdbId());
        return repository.save(LibraryEntry.watched(uid, media, rating, command.watchedOn(), command.review()));
    }
}