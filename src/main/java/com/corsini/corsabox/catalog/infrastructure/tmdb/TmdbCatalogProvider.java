package com.corsini.corsabox.catalog.infrastructure.tmdb;

import java.net.URI;
import java.net.http.HttpClient;
import java.text.Collator;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;

import com.corsini.corsabox.catalog.application.port.CatalogProvider;
import com.corsini.corsabox.catalog.application.port.WatchProvider;
import com.corsini.corsabox.catalog.domain.BrowseCategory;
import com.corsini.corsabox.catalog.domain.BrowseSort;
import com.corsini.corsabox.catalog.domain.CatalogItem;
import com.corsini.corsabox.catalog.domain.EpisodeItem;
import com.corsini.corsabox.catalog.domain.MediaDetails;
import com.corsini.corsabox.catalog.domain.MediaFormat;
import com.corsini.corsabox.catalog.domain.MediaGenre;
import com.corsini.corsabox.catalog.domain.MediaKind;
import com.corsini.corsabox.catalog.domain.SeasonDetails;
import com.corsini.corsabox.catalog.domain.SeasonSummary;
import com.corsini.corsabox.catalog.domain.TrendingWindow;
import com.corsini.corsabox.catalog.domain.WatchAvailability;
import com.corsini.corsabox.catalog.domain.exception.CatalogItemNotFoundException;
import com.corsini.corsabox.catalog.domain.exception.CatalogUnavailableException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@Component
public class TmdbCatalogProvider implements CatalogProvider {
    private static final Logger log = LoggerFactory.getLogger(TmdbCatalogProvider.class);
    private static final String IMAGE_BASE = "https://image.tmdb.org/t/p";
    private static final int ANIMATION_GENRE = 16;
    private static final Set<String> WRITER_JOBS = Set.of("Screenplay", "Writer", "Story");

    private final RestClient http;
    private final String language;
    private final TmdbCinemaStatusProvider cinemaStatus;

    public TmdbCatalogProvider(@Value("${app.tmdb.base-url}") String baseUrl,
            @Value("${app.tmdb.token}") String token,
            @Value("${app.tmdb.language}") String language, TmdbCinemaStatusProvider cinemaStatus) {
        var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        var factory = new JdkClientHttpRequestFactory(client);
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.http = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        this.language = language;
        this.cinemaStatus = cinemaStatus;
        log.info("TMDB token length = {}", token.length());
    }

    // Listas

    @Override
    @Cacheable(cacheNames = "tmdb-genres", key = "#format")
    public List<MediaGenre> genres(MediaFormat format) {
        var path = switch (format) {
            case MOVIE -> "/genre/movie/list";
            case TV -> "/genre/tv/list";
        };

        var response = http.get()
                .uri(u -> u.path(path)
                        .queryParam("language", language)
                        .build())
                .retrieve()
                .body(TmdbGenreList.class);

        return response == null || response.genres() == null
                ? List.of()
                : response.genres().stream()
                        .map(g -> new MediaGenre(g.id(), g.name()))
                        .toList();
    }

    @Override
    @Cacheable(cacheNames = "tmdb-trending", key = "#window")
    public List<CatalogItem> trending(TrendingWindow window) {
        return fetch(u -> u.path("/trending/all/{w}")
                .queryParam("language", language)
                .build(window.path()));
    }

    @Override
    @Cacheable(cacheNames = "tmdb-search", key = "#query + ':' + #page")
    public List<CatalogItem> search(String query, int page) {
        return fetch(u -> u.path("/search/multi")
                .queryParam("query", "{q}")
                .queryParam("page", page)
                .queryParam("include_adult", false)
                .queryParam("language", language)
                .build(query));
    }

    @Override
    @Cacheable(cacheNames = "tmdb-browse", key = "#category + ':' + #sort + ':' + #page + ':' + #genreIds")
    public List<CatalogItem> browse(BrowseCategory category, BrowseSort sort, int page, List<Integer> genreIds) {
        var results = switch (category) {
            case MOVIES -> discover(MediaFormat.MOVIE, sort, page, false, genreIds);
            case TV -> discover(MediaFormat.TV, sort, page, false, genreIds);
            case ANIME -> mergeSorted(discover(MediaFormat.MOVIE, sort, page, true, genreIds),
                    discover(MediaFormat.TV, sort, page, true, genreIds), sort);
        };
        var wantAnime = category == BrowseCategory.ANIME;
        return results.stream()
                .map(this::toItem)
                .flatMap(Optional::stream)
                .filter(i -> wantAnime == (i.kind() == MediaKind.ANIME))
                .toList();
    }

