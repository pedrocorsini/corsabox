package com.corsini.corsabox.library.domain;

import java.util.UUID;

public record PublishedReview(LibraryEntry entry, UUID authorId, String username, String firstName, String lastName) {}
