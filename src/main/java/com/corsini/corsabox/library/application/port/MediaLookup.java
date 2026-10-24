package com.corsini.corsabox.library.application.port;

import com.corsini.corsabox.catalog.domain.MediaFormat;
import com.corsini.corsabox.library.domain.MediaSnapshot;

public interface MediaLookup {
    MediaSnapshot find(MediaFormat format, int tmdbId);
}