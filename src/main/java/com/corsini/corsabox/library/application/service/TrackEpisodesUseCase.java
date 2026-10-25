package com.corsini.corsabox.library.application.service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.corsini.corsabox.catalog.domain.MediaFormat;
import com.corsini.corsabox.identity.domain.UserId;
import com.corsini.corsabox.library.application.dto.LogEpisodeCommand;
import com.corsini.corsabox.library.application.port.EpisodeLookup;
import com.corsini.corsabox.library.application.port.MediaLookup;
import com.corsini.corsabox.library.domain.EntryStatus;
import com.corsini.corsabox.library.domain.EpisodeLog;
import com.corsini.corsabox.library.domain.EpisodeLogRepository;
import com.corsini.corsabox.library.domain.LibraryEntry;
import com.corsini.corsabox.library.domain.LibraryRepository;
import com.corsini.corsabox.library.domain.Rating;
import com.corsini.corsabox.library.domain.exception.EntryNotFoundException;

@Service
public class TrackEpisodesUseCase {
    private final LibraryRepository entries;
    private final EpisodeLogRepository logs;
    private final MediaLookup mediaLookup;
    private final EpisodeLookup episodeLookup;

    public TrackEpisodesUseCase(LibraryRepository entries, EpisodeLogRepository logs,
            MediaLookup mediaLookup, EpisodeLookup episodeLookup) {
        this.entries = entries;
        this.logs = logs;
        this.mediaLookup = mediaLookup;
        this.episodeLookup = episodeLookup;
    }

    public EpisodeLog log(LogEpisodeCommand c) {
        var episode = episodeLookup.season(c.tmdbId(), c.season()).stream()
                .filter(e -> e.number() == c.episode()).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Episode not found"));
        if (!aired(episode))
            throw new IllegalArgumentException("Episode has not aired yet");

        var entry = ensureEntry(c.userId(), c.tmdbId());
        var rating = c.rating() == null ? null : Rating.of(c.rating());
        var log = logs.find(entry.getId(), c.season(), c.episode())
                .map(l -> {
                    l.update(rating, c.watchedOn(), c.review());
                    return l;
                })
                .orElseGet(() -> EpisodeLog.create(entry.getId(), c.season(), c.episode(),
                        rating, c.watchedOn(), c.review()));
        return logs.save(log);
    }

    /**
     * Marca a temporada inteira, ou do episódio 1 até `until`. Pula os já
     * registrados e os que não foram ao ar.
     */
    public List<EpisodeLog> markSeason(UUID userId, int tmdbId, int season, Integer until) {
        int limit = until == null ? Integer.MAX_VALUE : until;
        var airedEpisodes = episodeLookup.season(tmdbId, season).stream()
                .filter(e -> e.number() <= limit && aired(e)).toList();
        if (airedEpisodes.isEmpty())
            return progress(userId, tmdbId);

        var entry = ensureEntry(userId, tmdbId);
        var done = logs.findByEntry(entry.getId()).stream()
                .filter(l -> l.getSeasonNumber() == season)
                .map(EpisodeLog::getEpisodeNumber).collect(Collectors.toSet());
        logs.saveAll(airedEpisodes.stream()
                .filter(e -> !done.contains(e.number()))
                .map(e -> EpisodeLog.create(entry.getId(), season, e.number(), null, null, null))
                .toList());
        return logs.findByEntry(entry.getId());
    }

    public List<EpisodeLog> progress(UUID userId, int tmdbId) {
        return entries.find(new UserId(userId), tmdbId, MediaFormat.TV)
                .map(e -> logs.findByEntry(e.getId())).orElse(List.of());
    }

    public void remove(UUID userId, int tmdbId, int season, int episode) {
        var entry = entries.find(new UserId(userId), tmdbId, MediaFormat.TV)
                .orElseThrow(EntryNotFoundException::new);
        var log = logs.find(entry.getId(), season, episode).orElseThrow(EntryNotFoundException::new);
        logs.delete(log.getId());
    }

    // Quem estava na watchlist passa a assistindo. Quem não estava em lista entra
    // como assistindo.
    private LibraryEntry ensureEntry(UUID userId, int tmdbId) {
        var uid = new UserId(userId);
        var existing = entries.find(uid, tmdbId, MediaFormat.TV);
        if (existing.isPresent()) {
            var entry = existing.get();
            if (entry.getStatus() != EntryStatus.WATCHLIST)
                return entry;
            entry.startWatching();
            return entries.save(entry);
        }
        return entries.save(LibraryEntry.watching(uid, mediaLookup.find(MediaFormat.TV, tmdbId)));
    }

    // Sem data na TMDB, deixa passar. Um dia de folga para fusos à frente do UTC.
    private static boolean aired(EpisodeLookup.Episode e) {
        return e.airDate() == null || !e.airDate().isAfter(LocalDate.now(ZoneOffset.UTC).plusDays(1));
    }

    public void markAllSeasons(UUID entryId, int tmdbId) {
        var seen = logs.findByEntry(entryId).stream()
                .map(l -> episodeKey(l.getSeasonNumber(), l.getEpisodeNumber()))
                .collect(Collectors.toCollection(HashSet::new));
        var fresh = new ArrayList<EpisodeLog>();
        for (var season : episodeLookup.seasonNumbers(tmdbId)) {
            if (season <= 0)
                continue;
            for (var e : episodeLookup.season(tmdbId, season))
                if (aired(e) && seen.add(episodeKey(season, e.number())))
                    fresh.add(EpisodeLog.create(entryId, season, e.number(), null, null, null));
        }
        logs.saveAll(fresh);
    }

    private static String episodeKey(int season, int episode) {
        return season + ":" + episode;
    }
}
