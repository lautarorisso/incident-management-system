package com.lautarorisso.notification_service.domain;

import com.lautarorisso.notification_service.domain.model.Notification;
import com.lautarorisso.notification_service.domain.model.NotificationId;
import com.lautarorisso.notification_service.domain.model.NotificationStatus;
import com.lautarorisso.notification_service.domain.model.NotificationType;
import com.lautarorisso.notification_service.domain.model.ProcessedEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NotificationDomainTest {

    // --- NotificationId Tests ---

    @Test
    void notificationIdWrapsUuid() {
        UUID rawUuid = UUID.randomUUID();
        NotificationId id = new NotificationId(rawUuid);
        assertEquals(rawUuid, id.getValue());
    }

    @Test
    void notificationIdsAreEqualWhenUuidsMatch() {
        UUID rawUuid = UUID.randomUUID();
        NotificationId id1 = new NotificationId(rawUuid);
        NotificationId id2 = new NotificationId(rawUuid);
        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    void notificationIdsAreNotEqualWhenUuidsDiffer() {
        NotificationId id1 = new NotificationId(UUID.randomUUID());
        NotificationId id2 = new NotificationId(UUID.randomUUID());
        assertNotEquals(id1, id2);
    }

    // --- NotificationType Tests ---

    @Test
    void notificationTypeHasExpectedValues() {
        assertEquals(4, NotificationType.values().length);
        assertNotNull(NotificationType.valueOf("INCIDENT_ASSIGNED"));
        assertNotNull(NotificationType.valueOf("INCIDENT_STATUS_CHANGED"));
        assertNotNull(NotificationType.valueOf("INCIDENT_PRIORITY_CHANGED"));
        assertNotNull(NotificationType.valueOf("INCIDENT_COMMENT_ADDED"));
    }

    // --- NotificationStatus Tests ---

    @Test
    void notificationStatusHasExpectedValues() {
        assertEquals(2, NotificationStatus.values().length);
        assertNotNull(NotificationStatus.valueOf("UNREAD"));
        assertNotNull(NotificationStatus.valueOf("READ"));
    }

    // --- Notification Model Tests ---

    @Test
    void notificationCanBeCreatedWithAllFields() {
        NotificationId id = new NotificationId(UUID.randomUUID());
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
                .id(new NotificationId(UUID.randomUUID()))
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
                .id(new NotificationId(UUID.randomUUID()))
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
