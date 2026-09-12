package com.lautarorisso.incident_service.messaging;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lautarorisso.incident_service.enums.IncidentEvent;
import com.lautarorisso.incident_service.entity.OutboxEvent;
import com.lautarorisso.incident_service.repository.OutboxEventRepository;
import com.lautarorisso.incident_service.service.OutboxPoller;
import com.lautarorisso.incident_service.support.AbstractPostgresTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.RabbitMQContainer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for the outbox → publish flow using real PostgreSQL and a real
 * RabbitMQ broker (both Testcontainers).
 * <p>
 * Verifies that the {@link OutboxPoller} reads unpublished events from the database,
 * publishes them to the {@code incident.events} exchange via
 * {@link RabbitMqEventPublisher}, marks them as published, and that the serialized
 * payload actually lands on the {@code incident.events.queue} queue with the expected
 * keys (eventType, incidentId, eventId).
 */
@SpringBootTest
@ActiveProfiles("test")
class OutboxPollerIntegrationTest extends AbstractPostgresTestBase {

    private static final String EXCHANGE = RabbitMqConfig.EXCHANGE_NAME;
    private static final String QUEUE = RabbitMqConfig.QUEUE_NAME;

    @ServiceConnection
    static final RabbitMQContainer RABBITMQ = startRabbitMq();

    private static RabbitMQContainer startRabbitMq() {
        RabbitMQContainer container = new RabbitMQContainer("rabbitmq:3-management");
        container.start();
        return container;
    }

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private OutboxPoller outboxPoller;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID incidentId;

    @BeforeEach
    void setUp() {
        // Drain anything left over from the previous test (or from the real
        // @Scheduled poller, which can also publish events once its 10s initial
        // delay has elapsed) so queue assertions are per-test isolated.
        drainQueue();
        outboxEventRepository.deleteAll();
        incidentId = UUID.randomUUID();
    }

    @Test
    void shouldPublishOutboxEventAndMarkAsPublished() {
        UUID eventId = UUID.randomUUID();
        OutboxEvent event = new OutboxEvent();
        event.setId(eventId);
        event.setAggregateId(incidentId);
        event.setEventType(IncidentEvent.INCIDENT_CREATED.name());
        event.setPayload("{\"incidentId\":\"" + incidentId + "\",\"title\":\"Test\",\"status\":\"OPEN\",\"priority\":\"HIGH\"}");
        event.setPublished(false);
        event.setCreatedAt(Instant.now());
        outboxEventRepository.save(event);
        assertThat(outboxEventRepository.findByPublishedFalse()).hasSize(1);

        outboxPoller.processOutbox();

        assertThat(outboxEventRepository.findByPublishedFalse()).isEmpty();
        assertThat(outboxEventRepository.count()).isEqualTo(1);
        var saved = outboxEventRepository.findAll().get(0);
        assertThat(saved.isPublished()).isTrue();

        List<Message> messages = drainQueue();
        assertThat(messages).as("message must land on " + QUEUE).isNotEmpty();
        Map<String, Object> payload = toPayloadMap(messages.getFirst());
        assertThat(payload.get("eventType")).isEqualTo("INCIDENT_CREATED");
        assertThat(payload.get("incidentId")).isEqualTo(incidentId.toString());
        assertThat(payload.get("eventId")).isEqualTo(eventId.toString());
    }

    @Test
    void shouldPublishMultipleEventsSequentially() {
        UUID firstEventId = UUID.randomUUID();
        UUID secondEventId = UUID.randomUUID();
        UUID secondIncidentId = UUID.randomUUID();
        UUID assigneeId = UUID.randomUUID();

        OutboxEvent event1 = new OutboxEvent();
        event1.setId(firstEventId);
        event1.setAggregateId(incidentId);
        event1.setEventType(IncidentEvent.INCIDENT_CREATED.name());
        event1.setPayload("{\"incidentId\":\"" + incidentId + "\",\"title\":\"Test\",\"status\":\"OPEN\",\"priority\":\"HIGH\"}");
        event1.setPublished(false);
        event1.setCreatedAt(Instant.now());
        OutboxEvent event2 = new OutboxEvent();
        event2.setId(secondEventId);
        event2.setAggregateId(secondIncidentId);
        event2.setEventType(IncidentEvent.INCIDENT_ASSIGNED.name());
        event2.setPayload("{\"incidentId\":\"" + secondIncidentId + "\",\"title\":\"Assign\",\"status\":\"OPEN\",\"priority\":\"LOW\",\"assigneeId\":\"" + assigneeId + "\"}");
        event2.setPublished(false);
        event2.setCreatedAt(Instant.now());
        outboxEventRepository.save(event1);
        outboxEventRepository.save(event2);

        outboxPoller.processOutbox();

        assertThat(outboxEventRepository.findByPublishedFalse()).isEmpty();
        assertThat(outboxEventRepository.count()).isEqualTo(2);

        List<Map<String, Object>> payloads = drainQueue().stream()
                .map(this::toPayloadMap)
                .toList();
        assertThat(payloads).as("both events must land on " + QUEUE).isNotEmpty();
        assertThat(payloads).extracting(m -> m.get("eventId"))
                .contains(firstEventId.toString(), secondEventId.toString());
        assertThat(payloads).extracting(m -> m.get("eventType"))
                .contains("INCIDENT_CREATED", "INCIDENT_ASSIGNED");
    }

    @Test
    void shouldNotFailWhenNoEventsExist() {
        outboxPoller.processOutbox();

        assertThat(outboxEventRepository.findByPublishedFalse()).isEmpty();
        assertThat(drainQueue()).isEmpty();
    }

    /**
     * Receives every message currently on the queue (up to a safety cap), so the
     * broker is left clean for the next test.
     */
    private List<Message> drainQueue() {
        List<Message> messages = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Message message = rabbitTemplate.receive(QUEUE, 500);
            if (message == null) {
                break;
            }
            messages.add(message);
        }
        return messages;
    }

    private Map<String, Object> toPayloadMap(Message message) {
        try {
            return objectMapper.readValue(message.getBody(),
                    new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize received message", e);
        }
    }
}