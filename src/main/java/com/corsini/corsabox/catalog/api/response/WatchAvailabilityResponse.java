package com.corsini.corsabox.catalog.api.response;

import java.util.List;

import com.corsini.corsabox.catalog.application.port.WatchProvider;
import com.corsini.corsabox.catalog.domain.WatchAvailability;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record WatchAvailabilityResponse(String link, boolean inTheaters, List<WatchProviderResponse> streaming,
        List<WatchProviderResponse> free, List<WatchProviderResponse> ads,
        List<WatchProviderResponse> rent, List<WatchProviderResponse> buy) {
    public static WatchAvailabilityResponse from(WatchAvailability availability) {
        return new WatchAvailabilityResponse(availability.link(), availability.inTheaters(), map(availability.streaming()), map(availability.free()),
                map(availability.ads()), map(availability.rent()), map(availability.buy()));
    }

    private static List<WatchProviderResponse> map(List<WatchProvider> providers) {
        return providers.stream().map(WatchProviderResponse::from).toList();
    }
}
