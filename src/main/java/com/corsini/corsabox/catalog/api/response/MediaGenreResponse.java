package com.corsini.corsabox.catalog.api.response;

import com.corsini.corsabox.catalog.domain.MediaGenre;

public record MediaGenreResponse(int id, String name) {
    public static MediaGenreResponse from(MediaGenre genre){
        return new MediaGenreResponse(genre.id(), genre.name());
    }
}

