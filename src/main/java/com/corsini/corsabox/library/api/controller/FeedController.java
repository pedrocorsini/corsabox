package com.corsini.corsabox.library.api.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

import com.corsini.corsabox.library.api.response.LibraryEntryResponse;
import com.corsini.corsabox.library.application.service.ListFeedUseCase;
import com.corsini.corsabox.library.application.service.ReviewInteractionService;
import com.corsini.corsabox.library.domain.Reaction;
import com.corsini.corsabox.library.domain.ReactionSummary;

@RestController
@RequestMapping("/api/feed")
public class FeedController {
    private final ListFeedUseCase listFeed;
    private final ReviewInteractionService interactions;

    public FeedController(ListFeedUseCase listFeed, ReviewInteractionService interactions) {
        this.listFeed = listFeed;
        this.interactions = interactions;
    }

    public record Author(UUID id, String username, String firstName, String lastName) {}
    public record Episode(int season, int episode) {}
    public record Review(Author author, LibraryEntryResponse entry, ReactionSummary reactions, long commentCount,
                         Episode episode) {}
    public record CommentAuthor(UUID id, String username) {}
    public record CommentResponse(UUID id, CommentAuthor author, String text, Instant createdAt, ReactionSummary reactions) {
        static CommentResponse from(ReviewInteractionService.Comment value) {
            var c = value.comment();
            return new CommentResponse(c.id(), new CommentAuthor(c.authorId(), c.username()), c.text(), c.createdAt(), value.reactions());
        }
    }
    public record CommentRequest(@NotBlank @Size(max = 2000) String text) {}
    public record ReactionRequest(@NotNull Reaction reaction) {}

    private static UUID userId(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }

    @GetMapping
    public Page<Review> list(@AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) UUID authorId) {
        return listFeed.execute(userId(jwt), authorId, page, size).map(item -> {
            var review = item.review();
            return new Review(
                new Author(review.authorId(), review.username(), review.firstName(), review.lastName()),
                LibraryEntryResponse.from(review), item.activity().reactions(), item.activity().commentCount(),
                review.episode() == null ? null : new Episode(review.episode().getSeasonNumber(), review.episode().getEpisodeNumber()));
        });
    }

    @GetMapping("/{reviewId}/comments")
    public Page<CommentResponse> comments(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID reviewId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return interactions.comments(reviewId, userId(jwt), page, size).map(CommentResponse::from);
    }

    @PostMapping("/{reviewId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse addComment(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID reviewId,
            @Valid @RequestBody CommentRequest request) {
        return CommentResponse.from(interactions.addComment(reviewId, userId(jwt), request.text()));
    }

    @DeleteMapping("/{reviewId}/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID reviewId, @PathVariable UUID commentId) {
        interactions.deleteComment(reviewId, commentId, userId(jwt));
    }

    @PutMapping("/{reviewId}/reaction")
    public ReactionSummary react(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID reviewId,
            @Valid @RequestBody ReactionRequest request) {
        return interactions.react(reviewId, null, userId(jwt), request.reaction());
    }

    @DeleteMapping("/{reviewId}/reaction")
    public ReactionSummary removeReaction(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID reviewId) {
        return interactions.react(reviewId, null, userId(jwt), null);
    }

    @PutMapping("/{reviewId}/comments/{commentId}/reaction")
    public ReactionSummary reactToComment(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID reviewId,
            @PathVariable UUID commentId, @Valid @RequestBody ReactionRequest request) {
        return interactions.react(reviewId, commentId, userId(jwt), request.reaction());
    }

    @DeleteMapping("/{reviewId}/comments/{commentId}/reaction")
    public ReactionSummary removeCommentReaction(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID reviewId,
            @PathVariable UUID commentId) {
        return interactions.react(reviewId, commentId, userId(jwt), null);
    }
}
