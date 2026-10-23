package com.corsini.corsabox.catalog.infrastructure.tmdb;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;

import com.corsini.corsabox.catalog.application.port.CatalogProvider;
import com.corsini.corsabox.catalog.domain.CatalogItem;
import com.corsini.corsabox.catalog.domain.MediaKind;
import com.corsini.corsabox.catalog.domain.exception.CatalogUnavailableException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@Component
public class TmdbCatalogProvider implements CatalogProvider {
    private static final Logger log = LoggerFactory.getLogger(TmdbCatalogProvider.class);
    private static final String IMAGE_BASE = "https://image.tmdb.org/t/p";
    private static final int ANIMATION_GENRE = 16;

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

    @Override
    @Cacheable("tmdb-trending")
    public List<CatalogItem> trending() {
        return fetch(u -> u.path("/trending/all/week")
                .queryParam("language", language)
                .build());
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

    private List<CatalogItem> fetch(Function<UriBuilder, URI> uri) {
        try {
            var body = http.get().uri(uri).retrieve().body(TmdbPage.class);
            if (body == null || body.results() == null) return List.of();
            return body.results().stream()
                    .map(this::toItem)
                    .flatMap(Optional::stream)
                    .toList();
        } catch (RestClientException e) {
            log.error("Falha ao consultar a TMDB", e);
            throw new CatalogUnavailableException(e);
        }
    }

    private Optional<CatalogItem> toItem(TmdbResult r) {
        var movie = "movie".equals(r.mediaType());
        if (!movie && !"tv".equals(r.mediaType())) return Optional.empty(); // ignora pessoas

        var title = movie ? r.title() : r.name();
        if (title == null || title.isBlank()) return Optional.empty();

        var kind = isAnime(r) ? MediaKind.ANIME : movie ? MediaKind.MOVIE : MediaKind.TV;
        var date = movie ? r.releaseDate() : r.firstAirDate();
        return Optional.of(new CatalogItem(r.id(), title, kind, year(date),
                image(r.posterPath(), "w500"), image(r.backdropPath(), "w1280")));
    }

    private static boolean isAnime(TmdbResult r) {
        return "ja".equals(r.originalLanguage())
                && r.genreIds() != null
                && r.genreIds().contains(ANIMATION_GENRE);
    }

    private static Integer year(String date) {
        if (date == null || date.length() < 4) return null;
        try {
            return Integer.valueOf(date.substring(0, 4));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String image(String path, String size) {
        return path == null ? null : IMAGE_BASE + "/" + size + path;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TmdbPage(List<TmdbResult> results) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TmdbResult(
            int id,
            @JsonProperty("media_type") String mediaType,
            String title,
            String name,
            @JsonProperty("release_date") String releaseDate,
            @JsonProperty("first_air_date") String firstAirDate,
            @JsonProperty("poster_path") String posterPath,
            @JsonProperty("backdrop_path") String backdropPath,
            @JsonProperty("original_language") String originalLanguage,
            @JsonProperty("genre_ids") List<Integer> genreIds) {}
}