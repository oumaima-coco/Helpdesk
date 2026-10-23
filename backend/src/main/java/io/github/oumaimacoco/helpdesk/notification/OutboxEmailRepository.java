package io.github.oumaimacoco.helpdesk.notification;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEmailRepository extends JpaRepository<OutboxEmail, Long> {

    List<OutboxEmail> findTop20BySentAtIsNullAndAttemptsLessThanOrderByIdAsc(int maxAttempts);

    List<OutboxEmail> findByRecipientOrderByIdAsc(String recipient);
}