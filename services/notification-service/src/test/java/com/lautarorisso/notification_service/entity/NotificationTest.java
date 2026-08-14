package com.lautarorisso.notification_service.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NotificationTest {

    // --- NotificationType Tests ---

    @Test
    void notificationTypeHasExpectedValues() {
        assertEquals(2, NotificationType.values().length);
        assertNotNull(NotificationType.valueOf("INCIDENT_ASSIGNED"));
        assertNotNull(NotificationType.valueOf("INCIDENT_STATUS_CHANGED"));
    }

    // --- NotificationStatus Tests ---

    @Test
    void notificationStatusHasExpectedValues() {
        assertEquals(4, NotificationStatus.values().length);
        assertNotNull(NotificationStatus.valueOf("UNREAD"));
        assertNotNull(NotificationStatus.valueOf("SENT"));
        assertNotNull(NotificationStatus.valueOf("FAILED"));
        assertNotNull(NotificationStatus.valueOf("READ"));
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
                .status(NotificationStatus.UNREAD)
                .createdAt(now)
                .build();

        assertEquals(id, notification.getId());
        assertEquals(NotificationType.INCIDENT_ASSIGNED, notification.getType());
        assertEquals(userId, notification.getUserId());
        assertEquals(incidentId, notification.getIncidentId());
        assertEquals("You have been assigned", notification.getTitle());
        assertEquals("Incident #123 has been assigned to you", notification.getMessage());
        assertEquals(NotificationStatus.UNREAD, notification.getStatus());
        assertEquals(now, notification.getCreatedAt());
    }

    @Test
    void notificationDefaultsToUnread() {
        Notification notification = Notification.builder()
                .id(UUID.randomUUID())
                .type(NotificationType.INCIDENT_ASSIGNED)
                .userId(UUID.randomUUID())
                .incidentId(UUID.randomUUID())
                .title("Test")
                .message("Test message")
                .build();

        assertEquals(NotificationStatus.UNREAD, notification.getStatus());
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
                .status(NotificationStatus.UNREAD)
                .build();

        Notification read = notification.withStatus(NotificationStatus.READ);

        assertEquals(NotificationStatus.READ, read.getStatus());
        assertEquals(NotificationStatus.UNREAD, notification.getStatus()); // original unchanged
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
