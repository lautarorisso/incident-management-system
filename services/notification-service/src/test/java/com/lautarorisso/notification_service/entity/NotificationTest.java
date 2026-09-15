package com.lautarorisso.notification_service.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;
import com.lautarorisso.notification_service.enums.NotificationDeliveryStatus;
import com.lautarorisso.notification_service.enums.NotificationReadStatus;
import com.lautarorisso.notification_service.enums.NotificationType;

import static org.junit.jupiter.api.Assertions.*;

class NotificationTest {

    // --- NotificationType Tests ---

    @Test
    void notificationTypeHasExpectedValues() {
        assertEquals(2, NotificationType.values().length);
        assertNotNull(NotificationType.valueOf("INCIDENT_ASSIGNED"));
        assertNotNull(NotificationType.valueOf("INCIDENT_STATUS_CHANGED"));
    }

    @Test
    void fromEventTypeMapsKnownEventTypes() {
        assertEquals(NotificationType.INCIDENT_ASSIGNED,
                NotificationType.fromEventType("INCIDENT_ASSIGNED"));
        assertEquals(NotificationType.INCIDENT_STATUS_CHANGED,
                NotificationType.fromEventType("INCIDENT_STATUS_CHANGED"));
    }

    @Test
    void fromEventTypeReturnsNullForUnknownOrNull() {
        assertNull(NotificationType.fromEventType("UNKNOWN_EVENT"));
        assertNull(NotificationType.fromEventType(null));
    }

    // --- NotificationDeliveryStatus Tests ---

    @Test
    void notificationDeliveryStatusHasExpectedValues() {
        assertEquals(3, NotificationDeliveryStatus.values().length);
        assertNotNull(NotificationDeliveryStatus.valueOf("PENDING"));
        assertNotNull(NotificationDeliveryStatus.valueOf("SENT"));
        assertNotNull(NotificationDeliveryStatus.valueOf("FAILED"));
    }

    // --- NotificationReadStatus Tests ---

    @Test
    void notificationReadStatusHasExpectedValues() {
        assertEquals(2, NotificationReadStatus.values().length);
        assertNotNull(NotificationReadStatus.valueOf("UNREAD"));
        assertNotNull(NotificationReadStatus.valueOf("READ"));
    }

    // --- Notification Model Tests ---

    @Test
    void notificationCanBeCreatedWithAllFields() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();
        Instant now = Instant.now();

        Notification notification = Notification.builder()
                .id(id)
                .type(NotificationType.INCIDENT_ASSIGNED)
                .userId(userId)
                .incidentId(incidentId)
                .title("You have been assigned")
                .message("Incident #123 has been assigned to you")
                .recipientEmail("jdoe@example.com")
                .deliveryStatus(NotificationDeliveryStatus.SENT)
                .readStatus(NotificationReadStatus.READ)
                .createdAt(now)
                .build();

        assertEquals(id, notification.getId());
        assertEquals(NotificationType.INCIDENT_ASSIGNED, notification.getType());
        assertEquals(userId, notification.getUserId());
        assertEquals(incidentId, notification.getIncidentId());
        assertEquals("You have been assigned", notification.getTitle());
        assertEquals("Incident #123 has been assigned to you", notification.getMessage());
        assertEquals("jdoe@example.com", notification.getRecipientEmail());
        assertEquals(NotificationDeliveryStatus.SENT, notification.getDeliveryStatus());
        assertEquals(NotificationReadStatus.READ, notification.getReadStatus());
        assertEquals(now, notification.getCreatedAt());
    }

    @Test
    void notificationDefaultsToPendingAndUnread() {
        Notification notification = Notification.builder()
                .id(UUID.randomUUID())
                .type(NotificationType.INCIDENT_ASSIGNED)
                .userId(UUID.randomUUID())
                .incidentId(UUID.randomUUID())
                .title("Test")
                .message("Test message")
                .build();

        assertEquals(NotificationDeliveryStatus.PENDING, notification.getDeliveryStatus());
        assertEquals(NotificationReadStatus.UNREAD, notification.getReadStatus());
    }

    @Test
    void notificationCanBeMarkedAsRead() {
        Notification notification = Notification.builder()
                .id(UUID.randomUUID())
                .type(NotificationType.INCIDENT_STATUS_CHANGED)
                .userId(UUID.randomUUID())
                .incidentId(UUID.randomUUID())
                .title("Status changed")
                .message("Incident is now IN_PROGRESS")
                .deliveryStatus(NotificationDeliveryStatus.SENT)
                .build();

        Notification read = notification.withReadStatus(NotificationReadStatus.READ);

        assertEquals(NotificationReadStatus.READ, read.getReadStatus());
        // Delivery status is preserved by the copy-through.
        assertEquals(NotificationDeliveryStatus.SENT, read.getDeliveryStatus());
        assertEquals(NotificationReadStatus.UNREAD, notification.getReadStatus()); // original unchanged
    }

    @Test
    void markDeliveredAndMarkFailedPreserveReadStatus() {
        Notification notification = Notification.builder()
                .id(UUID.randomUUID())
                .type(NotificationType.INCIDENT_ASSIGNED)
                .userId(UUID.randomUUID())
                .readStatus(NotificationReadStatus.READ)
                .build();

        Notification delivered = notification.markDelivered();
        assertEquals(NotificationDeliveryStatus.SENT, delivered.getDeliveryStatus());
        assertEquals(NotificationReadStatus.READ, delivered.getReadStatus());

        Notification failed = notification.markFailed();
        assertEquals(NotificationDeliveryStatus.FAILED, failed.getDeliveryStatus());
        assertEquals(NotificationReadStatus.READ, failed.getReadStatus());

        // Original instance unchanged.
        assertEquals(NotificationDeliveryStatus.PENDING, notification.getDeliveryStatus());
        assertEquals(NotificationReadStatus.READ, notification.getReadStatus());
    }

    // --- ProcessedEvent Tests ---

    @Test
    void processedEventCanBeCreated() {
        String eventId = UUID.randomUUID().toString();
        Instant now = Instant.now();

        ProcessedEvent event = ProcessedEvent.builder()
                .eventId(eventId)
                .processedAt(now)
                .build();

        assertEquals(eventId, event.getEventId());
        assertEquals(now, event.getProcessedAt());
    }

    @Test
    void processedEventsAreEqualWhenEventIdsMatch() {
        String eventId = UUID.randomUUID().toString();
        Instant now = Instant.now();

        ProcessedEvent event1 = ProcessedEvent.builder()
                .eventId(eventId)
                .processedAt(now)
                .build();

        ProcessedEvent event2 = ProcessedEvent.builder()
                .eventId(eventId)
                .processedAt(now)
                .build();

        assertEquals(event1, event2);
        assertEquals(event1.hashCode(), event2.hashCode());
    }

    @Test
    void processedEventsAreNotEqualWhenEventIdsDiffer() {
        ProcessedEvent event1 = ProcessedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .processedAt(Instant.now())
                .build();

        ProcessedEvent event2 = ProcessedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .processedAt(Instant.now())
                .build();

        assertNotEquals(event1, event2);
    }
}