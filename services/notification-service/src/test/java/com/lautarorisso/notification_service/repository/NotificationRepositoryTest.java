package com.lautarorisso.notification_service.repository;

import com.lautarorisso.notification_service.entity.Notification;
import com.lautarorisso.notification_service.enums.NotificationDeliveryStatus;
import com.lautarorisso.notification_service.enums.NotificationReadStatus;
import com.lautarorisso.notification_service.enums.NotificationType;
import com.lautarorisso.notification_service.entity.ProcessedEvent;
import com.lautarorisso.notification_service.support.AbstractMongoTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataMongoTest
@ActiveProfiles("test")
class NotificationRepositoryTest extends AbstractMongoTestBase {

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
    void saveAndFindNotificationById() {
        UUID id = UUID.randomUUID();
        Notification notification = Notification.builder()
                .id(id)
                .type(NotificationType.INCIDENT_ASSIGNED)
                .userId(UUID.randomUUID())
                .incidentId(UUID.randomUUID())
                .title("Test notification")
                .message("Test message")
                .deliveryStatus(NotificationDeliveryStatus.SENT)
                .readStatus(NotificationReadStatus.READ)
                .createdAt(Instant.now())
                .build();

        Notification saved = notificationRepository.save(notification);
        var found = notificationRepository.findById(id);

        assertTrue(found.isPresent());
        assertEquals(id, found.get().getId());
        assertEquals("Test notification", found.get().getTitle());
        assertEquals(NotificationDeliveryStatus.SENT, found.get().getDeliveryStatus());
        assertEquals(NotificationReadStatus.READ, found.get().getReadStatus());
    }

    @Test
    void findByUserIdReturnsUserNotifications() {
        UUID userId = UUID.randomUUID();

        notificationRepository.save(Notification.builder()
                .id(UUID.randomUUID()).type(NotificationType.INCIDENT_ASSIGNED)
                .userId(userId).incidentId(UUID.randomUUID())
                .title("First").message("First msg")
                .createdAt(Instant.now()).build());

        notificationRepository.save(Notification.builder()
                .id(UUID.randomUUID()).type(NotificationType.INCIDENT_STATUS_CHANGED)
                .userId(userId).incidentId(UUID.randomUUID())
                .title("Second").message("Second msg")
                .createdAt(Instant.now()).build());

        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);

        assertEquals(2, notifications.size());
    }

    @Test
    void findByUserIdAndReadStatusFiltersByReadStatus() {
        UUID userId = UUID.randomUUID();
        UUID unreadId = UUID.randomUUID();
        UUID readId = UUID.randomUUID();

        notificationRepository.save(Notification.builder()
                .id(unreadId).type(NotificationType.INCIDENT_ASSIGNED)
                .userId(userId).incidentId(UUID.randomUUID())
                .title("Unread").message("Unread msg")
                .readStatus(NotificationReadStatus.UNREAD)
                .createdAt(Instant.now()).build());

        notificationRepository.save(Notification.builder()
                .id(readId).type(NotificationType.INCIDENT_STATUS_CHANGED)
                .userId(userId).incidentId(UUID.randomUUID())
                .title("Read").message("Read msg")
                .readStatus(NotificationReadStatus.READ)
                .createdAt(Instant.now()).build());

        List<Notification> unread = notificationRepository.findByUserIdAndReadStatusOrderByCreatedAtDesc(userId, NotificationReadStatus.UNREAD);

        assertEquals(1, unread.size());
        assertEquals(unreadId, unread.getFirst().getId());
    }

    @Test
    void findByUserIdAndReadStatusIgnoresDeliveryStatus() {
        UUID userId = UUID.randomUUID();
        UUID pendingId = UUID.randomUUID();

        // Both "unread": one failed to deliver (FAILED), one pending (PENDING).
        notificationRepository.save(Notification.builder()
                .id(pendingId).type(NotificationType.INCIDENT_ASSIGNED)
                .userId(userId).incidentId(UUID.randomUUID())
                .title("Pending").message("Pending msg")
                .deliveryStatus(NotificationDeliveryStatus.FAILED)
                .readStatus(NotificationReadStatus.UNREAD)
                .createdAt(Instant.now()).build());

        notificationRepository.save(Notification.builder()
                .id(UUID.randomUUID()).type(NotificationType.INCIDENT_STATUS_CHANGED)
                .userId(userId).incidentId(UUID.randomUUID())
                .title("Second").message("Second msg")
                .deliveryStatus(NotificationDeliveryStatus.PENDING)
                .readStatus(NotificationReadStatus.UNREAD)
                .createdAt(Instant.now()).build());

        List<Notification> unread = notificationRepository.findByUserIdAndReadStatusOrderByCreatedAtDesc(userId, NotificationReadStatus.UNREAD);

        assertEquals(2, unread.size());
    }

    @Test
    void saveAndCheckProcessedEvent() {
        String eventId = UUID.randomUUID().toString();

        ProcessedEvent event = ProcessedEvent.builder()
                .eventId(eventId)
                .processedAt(Instant.now())
                .build();

        processedEventRepository.save(event);

        assertTrue(processedEventRepository.existsById(eventId));
        assertFalse(processedEventRepository.existsById(UUID.randomUUID().toString()));
    }

    @Test
    void findByEventIdReturnsProcessedEvent() {
        String eventId = UUID.randomUUID().toString();

        processedEventRepository.save(ProcessedEvent.builder()
                .eventId(eventId)
                .processedAt(Instant.now())
                .build());

        var found = processedEventRepository.findById(eventId);

        assertTrue(found.isPresent());
        assertEquals(eventId, found.get().getEventId());
    }

    @Test
    void existsByEventIdAndUserIdMatchesOnlySameUserAndEventId() {
        String eventId = UUID.randomUUID().toString();
        UUID userId = UUID.randomUUID();

        notificationRepository.save(Notification.builder()
                .id(UUID.randomUUID())
                .type(NotificationType.INCIDENT_ASSIGNED)
                .userId(userId)
                .incidentId(UUID.randomUUID())
                .title("Dedup key test")
                .message("msg")
                .eventId(eventId)
                .createdAt(Instant.now())
                .build());

        assertTrue(notificationRepository.existsByEventIdAndUserId(eventId, userId));
        assertFalse(notificationRepository.existsByEventIdAndUserId(eventId, UUID.randomUUID()));
        assertFalse(notificationRepository.existsByEventIdAndUserId(UUID.randomUUID().toString(), userId));
        // legacy documents without an eventId must never match the pre-check
        assertFalse(notificationRepository.existsByEventIdAndUserId(null, userId));
    }
}
