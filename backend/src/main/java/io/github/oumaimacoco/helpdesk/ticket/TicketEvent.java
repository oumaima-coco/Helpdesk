package io.github.oumaimacoco.helpdesk.ticket;

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
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "ticket_event")
public class TicketEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false, updatable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", updatable = false)
    private AppUser actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 30)
    private TicketEventType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", updatable = false, length = 20)
    private TicketStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, updatable = false, length = 20)
    private TicketStatus toStatus;

    @Column(updatable = false, columnDefinition = "text")
    private String detail;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected TicketEvent() {
    }

    public TicketEvent(Ticket ticket, AppUser actor, TicketEventType type,
                       TicketStatus fromStatus, TicketStatus toStatus, String detail) {
        this.ticket = ticket;
        this.actor = actor;
        this.type = type;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.detail = detail;
    }

    public Long getId() {
        return id;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public AppUser getActor() {
        return actor;
    }

    public TicketEventType getType() {
        return type;
    }

    public TicketStatus getFromStatus() {
        return fromStatus;
    }

    public TicketStatus getToStatus() {
        return toStatus;
    }

    public String getDetail() {
        return detail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}