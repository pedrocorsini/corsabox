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
import com.corsini.corsabox.catalog.domain.BrowseCategory;
import com.corsini.corsabox.catalog.domain.BrowseSort;
import com.corsini.corsabox.catalog.domain.CatalogItem;
import com.corsini.corsabox.catalog.domain.MediaDetails;
import com.corsini.corsabox.catalog.domain.MediaFormat;
import com.corsini.corsabox.catalog.domain.MediaKind;
import com.corsini.corsabox.catalog.domain.TrendingWindow;
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

    public TmdbCatalogProvider(@Value("${app.tmdb.base-url}") String baseUrl,
                               @Value("${app.tmdb.token}") String token,
                               @Value("${app.tmdb.language}") String language) {
        var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        var factory = new JdkClientHttpRequestFactory(client);
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.http = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        this.language = language;
        log.info("TMDB token length = {}", token.length());
    }

    // Listas

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
    @Cacheable(cacheNames = "tmdb-browse", key = "#category + ':' + #sort + ':' + #page")
    public List<CatalogItem> browse(BrowseCategory category, BrowseSort sort, int page) {
        var results = switch (category) {
            case MOVIES -> discover(MediaFormat.MOVIE, sort, page, false);
            case TV -> discover(MediaFormat.TV, sort, page, false);
            case ANIME -> mergeSorted(discover(MediaFormat.MOVIE, sort, page, true),
                                      discover(MediaFormat.TV, sort, page, true), sort);
        };
        var wantAnime = category == BrowseCategory.ANIME;
        return results.stream()
                .map(this::toItem)
                .flatMap(Optional::stream)
                .filter(i -> wantAnime == (i.kind() == MediaKind.ANIME))
                .toList();
    }

    private List<TmdbResult> discover(MediaFormat format, BrowseSort sort, int page, boolean animeOnly) {
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
            if (sort == BrowseSort.TITLE) b = b.queryParam("vote_count.gte", 100);
            if (animeOnly) b = b.queryParam("with_genres", ANIMATION_GENRE)
                                .queryParam("with_original_language", "ja");
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
            var append = movie ? "credits,release_dates" : "credits";
            var d = http.get()
                    .uri(u -> u.path("/{type}/{id}")
                            .queryParam("language", language)
                            .queryParam("append_to_response", append)
                            .build(format.path(), id))
                    .retrieve()
                    .body(TmdbDetail.class);
            if (d == null) throw new CatalogItemNotFoundException(format, id);

            var genreIds = d.genres() == null
                    ? List.<Integer>of()
                    : d.genres().stream().map(TmdbGenre::id).toList();
            var result = new TmdbResult(d.id(), format.path(), d.title(), d.name(), d.overview(),
                    d.releaseDate(), d.firstAirDate(), d.posterPath(), d.backdropPath(),
                    d.originalLanguage(), genreIds, null);
            var item = toItem(result).orElseThrow(() -> new CatalogItemNotFoundException(format, id));

            var crew = d.credits() == null || d.credits().crew() == null
                    ? List.<TmdbCrew>of() : d.credits().crew();
            var cast = d.credits() == null || d.credits().cast() == null
                    ? List.<TmdbCast>of() : d.credits().cast();

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
                            ? null : positive(d.episodeRunTime().get(0)));

            // Filme: estreia no Brasil. Sem ela, vale a data original. Série: data de estreia.
            LocalDate released = movie ? brazilRelease(d.releaseDates()) : null;
            if (released == null) released = parseDate(movie ? d.releaseDate() : d.firstAirDate());

            return new MediaDetails(item, released, runtime, genres, directors, writers, creators, castNames);
        } catch (HttpClientErrorException.NotFound e) {
            throw new CatalogItemNotFoundException(format, id);
        } catch (RestClientException e) {
            log.error("Falha ao consultar a TMDB", e);
            throw new CatalogUnavailableException(e);
        }
    }

    /** Prefere estreia em cinemas (tipo 3), depois estreia limitada (tipo 2), e a data mais antiga. */
    private static LocalDate brazilRelease(TmdbReleaseDates rd) {
        if (rd == null || rd.results() == null) return null;
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

    // Apoio

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
        if (!movie && !"tv".equals(r.mediaType())) return Optional.empty(); // ignora pessoas

        var title = movie ? r.title() : r.name();
        if (title == null || title.isBlank()) return Optional.empty();

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
        if (date == null || date.length() < 4) return null;
        try {
            return Integer.valueOf(date.substring(0, 4));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static LocalDate parseDate(String s) {
        if (s == null || s.length() < 10) return null;
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
    record TmdbPage(List<TmdbResult> results) {}

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
    record TmdbGenre(int id, String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TmdbPerson(String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TmdbCast(String name, Integer order) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TmdbCrew(String name, String job) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TmdbCredits(List<TmdbCast> cast, List<TmdbCrew> crew) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TmdbRelease(int type, @JsonProperty("release_date") String releaseDate) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TmdbCountryReleases(@JsonProperty("iso_3166_1") String country,
                               @JsonProperty("release_dates") List<TmdbRelease> releaseDates) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TmdbReleaseDates(List<TmdbCountryReleases> results) {}

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
            @JsonProperty("release_dates") TmdbReleaseDates releaseDates) {}
}