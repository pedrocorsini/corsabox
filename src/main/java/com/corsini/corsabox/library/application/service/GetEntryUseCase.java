package com.corsini.corsabox.library.application.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.corsini.corsabox.catalog.domain.MediaFormat;
import com.corsini.corsabox.identity.domain.UserId;
import com.corsini.corsabox.library.domain.LibraryEntry;
import com.corsini.corsabox.library.domain.LibraryRepository;
import com.corsini.corsabox.library.domain.exception.EntryNotFoundException;

@Service
public class GetEntryUseCase {
    private final LibraryRepository repository;

    public GetEntryUseCase(LibraryRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public LibraryEntry execute(UUID userId, MediaFormat format, int tmdbId) {
        return repository.find(new UserId(userId), tmdbId, format)
                .orElseThrow(EntryNotFoundException::new);
    }
}