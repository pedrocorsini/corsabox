package com.corsini.corsabox.catalog.api.response;

import com.corsini.corsabox.catalog.application.port.WatchProvider;

public record WatchProviderResponse(String name, String logoUrl) {
    public static WatchProviderResponse from(WatchProvider provider) {
        return new WatchProviderResponse(provider.name(), provider.logoUrl());
    }
}
