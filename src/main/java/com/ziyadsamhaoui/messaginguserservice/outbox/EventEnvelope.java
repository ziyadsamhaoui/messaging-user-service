package com.ziyadsamhaoui.messaginguserservice.outbox;

import java.time.Instant;
import java.util.UUID;

/**
 * Sprint 6 shared envelope (§3.1) — identical shape on every topic.
 * Mirrors messaging-auth-service {@code outbox/EventEnvelope} so consumers see one
 * wire format; kept local per the repo's no-shared-libraries convention.
 *
 * @param eventId       consumer-facing idempotency key (§3.4)
 * @param eventType     AGGREGATE_PAST_TENSE_VERB, e.g. USER_PROFILE_CREATED
 * @param eventVersion  bump on breaking payload changes (Part 1, Rule 5)
 * @param occurredAt    when the domain change happened
 * @param producer      publishing service name
 * @param correlationId carries the Gateway's X-Correlation-Id into the event stream
 * @param aggregateId   the id this topic partitions by (user id here)
 * @param payload       event-specific data, serialized from a record
 */
public record EventEnvelope(
        UUID eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        String producer,
        String correlationId,
        String aggregateId,
        Object payload) {

    public static final String PRODUCER = "messaging-user-service";
    public static final int VERSION_1 = 1;
}
