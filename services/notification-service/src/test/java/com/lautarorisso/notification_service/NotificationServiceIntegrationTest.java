package com.lautarorisso.notification_service;

import com.lautarorisso.notification_service.entity.Notification;
import com.lautarorisso.notification_service.entity.NotificationStatus;
import com.lautarorisso.notification_service.messaging.IncidentEventListener;
import com.lautarorisso.notification_service.repository.NotificationRepository;
import com.lautarorisso.notification_service.repository.ProcessedEventRepository;
import com.lautarorisso.notification_service.support.AbstractMongoTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.RabbitMQContainer;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test of the full listener chain with real infrastructure:
 * RabbitMQ broker → {@code @RabbitListener} ({@link IncidentEventListener})
 * → {@code NotificationRoutingService} → MongoDB.
 * <p>
 * The broker container is only needed so Spring's auto-configured
 * {@code RabbitAdmin} can declare exchange/queue/binding against a real broker
 * instead of spewing connect-refused retries against localhost:5672.
 * {@code spring.rabbitmq.listener.auto-startup} stays {@code false} (test
 * profile default): this test calls {@link IncidentEventListener#handleIncidentEvent}
 * directly, so the listener container must NOT auto-consume.
 */
@SpringBootTest
@ActiveProfiles("test")
class NotificationServiceIntegrationTest extends AbstractMongoTestBase {

    @ServiceConnection
    static final RabbitMQContainer RABBITMQ = startRabbitMq();

    private static RabbitMQContainer startRabbitMq() {
        RabbitMQContainer container = new RabbitMQContainer("rabbitmq:3-management");
        container.start();
        return container;
    }

    @Autowired
    private IncidentEventListener eventListener;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Test
    void fullFlowConsumesEventAndPersistsNotification() {
        UUID assigneeId = UUID.randomUUID();
        String incidentId = UUID.randomUUID().toString();
        String eventId = UUID.randomUUID().toString();

        Map<String, Object> event = Map.of(
                "eventType", "INCIDENT_ASSIGNED",
                "incidentId", incidentId,
                "eventId", eventId,
                "assigneeId", assigneeId.toString(),
                "changedBy", UUID.randomUUID().toString()
        );

        eventListener.handleIncidentEvent(event);

        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(assigneeId);
        assertEquals(1, notifications.size());
        assertEquals(NotificationStatus.SENT, notifications.getFirst().getStatus());
        assertTrue(processedEventRepository.existsById(eventId));
    }

    @Test
    void duplicateEventIdIsPersistedOnlyOnce() {
        UUID assigneeId = UUID.randomUUID();
        String incidentId = UUID.randomUUID().toString();
        String eventId = UUID.randomUUID().toString();

        Map<String, Object> event = Map.of(
                "eventType", "INCIDENT_ASSIGNED",
                "incidentId", incidentId,
                "eventId", eventId,
                "assigneeId", assigneeId.toString(),
                "changedBy", UUID.randomUUID().toString()
        );

        // First call — should create notification and mark the event processed.
        eventListener.handleIncidentEvent(event);
        assertTrue(processedEventRepository.existsById(eventId));

        // Second call with the same eventId — must be skipped (idempotent).
        eventListener.handleIncidentEvent(event);

        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(assigneeId);
        assertEquals(1, notifications.size(), "Should not create duplicate notification");
    }

    @Test
    void differentEventIdsForSameIncidentProduceDistinctNotifications() {
        UUID assigneeId = UUID.randomUUID();
        String incidentId = UUID.randomUUID().toString();

        Map<String, Object> firstEvent = Map.of(
                "eventType", "INCIDENT_ASSIGNED",
                "incidentId", incidentId,
                "eventId", UUID.randomUUID().toString(),
                "assigneeId", assigneeId.toString(),
                "changedBy", UUID.randomUUID().toString()
        );
        Map<String, Object> secondEvent = Map.of(
                "eventType", "INCIDENT_ASSIGNED",
                "incidentId", incidentId,
                "eventId", UUID.randomUUID().toString(),
                "assigneeId", assigneeId.toString(),
                "changedBy", UUID.randomUUID().toString()
        );

        eventListener.handleIncidentEvent(firstEvent);
        eventListener.handleIncidentEvent(secondEvent);

        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(assigneeId);
        assertEquals(2, notifications.size(), "Distinct eventIds must each produce a notification");
        assertTrue(processedEventRepository.existsById((String) firstEvent.get("eventId")));
        assertTrue(processedEventRepository.existsById((String) secondEvent.get("eventId")));
    }

    @Test
    void eventWithNoTargetsDoesNotPersistAnything() {
        String eventId = UUID.randomUUID().toString();

        Map<String, Object> event = Map.of(
                "eventType", "INCIDENT_STATUS_CHANGED",
                "incidentId", UUID.randomUUID().toString(),
                "eventId", eventId
        );

        eventListener.handleIncidentEvent(event);

        assertFalse(processedEventRepository.existsById(eventId),
                "Should not mark as processed when no notifications created");
    }
}