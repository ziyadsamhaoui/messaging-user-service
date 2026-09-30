package com.ziyadsamhaoui.messaginguserservice.kafka;

import com.ziyadsamhaoui.messaginguserservice.dto.UserDtos.CreateUserRequest;
import com.ziyadsamhaoui.messaginguserservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

/**
 * Sprint 6 §2.1 — dual-path registration consumer. Calls the SAME idempotent
 * method that backs POST /internal/users ({@code UserService.createInternalUser}),
 * never a second divergent code path. If User was down during a registration's
 * sync call (credential rolled back, client retries later) or if User's state
 * must ever be rebuilt from the event log, this path and the REST path converge
 * on one idempotent operation — replay of an already-applied event is a no-op
 * (§3.4, naturally idempotent target write, so no ledger bookkeeping needed).
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "badrlink.kafka", name = "enabled", havingValue = "true")
public class CredentialRegisteredConsumer {

    public static final String TOPIC = "badrlink.auth.credential.v1";
    public static final String GROUP_ID = "messaging-user-service";

    private final UserService userService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = TOPIC, groupId = GROUP_ID)
    public void onMessage(ConsumerRecord<String, String> record) {
        try {
            JsonNode envelope = objectMapper.readTree(record.value());
            String eventType = envelope.get("eventType").asString();
            if (!"CREDENTIAL_REGISTERED".equals(eventType)) {
                // Topic contract: only CREDENTIAL_* events arrive here. Other event
                // types belong to other consumers — skip, don't fail.
                log.debug("ignoring event type {} on {}", eventType, TOPIC);
                return;
            }
            JsonNode payload = envelope.get("payload");
            UUID credentialId = UUID.fromString(payload.get("credentialId").asString());
            String username = payload.get("username").asString();
            userService.createInternalUser(new CreateUserRequest(credentialId, username));
            log.info("processed CREDENTIAL_REGISTERED eventId={} credentialId={}",
                    envelope.get("eventId").asString(), credentialId);
        } catch (RuntimeException ex) {
            // Poison-pill protection: a malformed or conflicting event must not spin
            // the listener; log and continue. Conflicting username on a NEW credential
            // id is a genuine 409-shaped conflict that the REST path would also raise.
            log.error("failed to process event from {}: {}", TOPIC, record.value(), ex);
        }
    }
}
