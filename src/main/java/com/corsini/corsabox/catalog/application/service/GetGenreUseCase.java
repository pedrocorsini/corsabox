package com.corsini.corsabox.catalog.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.corsini.corsabox.catalog.application.port.CatalogProvider;
import com.corsini.corsabox.catalog.domain.MediaFormat;
import com.corsini.corsabox.catalog.domain.MediaGenre;

@Service 
public class GetGenreUseCase {
    private final CatalogProvider provider;

    public GetGenreUseCase(CatalogProvider provider) {
        this.provider = provider;
    }
    
    public List<MediaGenre> execute(MediaFormat format){
        return provider.genres(format);
    }
}
