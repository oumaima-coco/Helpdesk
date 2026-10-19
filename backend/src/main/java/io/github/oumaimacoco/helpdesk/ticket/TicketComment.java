package io.github.oumaimacoco.helpdesk.ticket;

import io.github.oumaimacoco.helpdesk.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "ticket_comment")
public class TicketComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false, updatable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false, updatable = false)
    private AppUser author;

    @Column(nullable = false, updatable = false, columnDefinition = "text")
    private String body;

    @Column(nullable = false, updatable = false)
    private boolean internal;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected TicketComment() {
    }

    public TicketComment(Ticket ticket, AppUser author, String body, boolean internal) {
        this.ticket = ticket;
        this.author = author;
        this.body = body;
        this.internal = internal;
    }

    public Long getId() {
        return id;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public AppUser getAuthor() {
        return author;
    }

    public String getBody() {
        return body;
    }

    public boolean isInternal() {
        return internal;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}