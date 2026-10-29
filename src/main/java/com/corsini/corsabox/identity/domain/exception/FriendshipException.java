package com.corsini.corsabox.identity.domain.exception;

public class FriendshipException extends RuntimeException {
    public enum Reason { SELF_REQUEST, INCOMING_REQUEST, NOT_RECIPIENT, NOT_FOUND }
    private final Reason reason;

    public FriendshipException(Reason reason) {
        super(switch (reason) {
            case SELF_REQUEST -> "Você não pode enviar uma solicitação para si mesmo.";
            case INCOMING_REQUEST -> "Essa pessoa já enviou uma solicitação. Aceite ou recuse o pedido recebido.";
            case NOT_RECIPIENT -> "Somente quem recebeu a solicitação pode aceitá-la.";
            case NOT_FOUND -> "Solicitação de amizade não encontrada.";
        });
        this.reason = reason;
    }

    public Reason getReason() { return reason; }
}