    private List<TmdbResult> discover(MediaFormat format, BrowseSort sort, int page, boolean animeOnly,
            List<Integer> genreIds) {
        var sortBy = switch (sort) {
            case POPULARITY -> "popularity.desc";
            case TITLE -> format == MediaFormat.MOVIE ? "title.asc" : "name.asc";
        };
        return fetchResults(u -> {
            var b = u.path("/discover/{type}")
                    .queryParam("language", language)
                    .queryParam("sort_by", sortBy)
                    .queryParam("page", page)
                    .queryParam("include_adult", false);
            // Sem este piso, a ordem alfabética começa por títulos obscuros sem pôster.
            String genres = null;
            if (sort == BrowseSort.TITLE)
                b = b.queryParam("vote_count.gte", 100);
            var selectedGenreIds = genreIds == null ? List.<Integer>of() : genreIds;
            var requiredGenreIds = new ArrayList<Integer>();
            if (animeOnly) requiredGenreIds.add(ANIMATION_GENRE);
            requiredGenreIds.addAll(selectedGenreIds);
            if (!requiredGenreIds.isEmpty()) {
                genres = requiredGenreIds.stream().distinct().map(String::valueOf)
                        .collect(java.util.stream.Collectors.joining(","));
            }

            if (genres != null) {
                b = b.queryParam("with_genres", genres);
            }

            if (animeOnly) {
                b = b.queryParam("with_original_language", "ja");
            }
            return b.build(format.path());
        }).stream().map(r -> r.withMediaType(format.path())).toList();
    }

    private static List<TmdbResult> mergeSorted(List<TmdbResult> a, List<TmdbResult> b, BrowseSort sort) {
        var all = new ArrayList<>(a);
        all.addAll(b);
        Comparator<TmdbResult> order;
        if (sort == BrowseSort.POPULARITY) {
            order = Comparator.comparingDouble(
                    (TmdbResult r) -> r.popularity() == null ? 0 : r.popularity()).reversed();
        } else {
            var collator = Collator.getInstance(Locale.forLanguageTag("pt-BR"));
            order = Comparator.comparing(TmdbResult::label, collator);
        }
        all.sort(order);
        return all;
    }

    // Detalhes

