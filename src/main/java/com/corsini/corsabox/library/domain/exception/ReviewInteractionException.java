package com.corsini.corsabox.library.domain.exception;

public class ReviewInteractionException extends RuntimeException {
    public enum Reason { NOT_FOUND, COMMENTS_CLOSED, NOT_OWNER }
    private final Reason reason;

    public ReviewInteractionException(Reason reason) {
        super(switch (reason) {
            case NOT_FOUND -> "Publicação ou comentário não encontrado.";
            case COMMENTS_CLOSED -> "O autor desativou novos comentários nesta review.";
            case NOT_OWNER -> "Você só pode excluir seus próprios comentários.";
        });
        this.reason = reason;
    }

    public Reason getReason() { return reason; }
}
