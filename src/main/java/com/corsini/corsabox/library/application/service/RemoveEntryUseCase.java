package com.corsini.corsabox.library.application.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.corsini.corsabox.catalog.domain.MediaFormat;
import com.corsini.corsabox.identity.domain.UserId;
import com.corsini.corsabox.library.domain.EntryStatus;
import com.corsini.corsabox.library.domain.EpisodeLogRepository;
import com.corsini.corsabox.library.domain.LibraryRepository;
import com.corsini.corsabox.library.domain.exception.EntryNotFoundException;

@Service
public class RemoveEntryUseCase {
    private final LibraryRepository repository;
    private final EpisodeLogRepository logs;

    public RemoveEntryUseCase(LibraryRepository repository, EpisodeLogRepository logs) {
        this.repository = repository;
        this.logs = logs;
    }

    /** Só remove se o título estiver na lista indicada (watchlist ou assistidos). */
    @Transactional
    public void execute(UUID userId, MediaFormat format, int tmdbId, EntryStatus from) {
        var entry = repository.find(new UserId(userId), tmdbId, format)
                .filter(e -> e.getStatus() == from)
                .orElseThrow(EntryNotFoundException::new);
        logs.deleteByEntry(entry.getId());        
        repository.deleteById(entry.getId());
    }
}