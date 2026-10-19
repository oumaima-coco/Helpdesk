package io.github.oumaimacoco.helpdesk.ticket;

import io.github.oumaimacoco.helpdesk.user.CurrentUserService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/tickets/{ticketId}/attachments")
public class TicketAttachmentController {

    private final TicketAttachmentService attachments;
    private final CurrentUserService currentUsers;

    public TicketAttachmentController(TicketAttachmentService attachments, CurrentUserService currentUsers) {
        this.attachments = attachments;
        this.currentUsers = currentUsers;
    }

    @GetMapping
    public List<AttachmentResponse> list(JwtAuthenticationToken authentication, @PathVariable Long ticketId) {
        return attachments.list(currentUsers.currentActor(authentication), ticketId);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AttachmentResponse upload(JwtAuthenticationToken authentication, @PathVariable Long ticketId,
                                     @RequestParam("file") MultipartFile file) {
        return attachments.upload(currentUsers.currentActor(authentication), ticketId, file);
    }

    @GetMapping("/{attachmentId}")
    public ResponseEntity<InputStreamResource> download(JwtAuthenticationToken authentication,
                                                        @PathVariable Long ticketId,
                                                        @PathVariable Long attachmentId) {
        AttachmentDownload download = attachments.download(
                currentUsers.currentActor(authentication), ticketId, attachmentId);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(download.filename(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(download.contentType()))
                .contentLength(download.sizeBytes())
                .body(new InputStreamResource(download.content()));
    }
}