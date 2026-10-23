package io.github.oumaimacoco.helpdesk.notification;

import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailOutbox {

    static final int MAX_ATTEMPTS = 5;

    private final OutboxEmailRepository emails;
    private final Clock clock;

    public EmailOutbox(OutboxEmailRepository emails, Clock clock) {
        this.emails = emails;
        this.clock = clock;
    }

    @Transactional
    public void queue(String recipient, String subject, String body) {
        emails.save(new OutboxEmail(recipient, subject, body));
    }

    @Transactional(readOnly = true)
    public List<OutboxEmail> pending() {
        return emails.findTop20BySentAtIsNullAndAttemptsLessThanOrderByIdAsc(MAX_ATTEMPTS);
    }

    @Transactional
    public void markSent(Long id) {
        emails.findById(id).ifPresent(email -> email.markSent(clock.instant()));
    }

    @Transactional
    public void markFailed(Long id, String error) {
        emails.findById(id).ifPresent(email -> email.markFailed(error));
    }
}