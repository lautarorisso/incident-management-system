package com.lautarorisso.notification_service;

import com.lautarorisso.notification_service.entity.Notification;
import com.lautarorisso.notification_service.entity.NotificationStatus;
import com.lautarorisso.notification_service.messaging.IncidentEventListener;
import com.lautarorisso.notification_service.repository.NotificationRepository;
import com.lautarorisso.notification_service.repository.ProcessedEventRepository;
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
        UUID assigneeId = UUID.randomUUID();
        String incidentId = UUID.randomUUID().toString();

        Map<String, Object> event = Map.of(
                "eventType", "INCIDENT_ASSIGNED",
                "incidentId", incidentId,
                "assigneeId", assigneeId.toString(),
                "changedBy", UUID.randomUUID().toString()
        );

        eventListener.handleIncidentEvent(event);

        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(assigneeId);
        assertEquals(1, notifications.size());
        assertEquals(NotificationStatus.SENT, notifications.getFirst().getStatus());
        assertTrue(processedEventRepository.existsById(incidentId + ":INCIDENT_ASSIGNED"));
    }

    @Test
    void duplicateEventIsSkipped() {
        UUID assigneeId = UUID.randomUUID();

        Map<String, Object> event = Map.of(
                "eventType", "INCIDENT_ASSIGNED",
                "incidentId", UUID.randomUUID().toString(),
                "assigneeId", assigneeId.toString(),
                "changedBy", UUID.randomUUID().toString()
        );

        // First call — should create notification
        eventListener.handleIncidentEvent(event);

        // Second call — should skip (idempotent)
        eventListener.handleIncidentEvent(event);

        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(assigneeId);
        assertEquals(1, notifications.size(), "Should not create duplicate notification");
    }

    @Test
    void eventWithNoTargetsDoesNotPersistAnything() {
        Map<String, Object> event = Map.of(
                "eventType", "INCIDENT_STATUS_CHANGED",
                "incidentId", UUID.randomUUID().toString()
        );

        eventListener.handleIncidentEvent(event);

        assertFalse(processedEventRepository.existsById(
                                event.get("incidentId") + ":INCIDENT_STATUS_CHANGED"),
                "Should not mark as processed when no notifications created");
    }
}
