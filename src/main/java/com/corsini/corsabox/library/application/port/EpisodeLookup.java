package com.corsini.corsabox.library.application.port;

import java.time.LocalDate;
import java.util.List;

public interface EpisodeLookup {
    record Episode(int number, LocalDate airDate) {}
    List<Episode> season(int tvId, int season);
    List<Integer> seasonNumbers(int tvId);
}