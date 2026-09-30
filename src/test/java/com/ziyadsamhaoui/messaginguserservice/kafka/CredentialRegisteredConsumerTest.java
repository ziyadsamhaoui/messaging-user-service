package com.ziyadsamhaoui.messaginguserservice.kafka;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ziyadsamhaoui.messaginguserservice.dto.UserDtos.CreateUserRequest;
import com.ziyadsamhaoui.messaginguserservice.service.UserService;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;


class CredentialRegisteredConsumerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private UserService userService;
    private CredentialRegisteredConsumer consumer;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        consumer = new CredentialRegisteredConsumer(userService, MAPPER);
    }

    @Test
    void credentialRegisteredIsAppliedThroughTheIdempotentInternalMethod() {
        UUID credentialId = UUID.randomUUID();

        consumer.onMessage(record(envelope("CREDENTIAL_REGISTERED", credentialId, "zara")));

        ArgumentCaptor<CreateUserRequest> request = ArgumentCaptor.forClass(CreateUserRequest.class);
        verify(userService).createInternalUser(request.capture());
        assertThat(request.getValue().id()).isEqualTo(credentialId);
        assertThat(request.getValue().username()).isEqualTo("zara");
    }

    @Test
    void redeliveredEventIsAppliedAgainToTheSameIdempotentOperation() {
        UUID credentialId = UUID.randomUUID();
        String json = envelope("CREDENTIAL_REGISTERED", credentialId, "zara");

        consumer.onMessage(record(json));
        consumer.onMessage(record(json));

        // Both deliveries target the same id (natural idempotency, §3.4): the second
        // read hits existsById and returns the existing profile without a second insert.
        verify(userService, times(2)).createInternalUser(any(CreateUserRequest.class));
    }

    @Test
    void otherCredentialEventTypesAreIgnored() {
        consumer.onMessage(record(envelope("CREDENTIAL_LOCKED", UUID.randomUUID(), "zara")));

        verify(userService, never()).createInternalUser(any());
    }

    @Test
    void conflictingUsernameFromTheConsumerPathDoesNotEscapeTheListener() {
        when(userService.createInternalUser(any()))
                .thenThrow(new com.ziyadsamhaoui.messaginguserservice.exception.DuplicateUsernameException("taken"));

        assertThatCode(() -> consumer.onMessage(record(envelope("CREDENTIAL_REGISTERED", UUID.randomUUID(), "taken"))))
                .doesNotThrowAnyException();
    }

    @Test
    void malformedPayloadDoesNotEscapeTheListener() {
        assertThatCode(() -> consumer.onMessage(record("not-json"))).doesNotThrowAnyException();
        assertThatCode(() -> consumer.onMessage(record(envelope("CREDENTIAL_REGISTERED", null, null))))
                .doesNotThrowAnyException();
    }

    private ConsumerRecord<String, String> record(String value) {
        return new ConsumerRecord<>(CredentialRegisteredConsumer.TOPIC, 0, 0L, "key", value);
    }

    private String envelope(String eventType, UUID credentialId, String username) {
        return """
                {
                  "eventId": "%s",
                  "eventType": "%s",
                  "eventVersion": 1,
                  "occurredAt": "2026-01-01T10:00:00Z",
                  "producer": "messaging-auth-service",
                  "correlationId": "corr-1",
                  "aggregateId": "%s",
                  "payload": {"credentialId": "%s", "username": "%s", "email": "zara@example.com", "createdAt": "2026-01-01T10:00:00Z"}
                }
                """.formatted(UUID.randomUUID(), eventType, credentialId, credentialId, username);
    }
}
