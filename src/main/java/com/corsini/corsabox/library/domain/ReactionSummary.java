package com.corsini.corsabox.library.domain;

public record ReactionSummary(long likes, long dislikes, Reaction myReaction) {
    public static final ReactionSummary EMPTY = new ReactionSummary(0, 0, null);
}