    @Override
    @Cacheable(cacheNames = "tmdb-details", key = "#format + ':' + #id")
    public MediaDetails details(MediaFormat format, int id) {
        try {
            var movie = format == MediaFormat.MOVIE;
            var append = movie ? "credits,release_dates,videos" : "credits,videos";
            var d = http.get()
                    .uri(u -> u.path("/{type}/{id}")
                            .queryParam("language", language)
                            .queryParam("include_video_language", "pt,en,null")
                            .queryParam("append_to_response", append)
                            .build(format.path(), id))
                    .retrieve()
                    .body(TmdbDetail.class);
            if (d == null)
                throw new CatalogItemNotFoundException(format, id);

            var genreIds = d.genres() == null
                    ? List.<Integer>of()
                    : d.genres().stream().map(TmdbGenre::id).toList();
            var result = new TmdbResult(d.id(), format.path(), d.title(), d.name(), d.overview(),
                    d.releaseDate(), d.firstAirDate(), d.posterPath(), d.backdropPath(),
                    d.originalLanguage(), genreIds, null);
            var item = toItem(result).orElseThrow(() -> new CatalogItemNotFoundException(format, id));

            var crew = d.credits() == null || d.credits().crew() == null
                    ? List.<TmdbCrew>of()
                    : d.credits().crew();
            var cast = d.credits() == null || d.credits().cast() == null
                    ? List.<TmdbCast>of()
                    : d.credits().cast();

            var directors = movie
                    ? names(crew.stream().filter(c -> "Director".equals(c.job())).map(TmdbCrew::name), 3)
                    : List.<String>of();
            var writers = movie
                    ? names(crew.stream().filter(c -> WRITER_JOBS.contains(c.job())).map(TmdbCrew::name), 4)
                    : List.<String>of();
            var creators = d.createdBy() == null
                    ? List.<String>of()
                    : names(d.createdBy().stream().map(TmdbPerson::name), 3);
            var castNames = names(cast.stream()
                    .sorted(Comparator.comparingInt(
                            (TmdbCast c) -> c.order() == null ? Integer.MAX_VALUE : c.order()))
                    .map(TmdbCast::name), 8);
            var genres = d.genres() == null
                    ? List.<String>of()
                    : names(d.genres().stream().map(TmdbGenre::name), 3);

            Integer runtime = movie
                    ? positive(d.runtime())
                    : (d.episodeRunTime() == null || d.episodeRunTime().isEmpty()
                            ? null
                            : positive(d.episodeRunTime().get(0)));

            // Filme: estreia no Brasil. Sem ela, vale a data original. Série: data de
            // estreia.
            LocalDate released = movie ? brazilRelease(d.releaseDates()) : null;
            if (released == null)
                released = parseDate(movie ? d.releaseDate() : d.firstAirDate());
            var worldwideRelease = movie ? firstTheatricalRelease(d.releaseDates(), null) : null;
            if (worldwideRelease == null && movie)
                worldwideRelease = parseDate(d.releaseDate());
            var brazilTheatricalRelease = movie ? firstTheatricalRelease(d.releaseDates(), "BR") : null;
            var seasons = movie || d.seasons() == null
                    ? List.<SeasonSummary>of()
                    : d.seasons().stream()
                            .filter(s -> s.episodeCount() > 0)
                            .sorted(Comparator.comparingInt(TmdbSeasonStub::number))
                            .map(s -> new SeasonSummary(s.number(), text(s.name()), s.episodeCount(),
                                    parseDate(s.airDate())))
                            .toList();
            return new MediaDetails(item, released, worldwideRelease, brazilTheatricalRelease, runtime,
                    genres, directors, writers, creators, castNames,
                    trailerKey(d.videos()), seasons);
        } catch (HttpClientErrorException.NotFound e) {
            throw new CatalogItemNotFoundException(format, id);
        } catch (RestClientException e) {
            log.error("Falha ao consultar a TMDB", e);
            throw new CatalogUnavailableException(e);
        }
    }

    @Override
    @Cacheable(cacheNames = "tmdb-watch-providers", key = "#format + ':' + #id")
    public WatchAvailability watchAvailability(MediaFormat format, int id) {
        try {
            var response = http.get()
                    .uri(u -> u.path("/{type}/{id}/watch/providers").build(format.path(), id))
                    .retrieve()
                    .body(TmdbWatchProviderResults.class);
            var brazil = response == null || response.results() == null ? null : response.results().get("BR");
            var inTheaters = format == MediaFormat.MOVIE && cinemaStatus.currentlyPlayingInBrazil().contains(id);
            if (brazil == null)
                return new WatchAvailability(null, inTheaters, List.of(), List.of(), List.of(), List.of(), List.of());
            return new WatchAvailability(brazil.link(), inTheaters, providers(brazil.flatrate()), providers(brazil.free()),
                    providers(brazil.ads()), providers(brazil.rent()), providers(brazil.buy()));
        } catch (HttpClientErrorException.NotFound e) {
            var inTheaters = format == MediaFormat.MOVIE && cinemaStatus.currentlyPlayingInBrazil().contains(id);
            return new WatchAvailability(null, inTheaters, List.of(), List.of(), List.of(), List.of(), List.of());
        } catch (RestClientException e) {
            log.error("Falha ao consultar provedores de streaming na TMDB", e);
            throw new CatalogUnavailableException(e);
        }
    }

    private static List<WatchProvider> providers(List<TmdbWatchProvider> providers) {
        return providers == null ? List.of() : providers.stream()
                .filter(p -> p.providerName() != null && !p.providerName().isBlank())
                .sorted(Comparator.comparingInt(p -> p.displayPriority() == null ? Integer.MAX_VALUE : p.displayPriority()))
                .map(p -> new WatchProvider(p.providerName(), image(p.logoPath(), "w92")))
                .toList();
    }

