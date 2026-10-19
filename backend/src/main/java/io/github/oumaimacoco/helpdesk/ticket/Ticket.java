package io.github.oumaimacoco.helpdesk.ticket;

import io.github.oumaimacoco.helpdesk.category.Category;
import io.github.oumaimacoco.helpdesk.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String reference;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 2)
    private Priority priority;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private AppUser requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private AppUser assignee;

    @Column(columnDefinition = "text")
    private String resolution;

    @Column(name = "escalation_reason", columnDefinition = "text")
    private String escalationReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Ticket() {
    }

    public Ticket(String reference, String title, String description,
                  Priority priority, Category category, AppUser requester) {
        this.reference = reference;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.category = category;
        this.requester = requester;
        this.status = TicketStatus.NEW;
    }

    public void assignTo(AppUser agent) {
        requireStatus("assign", TicketStatus.NEW, TicketStatus.ASSIGNED);
        this.assignee = Objects.requireNonNull(agent, "agent");
        this.status = TicketStatus.ASSIGNED;
    }

    public void start() {
        requireStatus("start", TicketStatus.ASSIGNED);
        this.status = TicketStatus.IN_PROGRESS;
    }

    public void requestInfo() {
        requireStatus("request information on", TicketStatus.IN_PROGRESS);
        this.status = TicketStatus.WAITING_FOR_USER;
    }

    public void provideInfo() {
        requireStatus("provide information on", TicketStatus.WAITING_FOR_USER);
        this.status = TicketStatus.IN_PROGRESS;
    }

    public void resolve(String resolution) {
        requireStatus("resolve", TicketStatus.IN_PROGRESS);
        this.resolution = requireText(resolution, "A resolution is required to resolve a ticket");
        this.status = TicketStatus.RESOLVED;
    }

    public void confirm() {
        requireStatus("confirm", TicketStatus.RESOLVED);
        this.status = TicketStatus.CLOSED;
    }

    public void reopen() {
        requireStatus("reopen", TicketStatus.RESOLVED);
        this.status = TicketStatus.IN_PROGRESS;
    }

    public void escalate(String reason) {
        requireStatus("escalate", TicketStatus.ASSIGNED, TicketStatus.IN_PROGRESS);
        this.escalationReason = requireText(reason, "A reason is required to escalate a ticket");
        this.assignee = null;
        this.status = TicketStatus.NEW;
    }

    public void cancel() {
        requireStatus("cancel", TicketStatus.NEW);
        this.status = TicketStatus.CLOSED;
    }

    public boolean isRequestedBy(AppUser user) {
        return user != null && requester.getId().equals(user.getId());
    }

    public boolean isAssignedTo(AppUser user) {
        return user != null && assignee != null && assignee.getId().equals(user.getId());
    }

    private void requireStatus(String action, TicketStatus... allowed) {
        if (Arrays.stream(allowed).noneMatch(allowedStatus -> allowedStatus == status)) {
            throw new InvalidTicketTransitionException(
                    "Cannot " + action + " a ticket in status " + status);
        }
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new InvalidTicketTransitionException(message);
        }
        return value.strip();
    }

    public Long getId() {
        return id;
    }

    public String getReference() {
        return reference;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public Priority getPriority() {
        return priority;
    }

    public Category getCategory() {
        return category;
    }

    public AppUser getRequester() {
        return requester;
    }

    public AppUser getAssignee() {
        return assignee;
    }

    public String getResolution() {
        return resolution;
    }

    public String getEscalationReason() {
        return escalationReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}