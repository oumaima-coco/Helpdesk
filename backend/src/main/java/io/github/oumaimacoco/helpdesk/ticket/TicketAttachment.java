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
@Table(name = "ticket_attachment")
public class TicketAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false, updatable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploader_id", nullable = false, updatable = false)
    private AppUser uploader;

    @Column(name = "original_filename", nullable = false, updatable = false)
    private String originalFilename;

    @Column(name = "content_type", nullable = false, updatable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false, updatable = false)
    private long sizeBytes;

    @Column(name = "storage_key", nullable = false, updatable = false, unique = true, length = 100)
    private String storageKey;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected TicketAttachment() {
    }

    public TicketAttachment(Ticket ticket, AppUser uploader, String originalFilename,
                            String contentType, long sizeBytes, String storageKey) {
        this.ticket = ticket;
        this.uploader = uploader;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.storageKey = storageKey;
    }

    public Long getId() {
        return id;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public AppUser getUploader() {
        return uploader;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}