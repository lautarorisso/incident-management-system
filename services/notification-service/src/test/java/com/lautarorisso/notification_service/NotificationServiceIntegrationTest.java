package com.lautarorisso.notification_service;

import com.lautarorisso.notification_service.adapter.out.messaging.IncidentEventListener;
import com.lautarorisso.notification_service.domain.model.Notification;
import com.lautarorisso.notification_service.domain.model.NotificationStatus;
import com.lautarorisso.notification_service.domain.port.out.NotificationRepository;
import com.lautarorisso.notification_service.domain.port.out.ProcessedEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class NotificationServiceIntegrationTest {

    @Autowired
    private IncidentEventListener eventListener;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Test
    void fullFlowConsumesEventAndPersistsNotification() {
        String eventId = UUID.randomUUID().toString();
        UUID assigneeId = UUID.randomUUID();

        Map<String, Object> event = Map.of(
                "eventType", "INCIDENT_ASSIGNED",
                "incidentId", UUID.randomUUID().toString(),
                "assigneeId", assigneeId.toString(),
                "changedBy", UUID.randomUUID().toString()
        );

        eventListener.handleIncidentEvent(event, eventId);

        List<Notification> notifications = notificationRepository.findByUserId(assigneeId);
        assertEquals(1, notifications.size());
        assertEquals(NotificationStatus.UNREAD, notifications.getFirst().getStatus());
        assertTrue(processedEventRepository.existsByEventId(eventId));
    }

    @Test
    void duplicateEventIsSkipped() {
        String eventId = UUID.randomUUID().toString();
        UUID assigneeId = UUID.randomUUID();

        Map<String, Object> event = Map.of(
                "eventType", "INCIDENT_ASSIGNED",
                "incidentId", UUID.randomUUID().toString(),
                "assigneeId", assigneeId.toString(),
                "changedBy", UUID.randomUUID().toString()
        );

        // First call — should create notification
        eventListener.handleIncidentEvent(event, eventId);

        // Second call — should skip (idempotent)
        eventListener.handleIncidentEvent(event, eventId);

        List<Notification> notifications = notificationRepository.findByUserId(assigneeId);
        assertEquals(1, notifications.size(), "Should not create duplicate notification");
    }

    @Test
    void eventWithNoTargetsDoesNotPersistAnything() {
        String eventId = UUID.randomUUID().toString();

        Map<String, Object> event = Map.of(
                "eventType", "INCIDENT_STATUS_CHANGED",
                "incidentId", UUID.randomUUID().toString()
        );

        eventListener.handleIncidentEvent(event, eventId);

        assertFalse(processedEventRepository.existsByEventId(eventId),
                "Should not mark as processed when no notifications created");
    }
}
