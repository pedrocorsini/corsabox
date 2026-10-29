package com.corsini.corsabox.identity.infrastructure.persistence;

import jakarta.persistence.*;
import com.corsini.corsabox.catalog.domain.*;

@Embeddable
public class FavoriteMedia {
    @Column(nullable = false) private int mediaId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private MediaFormat format;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private MediaKind kind;
    @Column(nullable = false, length = 1000) private String title;
    private Integer releaseYear;
    @Column(length = 1000) private String posterUrl;
    protected FavoriteMedia() {}
    FavoriteMedia(CatalogItem item) {
        mediaId = item.id(); format = item.format(); kind = item.kind(); title = item.title();
        releaseYear = item.year(); posterUrl = item.posterUrl();
    }
    CatalogItem toDomain() {
        return new CatalogItem(mediaId, title, kind, format, releaseYear, null, posterUrl, null);
    }
}
