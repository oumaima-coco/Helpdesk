package io.github.oumaimacoco.helpdesk.ticket;

import java.time.Instant;

public record AttachmentResponse(
        Long id,
        String filename,
        String contentType,
        long sizeBytes,
        String uploaderName,
        Instant createdAt) {

    static AttachmentResponse from(TicketAttachment attachment) {
        return new AttachmentResponse(
                attachment.getId(),
                attachment.getOriginalFilename(),
                attachment.getContentType(),
                attachment.getSizeBytes(),
                attachment.getUploader().getFullName(),
                attachment.getCreatedAt());
    }
}