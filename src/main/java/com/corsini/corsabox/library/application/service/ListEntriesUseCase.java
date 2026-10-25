package com.corsini.corsabox.library.application.service;

import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.corsini.corsabox.identity.domain.UserId;
import com.corsini.corsabox.library.application.dto.EntryWithProgress;
import com.corsini.corsabox.library.application.port.MediaLookup;
import com.corsini.corsabox.library.domain.EntryStatus;
import com.corsini.corsabox.library.domain.EpisodeLogRepository;
import com.corsini.corsabox.library.domain.EpisodeProgress;
import com.corsini.corsabox.library.domain.LibraryEntry;
import com.corsini.corsabox.library.domain.LibraryRepository;

@Service
public class ListEntriesUseCase {
    private static final Logger log = LoggerFactory.getLogger(ListEntriesUseCase.class);

    private final LibraryRepository repository;
    private final EpisodeLogRepository episodeLogs;
    private final MediaLookup mediaLookup;

    public ListEntriesUseCase(LibraryRepository repository, EpisodeLogRepository episodeLogs,
                              MediaLookup mediaLookup) {
        this.repository = repository;
        this.episodeLogs = episodeLogs;
        this.mediaLookup = mediaLookup;
    }

    public Page<EntryWithProgress> execute(UUID userId, EntryStatus status, Pageable pageable) {
        var page = repository.findByStatus(new UserId(userId), status, pageable);
        if (status == EntryStatus.WATCHLIST)
            return page.map(e -> new EntryWithProgress(e, null));

        var entries = page.map(this::withMetadata);
        var latest = status == EntryStatus.WATCHING
                ? episodeLogs.latestByEntry(entries.getContent().stream().map(LibraryEntry::getId).toList())
                : Map.<UUID, EpisodeProgress>of();
        return entries.map(e -> new EntryWithProgress(e, latest.get(e.getId())));
    }

    private LibraryEntry withMetadata(LibraryEntry entry) {
        var media = entry.getMedia();
        if (media.hasMetadata()) return entry;
        try {
            var fresh = mediaLookup.find(media.format(), media.tmdbId());
            repository.updateMedia(entry.getId(), fresh);
            entry.refreshMedia(fresh);
        } catch (RuntimeException e) {
            log.warn("Sem detalhes para {}/{}", media.format().path(), media.tmdbId());
        }
        return entry;
    }
}