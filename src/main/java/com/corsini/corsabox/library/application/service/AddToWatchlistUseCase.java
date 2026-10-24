package com.corsini.corsabox.library.application.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.corsini.corsabox.catalog.domain.MediaFormat;
import com.corsini.corsabox.identity.domain.UserId;
import com.corsini.corsabox.library.application.port.MediaLookup;
import com.corsini.corsabox.library.domain.EntryStatus;
import com.corsini.corsabox.library.domain.LibraryEntry;
import com.corsini.corsabox.library.domain.LibraryRepository;
import com.corsini.corsabox.library.domain.exception.AlreadyWatchedException;

@Service
public class AddToWatchlistUseCase {
    private final LibraryRepository repository;
    private final MediaLookup mediaLookup;

    public AddToWatchlistUseCase(LibraryRepository repository, MediaLookup mediaLookup) {
        this.repository = repository;
        this.mediaLookup = mediaLookup;
    }

    // Sem @Transactional de propósito: a consulta à TMDB não deve segurar uma conexão do banco.
    public LibraryEntry execute(UUID userId, MediaFormat format, int tmdbId) {
        var uid = new UserId(userId);
        var existing = repository.find(uid, tmdbId, format);
        if (existing.isPresent()) {
            if (existing.get().getStatus() == EntryStatus.WATCHED)
                throw new AlreadyWatchedException();
            return existing.get();
        }
        return repository.save(LibraryEntry.addToWatchlist(uid, mediaLookup.find(format, tmdbId)));
    }
}