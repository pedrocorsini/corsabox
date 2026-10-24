package com.corsini.corsabox.library.domain;

import com.corsini.corsabox.catalog.domain.MediaFormat;
import com.corsini.corsabox.catalog.domain.MediaKind;

public record MediaSnapshot(int tmdbId, MediaFormat format, MediaKind kind, String title,
                            Integer year, String posterUrl, String backdropUrl) {}