    /**
     * Prefere estreia em cinemas (tipo 3), depois estreia limitada (tipo 2), e a
     * data mais antiga.
     */
    private static LocalDate brazilRelease(TmdbReleaseDates rd) {
        if (rd == null || rd.results() == null)
            return null;
        return rd.results().stream()
                .filter(c -> "BR".equals(c.country()) && c.releaseDates() != null)
                .flatMap(c -> c.releaseDates().stream())
                .filter(r -> r.releaseDate() != null && r.releaseDate().length() >= 10)
                .sorted(Comparator.comparingInt((TmdbRelease r) -> r.type() == 3 ? 0 : r.type() == 2 ? 1 : 2)
                        .thenComparing(TmdbRelease::releaseDate))
                .map(r -> parseDate(r.releaseDate()))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private static LocalDate firstTheatricalRelease(TmdbReleaseDates rd, String country) {
        if (rd == null || rd.results() == null)
            return null;
        return rd.results().stream()
                .filter(c -> country == null || country.equals(c.country()))
                .filter(c -> c.releaseDates() != null)
                .flatMap(c -> c.releaseDates().stream())
                .filter(r -> country == null
                        ? r.type() == 1 || r.type() == 2 || r.type() == 3
                        : r.type() == 2 || r.type() == 3)
                .map(TmdbRelease::releaseDate)
                .map(TmdbCatalogProvider::parseDate)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);
    }

    // Apoio

    @Override
    @Cacheable(cacheNames = "tmdb-season", key = "#tvId + ':' + #season")
    public SeasonDetails season(int tvId, int season) {
        try {
            var s = http.get()
                    .uri(u -> u.path("/tv/{id}/season/{n}")
                            .queryParam("language", language)
                            .build(tvId, season))
                    .retrieve()
                    .body(TmdbSeasonDetail.class);
            if (s == null)
                throw new CatalogItemNotFoundException(MediaFormat.TV, tvId);
            var episodes = s.episodes() == null ? List.<EpisodeItem>of()
                    : s.episodes().stream()
                            .map(e -> new EpisodeItem(e.number(), text(e.name()), text(e.overview()),
                                    parseDate(e.airDate()), positive(e.runtime()), image(e.stillPath(), "w300")))
                            .toList();
            return new SeasonDetails(s.number(), text(s.name()), episodes);
        } catch (HttpClientErrorException.NotFound e) {
            throw new CatalogItemNotFoundException(MediaFormat.TV, tvId);
        } catch (RestClientException e) {
            log.error("Falha ao consultar a TMDB", e);
            throw new CatalogUnavailableException(e);
        }
    }

    private static String trailerKey(TmdbVideos videos) {
        if (videos == null || videos.results() == null)
            return null;
        return videos.results().stream()
                .filter(v -> "YouTube".equals(v.site())
                        && ("Trailer".equals(v.type()) || "Teaser".equals(v.type()))
                        && v.key() != null && !v.key().isBlank())
                .min(Comparator
                        .comparingInt((TmdbVideo v) -> "Trailer".equals(v.type()) ? 0 : 1)
                        .thenComparingInt(v -> "pt".equals(v.language()) ? 0 : 1)
                        .thenComparingInt(v -> Boolean.TRUE.equals(v.official()) ? 0 : 1))
                .map(TmdbVideo::key)
                .orElse(null);
    }

    private List<TmdbResult> fetchResults(Function<UriBuilder, URI> uri) {
        try {
            var body = http.get().uri(uri).retrieve().body(TmdbPage.class);
            return body == null || body.results() == null ? List.of() : body.results();
        } catch (RestClientException e) {
            log.error("Falha ao consultar a TMDB", e);
            throw new CatalogUnavailableException(e);
        }
    }

    private List<CatalogItem> fetch(Function<UriBuilder, URI> uri) {
        return fetchResults(uri).stream().map(this::toItem).flatMap(Optional::stream).toList();
    }

    private Optional<CatalogItem> toItem(TmdbResult r) {
        var movie = "movie".equals(r.mediaType());
        if (!movie && !"tv".equals(r.mediaType()))
            return Optional.empty(); // ignora pessoas

        var title = movie ? r.title() : r.name();
        if (title == null || title.isBlank())
            return Optional.empty();

        var kind = isAnime(r) ? MediaKind.ANIME : movie ? MediaKind.MOVIE : MediaKind.TV;
        var date = movie ? r.releaseDate() : r.firstAirDate();
        var format = movie ? MediaFormat.MOVIE : MediaFormat.TV;
        return Optional.of(new CatalogItem(r.id(), title, kind, format, year(date), text(r.overview()),
                image(r.posterPath(), "w500"), image(r.backdropPath(), "w1280")));
    }

