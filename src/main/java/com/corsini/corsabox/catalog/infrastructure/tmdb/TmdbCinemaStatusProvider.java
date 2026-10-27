package com.corsini.corsabox.catalog.infrastructure.tmdb;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.corsini.corsabox.catalog.domain.exception.CatalogUnavailableException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@Component
public class TmdbCinemaStatusProvider {
    private final RestClient http;
    private final String language;

    public TmdbCinemaStatusProvider(@Value("${app.tmdb.base-url}") String baseUrl,
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
    }

    @Cacheable(cacheNames = "tmdb-now-playing-br", key = "'BR'")
    public Set<Integer> currentlyPlayingInBrazil() {
        try {
            var ids = new HashSet<Integer>();
            var page = 1;
            int totalPages;
            do {
                var currentPage = page;
                var response = http.get()
                        .uri(u -> u.path("/movie/now_playing")
                                .queryParam("language", language)
                                .queryParam("region", "BR")
                                .queryParam("page", currentPage)
                                .build())
                        .retrieve()
                        .body(TmdbNowPlayingPage.class);
                if (response == null)
                    return Set.of();
                if (response.results() != null)
                    response.results().stream().map(TmdbMovieId::id).forEach(ids::add);
                totalPages = Math.min(response.totalPages(), 500);
                page++;
            } while (page <= totalPages);
            return Set.copyOf(ids);
        } catch (RestClientException e) {
            throw new CatalogUnavailableException(e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TmdbNowPlayingPage(java.util.List<TmdbMovieId> results,
            @JsonProperty("total_pages") int totalPages) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TmdbMovieId(int id) {}
}
