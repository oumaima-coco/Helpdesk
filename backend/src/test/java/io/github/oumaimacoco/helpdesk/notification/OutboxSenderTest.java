package io.github.oumaimacoco.helpdesk.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class OutboxSenderTest {

    @MockitoBean
    private JavaMailSender mailSender;

    @Autowired
    private EmailOutbox outbox;

    @Autowired
    private OutboxSender sender;

    @Autowired
    private OutboxEmailRepository emails;

    @Test
    void queuedEmailIsSentOnceAndMarkedAsSent() {
        String recipient = "someone-" + UUID.randomUUID() + "@example.com";
        outbox.queue(recipient, "[Helpdesk] HD-2026-0001", "Please check the system.");

        sender.sendPending();
        sender.sendPending();

        verify(mailSender, times(1)).send(argThat((SimpleMailMessage message) ->
                recipient.equals(message.getTo()[0])
                        && "[Helpdesk] HD-2026-0001".equals(message.getSubject())
                        && "helpdesk@example.com".equals(message.getFrom())));
        assertThat(emails.findByRecipientOrderByIdAsc(recipient))
                .singleElement()
                .satisfies(email -> {
                    assertThat(email.getSentAt()).isNotNull();
                    assertThat(email.getAttempts()).isZero();
                });
    }

    @Test
    void failedSendIsKeptForRetryWithItsError() {
        doThrow(new MailSendException("SMTP server unreachable"))
                .when(mailSender).send(any(SimpleMailMessage.class));
        String recipient = "someone-" + UUID.randomUUID() + "@example.com";
        outbox.queue(recipient, "[Helpdesk] HD-2026-0002", "Please check the system.");

        sender.sendPending();

        assertThat(emails.findByRecipientOrderByIdAsc(recipient))
                .singleElement()
                .satisfies(email -> {
                    assertThat(email.getSentAt()).isNull();
                    assertThat(email.getAttempts()).isEqualTo(1);
                    assertThat(email.getLastError()).contains("SMTP server unreachable");
                });
    }
}