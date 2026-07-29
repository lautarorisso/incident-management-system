package com.lautarorisso.notification_service.adapter.persistence;

import com.lautarorisso.notification_service.adapter.out.persistence.NotificationPersistenceAdapter;
import com.lautarorisso.notification_service.adapter.out.persistence.ProcessedEventPersistenceAdapter;
import com.lautarorisso.notification_service.adapter.out.persistence.mapper.NotificationEntityMapper;
import com.lautarorisso.notification_service.adapter.out.persistence.mapper.ProcessedEventEntityMapper;
import com.lautarorisso.notification_service.domain.model.Notification;
import com.lautarorisso.notification_service.domain.model.NotificationId;
import com.lautarorisso.notification_service.domain.model.NotificationStatus;
import com.lautarorisso.notification_service.domain.model.NotificationType;
import com.lautarorisso.notification_service.domain.model.ProcessedEvent;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import({NotificationPersistenceAdapterTest.TestConfig.class,
        NotificationPersistenceAdapter.class,
        ProcessedEventPersistenceAdapter.class})
class NotificationPersistenceAdapterTest {

    @Autowired
    private NotificationPersistenceAdapter notificationAdapter;

    @Autowired
    private ProcessedEventPersistenceAdapter processedEventAdapter;

    @TestConfiguration
    static class TestConfig {
        @Bean
        NotificationEntityMapper notificationEntityMapper() {
            return Mappers.getMapper(NotificationEntityMapper.class);
        }

        @Bean
        ProcessedEventEntityMapper processedEventEntityMapper() {
            return Mappers.getMapper(ProcessedEventEntityMapper.class);
        }
    }

    @Test
    void saveAndFindNotificationById() {
        NotificationId id = new NotificationId(UUID.randomUUID());
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

        Notification saved = notificationAdapter.save(notification);
        Optional<Notification> found = notificationAdapter.findById(id);

        assertTrue(found.isPresent());
        assertEquals(id, found.get().getId());
        assertEquals("Test notification", found.get().getTitle());
        assertEquals(NotificationStatus.UNREAD, found.get().getStatus());
    }

    @Test
    void findByIdReturnsEmptyWhenNotFound() {
        Optional<Notification> found = notificationAdapter.findById(
                new NotificationId(UUID.randomUUID()));

        assertTrue(found.isEmpty());
    }

    @Test
    void findByUserIdReturnsUserNotifications() {
        UUID userId = UUID.randomUUID();
        NotificationId id1 = new NotificationId(UUID.randomUUID());
        NotificationId id2 = new NotificationId(UUID.randomUUID());

        notificationAdapter.save(Notification.builder()
                .id(id1).type(NotificationType.INCIDENT_ASSIGNED)
                .userId(userId).incidentId(UUID.randomUUID())
                .title("First").message("First msg")
                .createdAt(Instant.now()).build());

        notificationAdapter.save(Notification.builder()
                .id(id2).type(NotificationType.INCIDENT_STATUS_CHANGED)
                .userId(userId).incidentId(UUID.randomUUID())
                .title("Second").message("Second msg")
                .createdAt(Instant.now()).build());

        List<Notification> notifications = notificationAdapter.findByUserId(userId);

        assertEquals(2, notifications.size());
    }

    @Test
    void findByUserIdAndStatusFiltersByStatus() {
        UUID userId = UUID.randomUUID();
        NotificationId unreadId = new NotificationId(UUID.randomUUID());
        NotificationId readId = new NotificationId(UUID.randomUUID());

        notificationAdapter.save(Notification.builder()
                .id(unreadId).type(NotificationType.INCIDENT_ASSIGNED)
                .userId(userId).incidentId(UUID.randomUUID())
                .title("Unread").message("Unread msg")
                .status(NotificationStatus.UNREAD)
                .createdAt(Instant.now()).build());

        notificationAdapter.save(Notification.builder()
                .id(readId).type(NotificationType.INCIDENT_STATUS_CHANGED)
                .userId(userId).incidentId(UUID.randomUUID())
                .title("Read").message("Read msg")
                .status(NotificationStatus.READ)
                .createdAt(Instant.now()).build());

        List<Notification> unread = notificationAdapter.findByUserIdAndStatus(
                userId, NotificationStatus.UNREAD);

        assertEquals(1, unread.size());
        assertEquals(unreadId, unread.getFirst().getId());
    }

    @Test
    void findAllReturnsAllNotifications() {
        notificationAdapter.save(Notification.builder()
                .id(new NotificationId(UUID.randomUUID()))
                .type(NotificationType.INCIDENT_ASSIGNED)
                .userId(UUID.randomUUID()).incidentId(UUID.randomUUID())
                .title("A").message("A msg")
                .createdAt(Instant.now()).build());

        notificationAdapter.save(Notification.builder()
                .id(new NotificationId(UUID.randomUUID()))
                .type(NotificationType.INCIDENT_STATUS_CHANGED)
                .userId(UUID.randomUUID()).incidentId(UUID.randomUUID())
                .title("B").message("B msg")
                .createdAt(Instant.now()).build());

        List<Notification> all = notificationAdapter.findAll();

        assertEquals(2, all.size());
    }

    // --- ProcessedEvent persistence tests ---

    @Test
    void saveAndCheckProcessedEvent() {
        String eventId = UUID.randomUUID().toString();

        ProcessedEvent event = ProcessedEvent.builder()
                .eventId(eventId)
                .processedAt(Instant.now())
                .build();

        processedEventAdapter.save(event);

        assertTrue(processedEventAdapter.existsByEventId(eventId));
        assertFalse(processedEventAdapter.existsByEventId(UUID.randomUUID().toString()));
    }

    @Test
    void findByEventIdReturnsProcessedEvent() {
        String eventId = UUID.randomUUID().toString();

        processedEventAdapter.save(ProcessedEvent.builder()
                .eventId(eventId)
                .processedAt(Instant.now())
                .build());

        Optional<ProcessedEvent> found = processedEventAdapter.findByEventId(eventId);

        assertTrue(found.isPresent());
        assertEquals(eventId, found.get().getEventId());
    }

    @Test
    void findByEventIdReturnsEmptyWhenNotFound() {
        Optional<ProcessedEvent> found = processedEventAdapter.findByEventId(
                UUID.randomUUID().toString());

        assertTrue(found.isEmpty());
    }
}
