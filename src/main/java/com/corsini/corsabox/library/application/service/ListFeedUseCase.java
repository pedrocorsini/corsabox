package com.corsini.corsabox.library.application.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.corsini.corsabox.library.domain.LibraryRepository;
import com.corsini.corsabox.library.domain.PublishedReview;
import com.corsini.corsabox.library.domain.ReviewInteractionRepository;
import java.util.UUID;

@Service
public class ListFeedUseCase {
    private final LibraryRepository repository;
    private final ReviewInteractionRepository interactions;

    public ListFeedUseCase(LibraryRepository repository, ReviewInteractionRepository interactions) {
        this.repository = repository;
        this.interactions = interactions;
    }

    public record Item(PublishedReview review, ReviewInteractionRepository.Activity activity) {}

    @Transactional(readOnly = true)
    public Page<Item> execute(UUID viewerId, UUID authorId, int page, int size) {
        var reviews = repository.findPublishedByAuthor(authorId, PageRequest.of(Math.max(0, page), Math.min(50, Math.max(1, size))));
        var activities = interactions.activities(reviews.stream().map(review -> review.entry().getId()).toList(), viewerId);
        return reviews.map(review -> new Item(review, activities.getOrDefault(review.entry().getId(),
                ReviewInteractionRepository.Activity.EMPTY)));
    }
}
