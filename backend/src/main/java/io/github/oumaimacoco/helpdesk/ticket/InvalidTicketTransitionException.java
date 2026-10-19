package io.github.oumaimacoco.helpdesk.ticket;

public class InvalidTicketTransitionException extends RuntimeException {

    public InvalidTicketTransitionException(String message) {
        super(message);
    }
}