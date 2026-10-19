package io.github.oumaimacoco.helpdesk.ticket;

import io.github.oumaimacoco.helpdesk.common.BusinessRuleException;

public class InvalidTicketTransitionException extends BusinessRuleException {

    public InvalidTicketTransitionException(String message) {
        super(message);
    }
}