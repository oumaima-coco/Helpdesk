package io.github.oumaimacoco.helpdesk.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxSender {

    private static final Logger log = LoggerFactory.getLogger(OutboxSender.class);

    private final EmailOutbox outbox;
    private final JavaMailSender mailSender;
    private final String from;

    public OutboxSender(EmailOutbox outbox, JavaMailSender mailSender,
                        @Value("${helpdesk.mail.from}") String from) {
        this.outbox = outbox;
        this.mailSender = mailSender;
        this.from = from;
    }

    @Scheduled(
            fixedDelayString = "${helpdesk.mail.send-interval:PT30S}",
            initialDelayString = "${helpdesk.mail.initial-delay:PT30S}")
    public void sendPending() {
        for (OutboxEmail email : outbox.pending()) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(from);
                message.setTo(email.getRecipient());
                message.setSubject(email.getSubject());
                message.setText(email.getBody());
                mailSender.send(message);
                outbox.markSent(email.getId());
            } catch (MailException exception) {
                log.warn("Could not send email {} to {}: {}", email.getId(), email.getRecipient(), exception.getMessage());
                outbox.markFailed(email.getId(), exception.getMessage());
            }
        }
    }
}