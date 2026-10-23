package io.github.oumaimacoco.helpdesk.notification;

import io.github.oumaimacoco.helpdesk.ticket.Ticket;
import io.github.oumaimacoco.helpdesk.ticket.TicketChanged;
import io.github.oumaimacoco.helpdesk.user.AppUser;
import io.github.oumaimacoco.helpdesk.user.AppUserRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class TicketEmailNotifier {

    private final EmailOutbox outbox;
    private final AppUserRepository users;
    private final String appUrl;

    public TicketEmailNotifier(EmailOutbox outbox, AppUserRepository users,
                               @Value("${helpdesk.mail.app-url}") String appUrl) {
        this.outbox = outbox;
        this.users = users;
        this.appUrl = appUrl;
    }

    @EventListener
    public void onTicketChanged(TicketChanged change) {
        Ticket ticket = change.ticket();
        Map<Long, Recipient> recipients = new LinkedHashMap<>();
        switch (change.type()) {
            case TAKEN -> add(recipients, ticket.getRequester(), EmailReason.TICKET_HANDLED);
            case ASSIGNED -> {
                add(recipients, ticket.getAssignee(), EmailReason.ASSIGNED_TO_YOU);
                add(recipients, ticket.getRequester(), EmailReason.TICKET_HANDLED);
            }
            case INFO_REQUESTED -> add(recipients, ticket.getRequester(), EmailReason.INFO_REQUESTED);
            case RESOLVED -> add(recipients, ticket.getRequester(), EmailReason.RESOLVED);
            case ESCALATED -> {
                add(recipients, ticket.getRequester(), EmailReason.WAITING_FOR_AGENT);
                addTeamLeads(recipients, ticket, EmailReason.ESCALATED_TO_TEAM);
            }
            case OVERDUE -> {
                add(recipients, change.previousAssignee(), EmailReason.OVERDUE_AGENT);
                add(recipients, ticket.getRequester(), EmailReason.WAITING_FOR_AGENT);
                addTeamLeads(recipients, ticket, EmailReason.OVERDUE_TEAM);
            }
            case PICKUP_OVERDUE -> addTeamLeads(recipients, ticket, EmailReason.NOT_PICKED_UP);
            default -> {
            }
        }
        if (change.actor() != null) {
            recipients.remove(change.actor().getId());
        }
        String link = appUrl + "/tickets/" + ticket.getId();
        recipients.values().forEach(recipient -> {
            AppUser user = recipient.user();
            String language = user.getPreferredLanguage();
            outbox.queue(
                    user.getEmail(),
                    EmailTemplates.subject(language, recipient.reason(), ticket.getReference()),
                    EmailTemplates.body(language, recipient.reason(), user.getFullName(), ticket.getReference(), link));
        });
    }

    private void addTeamLeads(Map<Long, Recipient> recipients, Ticket ticket, EmailReason reason) {
        users.findByTeamAndTeamLeadTrue(ticket.getCategory().getTeam())
                .forEach(lead -> add(recipients, lead, reason));
    }

    private static void add(Map<Long, Recipient> recipients, AppUser user, EmailReason reason) {
        if (user != null) {
            recipients.putIfAbsent(user.getId(), new Recipient(user, reason));
        }
    }

    private record Recipient(AppUser user, EmailReason reason) {
    }
}