package io.github.oumaimacoco.helpdesk.notification;

enum EmailReason {
    TICKET_HANDLED,
    ASSIGNED_TO_YOU,
    INFO_REQUESTED,
    RESOLVED,
    WAITING_FOR_AGENT,
    ESCALATED_TO_TEAM,
    OVERDUE_AGENT,
    OVERDUE_TEAM,
    NOT_PICKED_UP
}