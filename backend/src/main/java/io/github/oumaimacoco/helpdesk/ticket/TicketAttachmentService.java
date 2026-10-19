package io.github.oumaimacoco.helpdesk.ticket;

import io.github.oumaimacoco.helpdesk.common.BusinessRuleException;
import io.github.oumaimacoco.helpdesk.common.InvalidRequestException;
import io.github.oumaimacoco.helpdesk.common.NotFoundException;
import io.github.oumaimacoco.helpdesk.storage.FileStorage;
import io.github.oumaimacoco.helpdesk.user.Actor;
import io.github.oumaimacoco.helpdesk.user.AppUserRepository;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Set;
import org.apache.tika.Tika;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TicketAttachmentService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "application/pdf",
            "text/plain");
    private static final int MAX_FILENAME_LENGTH = 255;
    private static final String DEFAULT_FILENAME = "attachment";

    private final TicketService ticketService;
    private final TicketAttachmentRepository attachments;
    private final AppUserRepository users;
    private final FileStorage storage;
    private final Tika tika = new Tika();

    public TicketAttachmentService(TicketService ticketService, TicketAttachmentRepository attachments,
                                   AppUserRepository users, FileStorage storage) {
        this.ticketService = ticketService;
        this.attachments = attachments;
        this.users = users;
        this.storage = storage;
    }

    @Transactional(readOnly = true)
    public List<AttachmentResponse> list(Actor actor, Long ticketId) {
        Ticket ticket = ticketService.findVisible(actor, ticketId);
        return attachments.findByTicketOrderByIdAsc(ticket).stream()
                .map(AttachmentResponse::from)
                .toList();
    }

    @Transactional
    public AttachmentResponse upload(Actor actor, Long ticketId, MultipartFile file) {
        Ticket ticket = ticketService.findVisible(actor, ticketId);
        if (!ticket.isRequestedBy(actor.user()) && !actor.isStaff()) {
            throw new AccessDeniedException("Only the requester and support staff can add attachments");
        }
        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new BusinessRuleException("Cannot add attachments to a closed ticket");
        }
        if (file == null || file.isEmpty()) {
            throw new InvalidRequestException("The file is empty");
        }

        byte[] content = readBytes(file);
        String contentType = tika.detect(content);
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new InvalidRequestException("File type " + contentType + " is not allowed");
        }

        String storageKey = storage.store(new ByteArrayInputStream(content));
        try {
            TicketAttachment attachment = attachments.saveAndFlush(new TicketAttachment(
                    ticket,
                    users.getReferenceById(actor.user().getId()),
                    safeFilename(file.getOriginalFilename()),
                    contentType,
                    content.length,
                    storageKey));
            return AttachmentResponse.from(attachment);
        } catch (RuntimeException exception) {
            storage.delete(storageKey);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public AttachmentDownload download(Actor actor, Long ticketId, Long attachmentId) {
        Ticket ticket = ticketService.findVisible(actor, ticketId);
        TicketAttachment attachment = attachments.findByIdAndTicket(attachmentId, ticket)
                .orElseThrow(() -> new NotFoundException("Attachment " + attachmentId + " not found"));
        return new AttachmentDownload(
                attachment.getOriginalFilename(),
                attachment.getContentType(),
                attachment.getSizeBytes(),
                storage.open(attachment.getStorageKey()));
    }

    static String safeFilename(String original) {
        if (original == null) {
            return DEFAULT_FILENAME;
        }
        String name = original.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        name = name.replaceAll("[\\p{Cntrl}\"]", "").strip();
        if (name.isEmpty()) {
            return DEFAULT_FILENAME;
        }
        return name.length() > MAX_FILENAME_LENGTH ? name.substring(0, MAX_FILENAME_LENGTH) : name;
    }

    private static byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot read the uploaded file", exception);
        }
    }
}