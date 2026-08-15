package com.lautarorisso.notification_service.repository;

import com.lautarorisso.notification_service.entity.Notification;
import com.lautarorisso.notification_service.entity.NotificationStatus;
import com.lautarorisso.notification_service.entity.NotificationType;
import com.lautarorisso.notification_service.entity.ProcessedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataMongoTest
@ActiveProfiles("test")
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @BeforeEach
    void cleanCollections() {
        notificationRepository.deleteAll();
        processedEventRepository.deleteAll();
    }

    // --- Notification Repository Tests ---

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
                .status(NotificationStatus.UNREAD)
                .createdAt(Instant.now())
                .build();

        Notification saved = notificationRepository.save(notification);
        Optional<Notification> found = notificationRepository.findById(id);

        assertTrue(found.isPresent());
        assertEquals(id, found.get().getId());
        assertEquals("Test notification", found.get().getTitle());
        assertEquals(NotificationStatus.UNREAD, found.get().getStatus());
    }

    @Test
    void findByIdReturnsEmptyWhenNotFound() {
        Optional<Notification> found = notificationRepository.findById(UUID.randomUUID());

        assertTrue(found.isEmpty());
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
    void findByUserIdAndStatusFiltersByStatus() {
        UUID userId = UUID.randomUUID();
        UUID unreadId = UUID.randomUUID();
        UUID readId = UUID.randomUUID();

        notificationRepository.save(Notification.builder()
                .id(unreadId).type(NotificationType.INCIDENT_ASSIGNED)
                .userId(userId).incidentId(UUID.randomUUID())
                .title("Unread").message("Unread msg")
                .status(NotificationStatus.UNREAD)
                .createdAt(Instant.now()).build());

        notificationRepository.save(Notification.builder()
                .id(readId).type(NotificationType.INCIDENT_STATUS_CHANGED)
                .userId(userId).incidentId(UUID.randomUUID())
                .title("Read").message("Read msg")
                .status(NotificationStatus.READ)
                .createdAt(Instant.now()).build());

        List<Notification> unread = notificationRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, NotificationStatus.UNREAD);

        assertEquals(1, unread.size());
        assertEquals(unreadId, unread.getFirst().getId());
    }

    @Test
    void findAllReturnsAllNotifications() {
        notificationRepository.save(Notification.builder()
                .id(UUID.randomUUID())
                .type(NotificationType.INCIDENT_ASSIGNED)
                .userId(UUID.randomUUID()).incidentId(UUID.randomUUID())
                .title("A").message("A msg")
                .createdAt(Instant.now()).build());

        notificationRepository.save(Notification.builder()
                .id(UUID.randomUUID())
                .type(NotificationType.INCIDENT_STATUS_CHANGED)
                .userId(UUID.randomUUID()).incidentId(UUID.randomUUID())
                .title("B").message("B msg")
                .createdAt(Instant.now()).build());

        List<Notification> all = notificationRepository.findAll();

        assertEquals(2, all.size());
    }

    // --- ProcessedEvent Repository Tests ---

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

        Optional<ProcessedEvent> found = processedEventRepository.findById(eventId);

        assertTrue(found.isPresent());
        assertEquals(eventId, found.get().getEventId());
    }

    @Test
    void findByEventIdReturnsEmptyWhenNotFound() {
        Optional<ProcessedEvent> found = processedEventRepository.findById(UUID.randomUUID().toString());

        assertTrue(found.isEmpty());
    }
}