    private static boolean isAnime(TmdbResult r) {
        return "ja".equals(r.originalLanguage())
                && r.genreIds() != null
                && r.genreIds().contains(ANIMATION_GENRE);
    }

    private static List<String> names(Stream<String> stream, int limit) {
        return stream.filter(n -> n != null && !n.isBlank()).distinct().limit(limit).toList();
    }

    private static Integer positive(Integer value) {
        return value != null && value > 0 ? value : null;
    }

    private static String text(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }

    private static Integer year(String date) {
        if (date == null || date.length() < 4)
            return null;
        try {
            return Integer.valueOf(date.substring(0, 4));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static LocalDate parseDate(String s) {
        if (s == null || s.length() < 10)
            return null;
        try {
            return LocalDate.parse(s.substring(0, 10));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String image(String path, String size) {
        return path == null ? null : IMAGE_BASE + "/" + size + path;
    }

    // Respostas da TMDB

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TmdbPage(List<TmdbResult> results) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TmdbResult(
            int id,
            @JsonProperty("media_type") String mediaType,
            String title,
            String name,
            String overview,
            @JsonProperty("release_date") String releaseDate,
            @JsonProperty("first_air_date") String firstAirDate,
            @JsonProperty("poster_path") String posterPath,
            @JsonProperty("backdrop_path") String backdropPath,
            @JsonProperty("original_language") String originalLanguage,
            @JsonProperty("genre_ids") List<Integer> genreIds,
            Double popularity) {

        TmdbResult withMediaType(String type) {
            return new TmdbResult(id, type, title, name, overview, releaseDate, firstAirDate,
                    posterPath, backdropPath, originalLanguage, genreIds, popularity);
        }

        String label() {
            var t = "movie".equals(mediaType) ? title : name;
            return t == null ? "" : t;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TmdbGenre(int id, String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TmdbPerson(String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TmdbCast(String name, Integer order) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TmdbCrew(String name, String job) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TmdbCredits(List<TmdbCast> cast, List<TmdbCrew> crew) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TmdbRelease(int type, @JsonProperty("release_date") String releaseDate) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TmdbCountryReleases(@JsonProperty("iso_3166_1") String country,
            @JsonProperty("release_dates") List<TmdbRelease> releaseDates) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TmdbReleaseDates(List<TmdbCountryReleases> results) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TmdbDetail(
            int id,
            String title,
            String name,
            String overview,
            @JsonProperty("release_date") String releaseDate,
            @JsonProperty("first_air_date") String firstAirDate,
            @JsonProperty("poster_path") String posterPath,
            @JsonProperty("backdrop_path") String backdropPath,
            @JsonProperty("original_language") String originalLanguage,
            List<TmdbGenre> genres,
            Integer runtime,
            @JsonProperty("episode_run_time") List<Integer> episodeRunTime,
            @JsonProperty("created_by") List<TmdbPerson> createdBy,
            TmdbCredits credits,
            @JsonProperty("release_dates") TmdbReleaseDates releaseDates,
            List<TmdbSeasonStub> seasons,
            TmdbVideos videos) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TmdbVideo(String key, String site, String type, Boolean official,
            @JsonProperty("iso_639_1") String language) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TmdbVideos(List<TmdbVideo> results) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TmdbSeasonStub(@JsonProperty("season_number") int number, String name,
            @JsonProperty("episode_count") int episodeCount,
            @JsonProperty("air_date") String airDate) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TmdbEpisode(@JsonProperty("episode_number") int number, String name, String overview,
            @JsonProperty("air_date") String airDate, Integer runtime,
            @JsonProperty("still_path") String stillPath) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TmdbSeasonDetail(@JsonProperty("season_number") int number, String name,
            List<TmdbEpisode> episodes) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TmdbGenreList(List<TmdbGenre> genres) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TmdbWatchProviderResults(Map<String, TmdbCountryWatchProviders> results) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TmdbCountryWatchProviders(String link, List<TmdbWatchProvider> flatrate, List<TmdbWatchProvider> free,
            List<TmdbWatchProvider> ads, List<TmdbWatchProvider> rent, List<TmdbWatchProvider> buy) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TmdbWatchProvider(@JsonProperty("provider_name") String providerName,
            @JsonProperty("logo_path") String logoPath,
            @JsonProperty("display_priority") Integer displayPriority) {}
}
