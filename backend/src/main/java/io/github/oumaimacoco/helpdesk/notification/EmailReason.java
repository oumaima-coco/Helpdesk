package io.github.oumaimacoco.helpdesk.notification;

enum EmailReason {
    TICKET_HANDLED,
    ASSIGNED_TO_YOU,
    WORK_STARTED,
    INFO_REQUESTED,
    REQUESTER_REPLIED,
    RESOLVED,
    CONFIRMED,
    REOPENED,
    WAITING_FOR_AGENT,
    ESCALATED_TO_TEAM,
    OVERDUE_AGENT,
    OVERDUE_TEAM,
    NOT_PICKED_UP
}