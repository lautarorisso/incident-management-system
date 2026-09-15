package com.lautarorisso.notification_service.messaging;

import com.lautarorisso.notification_service.entity.Notification;
import com.lautarorisso.notification_service.enums.NotificationDeliveryStatus;
import com.lautarorisso.notification_service.enums.NotificationReadStatus;
import com.lautarorisso.notification_service.enums.NotificationType;
import com.lautarorisso.notification_service.repository.NotificationRepository;
import com.lautarorisso.notification_service.repository.ProcessedEventRepository;
import com.lautarorisso.notification_service.support.AbstractMongoTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.RabbitMQContainer;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end test of the real event chain: RabbitMQ broker → {@code @RabbitListener}
 * ({@link IncidentEventListener}) → {@code NotificationRoutingService} → MongoDB.
 * <p>
 * Starts a real {@code rabbitmq:3-management} container (the module default keeps
 * {@code spring.rabbitmq.listener.auto-startup=false}, so the listener container is
 * enabled here with an explicit property) and publishes a real message with the real
 * {@link RabbitTemplate} to the {@code incident.events} exchange using the same
 * routing key and payload keys the incident-service outbox chain produces.
 */
@SpringBootTest(properties = "spring.rabbitmq.listener.auto-startup=true")
@ActiveProfiles("test")
class RabbitMqConsumptionIntegrationTest extends AbstractMongoTestBase {

    private static final String EXCHANGE = "incident.events";

    /**
     * Routing key produced by {@code RabbitMqEventPublisher.publish} for
     * {@code INCIDENT_ASSIGNED}: {@code "incident." + "incident_assigned"
     * .toLowerCase().replace("incident_", "")} = {@code incident.assigned}.
     */
    private static final String ROUTING_KEY_ASSIGNED = "incident.assigned";

    @ServiceConnection
    static final RabbitMQContainer RABBITMQ = startRabbitMq();

    private static RabbitMQContainer startRabbitMq() {
        RabbitMQContainer container = new RabbitMQContainer("rabbitmq:3-management");
        container.start();
        return container;
    }

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @BeforeEach
    void cleanCollections() {
        notificationRepository.deleteAll();
        processedEventRepository.deleteAll();
    }

    @Test
    void consumesRealIncidentEventAndPersistsNotification() {
        UUID assigneeId = UUID.randomUUID();
        String incidentId = UUID.randomUUID().toString();
        String eventId = UUID.randomUUID().toString();

        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY_ASSIGNED,
                buildEventPayload(eventId, incidentId, assigneeId.toString(), "jdoe@example.com"));

        // The listener writes the notification first and the dedup row LAST
        // (at-least-once ordering), so wait for both before asserting.
        awaitCondition(() -> !notificationRepository.findByUserIdOrderByCreatedAtDesc(assigneeId).isEmpty()
                && processedEventRepository.existsById(eventId));

        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(assigneeId);
        assertThat(notifications).hasSize(1);
        Notification notification = notifications.getFirst();
        assertThat(notification.getType()).isEqualTo(NotificationType.INCIDENT_ASSIGNED);
        assertThat(notification.getUserId()).isEqualTo(assigneeId);
        assertThat(notification.getIncidentId()).isEqualTo(UUID.fromString(incidentId));
        assertThat(notification.getDeliveryStatus()).isIn(NotificationDeliveryStatus.PENDING, NotificationDeliveryStatus.SENT);
        assertThat(notification.getReadStatus()).isEqualTo(NotificationReadStatus.UNREAD);
        assertThat(notification.getRecipientEmail()).isEqualTo("jdoe@example.com");
        assertThat(notification.getEventId()).isEqualTo(eventId);
        assertThat(processedEventRepository.existsById(eventId)).isTrue();
    }

    @Test
    void duplicateEventIdIsConsumedOnlyOnce() {
        UUID assigneeId = UUID.randomUUID();
        String incidentId = UUID.randomUUID().toString();
        String eventId = UUID.randomUUID().toString();

        Map<String, Object> payload = buildEventPayload(eventId, incidentId, assigneeId.toString(), "jdoe@example.com");
        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY_ASSIGNED, payload);
        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY_ASSIGNED, payload);

        // First delivery must be processed (dedup row + notification persisted).
        awaitCondition(() -> processedEventRepository.existsById(eventId)
                && !notificationRepository.findByUserIdOrderByCreatedAtDesc(assigneeId).isEmpty());

        // Give the second (duplicate) delivery time to be consumed and skipped:
        // idempotency on eventId must never produce a second notification.
        long deadline = System.currentTimeMillis() + 2000;
        while (System.currentTimeMillis() < deadline) {
            assertThat(notificationRepository.findByUserIdOrderByCreatedAtDesc(assigneeId))
                    .as("duplicate eventId %s must not create another notification", eventId)
                    .hasSize(1);
            sleep(200);
        }
        assertThat(notificationRepository.findByUserIdOrderByCreatedAtDesc(assigneeId)).hasSize(1);
    }

    /**
     * Builds the exact payload shape the incident-service outbox chain publishes:
     * {@code IncidentService.buildPayload(...)} (incidentId, title, status, priority,
     * assigneeId, teamId) → {@code OutboxPoller} stamps {@code eventId} →
     * {@code RabbitMqEventPublisher.publish} stamps {@code eventType} + {@code timestamp}.
     * The {@code assigneeEmail} key mirrors fix 1: the incident-service outbox now
     * carries the real recipient email in the payload.
     */
    private Map<String, Object> buildEventPayload(String eventId, String incidentId, String assigneeId, String assigneeEmail) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("incidentId", incidentId);
        payload.put("title", "Outbox event payload");
        payload.put("status", "OPEN");
        payload.put("priority", "HIGH");
        payload.put("assigneeId", assigneeId);
        payload.put("assigneeEmail", assigneeEmail);
        payload.put("teamId", UUID.randomUUID().toString());
        payload.put("eventId", eventId);
        payload.put("eventType", "INCIDENT_ASSIGNED");
        payload.put("timestamp", Instant.now().toString());
        return payload;
    }

    private void awaitCondition(BooleanSupplier condition) {
        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline && !condition.getAsBoolean()) {
            sleep(200);
        }
        assertThat(condition.getAsBoolean())
                .as("condition not met within 10s")
                .isTrue();
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting", e);
        }
    }
}