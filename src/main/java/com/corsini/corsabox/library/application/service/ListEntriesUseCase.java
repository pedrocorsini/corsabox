package com.corsini.corsabox.library.application.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.corsini.corsabox.identity.domain.UserId;
import com.corsini.corsabox.library.domain.EntryStatus;
import com.corsini.corsabox.library.domain.LibraryEntry;
import com.corsini.corsabox.library.domain.LibraryRepository;

@Service
public class ListEntriesUseCase {
    private final LibraryRepository repository;

    public ListEntriesUseCase(LibraryRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Page<LibraryEntry> execute(UUID userId, EntryStatus status, Pageable pageable) {
        return repository.findByStatus(new UserId(userId), status, pageable);
    }
}