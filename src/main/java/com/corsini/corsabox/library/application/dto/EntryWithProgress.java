package com.corsini.corsabox.library.application.dto;

import com.corsini.corsabox.library.domain.EpisodeProgress;
import com.corsini.corsabox.library.domain.LibraryEntry;

public record EntryWithProgress(LibraryEntry entry, EpisodeProgress progress) {}