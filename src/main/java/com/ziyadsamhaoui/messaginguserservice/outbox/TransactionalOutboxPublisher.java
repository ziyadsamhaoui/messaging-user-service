package com.ziyadsamhaoui.messaginguserservice.outbox;

import com.ziyadsamhaoui.messaginguserservice.web.CorrelationContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

/**
 * Sprint 6 §3.2 — appends outbox rows. Runs in {@link Propagation#MANDATORY}
 * so a call outside an ambient transaction fails fast: a domain write without
 * an outbox write (or the reverse) breaks the one non-negotiable rule of the
 * pattern (Part 1, Rule 3).
 */
@Component
@RequiredArgsConstructor
public class TransactionalOutboxPublisher {

    public static final String TOPIC = "badrlink.user.profile.v1";
    public static final String AGGREGATE_TYPE = "UserProfile";

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * Appends an outbox row for {@code eventType}. Must be invoked inside the
     * transaction that performs the domain write — or not at all.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(String aggregateType, String aggregateId, String eventType, Object payload) {
        outboxEventRepository.save(OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .eventVersion(EventEnvelope.VERSION_1)
                .payload(objectMapper.writeValueAsString(payload))
                .correlationId(CorrelationContext.current())
                .build());
    }
}
