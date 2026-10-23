package io.github.oumaimacoco.helpdesk.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "email_outbox")
public class OutboxEmail {

    private static final int MAX_ERROR_LENGTH = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String recipient;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    protected OutboxEmail() {
    }

    public OutboxEmail(String recipient, String subject, String body) {
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
    }

    void markSent(Instant moment) {
        this.sentAt = moment;
        this.lastError = null;
    }

    void markFailed(String error) {
        this.attempts++;
        this.lastError = error == null || error.length() <= MAX_ERROR_LENGTH
                ? error
                : error.substring(0, MAX_ERROR_LENGTH);
    }

    public Long getId() {
        return id;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public int getAttempts() {
        return attempts;
    }

    public String getLastError() {
        return lastError;
    }
}