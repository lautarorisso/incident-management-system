package com.lautarorisso.notification_service.messaging;

import com.lautarorisso.notification_service.entity.NotificationStatus;
import com.lautarorisso.notification_service.entity.NotificationType;
import com.lautarorisso.notification_service.entity.ProcessedEvent;
import com.lautarorisso.notification_service.notifier.EmailNotificationSender;
import com.lautarorisso.notification_service.repository.NotificationRepository;
import com.lautarorisso.notification_service.repository.ProcessedEventRepository;
import com.lautarorisso.notification_service.service.NotificationRoutingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentEventListenerTest {

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationRoutingService routingService;

    @Mock
    private EmailNotificationSender notificationSender;

    private IncidentEventListener listener;

    @Captor
    private ArgumentCaptor<ProcessedEvent> processedEventCaptor;

    private String eventIdFor(String incidentId, String eventType) {
        return incidentId + ":" + eventType;
    }

    @BeforeEach
    void setUp() {
        listener = new IncidentEventListener(
                processedEventRepository, notificationRepository, routingService, notificationSender);
    }

    @Test
    void handlesNewEventAndCreatesNotifications() {
        UUID assigneeId = UUID.randomUUID();
        String incidentId = UUID.randomUUID().toString();
        String eventId = eventIdFor(incidentId, "INCIDENT_ASSIGNED");
        Map<String, Object> event = Map.of(
                "eventType", "INCIDENT_ASSIGNED",
                "incidentId", incidentId,
                "assigneeId", assigneeId.toString(),
                "changedBy", UUID.randomUUID().toString()
        );

        when(processedEventRepository.existsById(eventId)).thenReturn(false);
        when(routingService.resolveTargets(event)).thenReturn(Set.of(assigneeId));
        when(routingService.resolveNotificationType("INCIDENT_ASSIGNED"))
                .thenReturn(NotificationType.INCIDENT_ASSIGNED);
        when(routingService.buildTitle(NotificationType.INCIDENT_ASSIGNED))
                .thenReturn("You have been assigned to incident");

        listener.handleIncidentEvent(event);

        // save called twice: once for UNREAD, once for SENT
        verify(notificationRepository, times(2)).save(argThat(n ->
                n.getUserId().equals(assigneeId) &&
                 n.getType() == NotificationType.INCIDENT_ASSIGNED
        ));
        verify(processedEventRepository).save(processedEventCaptor.capture());
        assertEquals(eventId, processedEventCaptor.getValue().getEventId());
    }

    @Test
    void skipsAlreadyProcessedEvent() {
        String incidentId = UUID.randomUUID().toString();
        when(processedEventRepository.existsById(eventIdFor(incidentId, "INCIDENT_ASSIGNED")))
                .thenReturn(true);

        listener.handleIncidentEvent(Map.of(
                "eventType", "INCIDENT_ASSIGNED",
                "incidentId", incidentId));

        verify(notificationRepository, never()).save(any());
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void skipsWhenNoTargetsResolved() {
        Map<String, Object> event = Map.of(
                "eventType", "INCIDENT_STATUS_CHANGED",
                "incidentId", UUID.randomUUID().toString()
        );

        when(processedEventRepository.existsById(eventIdFor(
                (String) event.get("incidentId"), "INCIDENT_STATUS_CHANGED"))).thenReturn(false);
        when(routingService.resolveNotificationType("INCIDENT_STATUS_CHANGED"))
                .thenReturn(NotificationType.INCIDENT_STATUS_CHANGED);
        when(routingService.resolveTargets(event)).thenReturn(Set.of());

        listener.handleIncidentEvent(event);

        verify(notificationRepository, never()).save(any());
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void createsNotificationsForMultipleTargets() {
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();
        Map<String, Object> event = Map.of(
                "eventType", "INCIDENT_ASSIGNED",
                "incidentId", UUID.randomUUID().toString(),
                "assigneeId", user1.toString()
        );

        when(processedEventRepository.existsById(eventIdFor(
                (String) event.get("incidentId"), "INCIDENT_ASSIGNED"))).thenReturn(false);
        when(routingService.resolveTargets(event)).thenReturn(Set.of(user1, user2));
        when(routingService.resolveNotificationType("INCIDENT_ASSIGNED"))
                .thenReturn(NotificationType.INCIDENT_ASSIGNED);
        when(routingService.buildTitle(NotificationType.INCIDENT_ASSIGNED))
                .thenReturn("You have been assigned to incident");

        listener.handleIncidentEvent(event);

        // save called 4 times: 2 users x 2 saves each (UNREAD + SENT)
        verify(notificationRepository, times(4)).save(argThat(n ->
                n.getUserId().equals(user1) || n.getUserId().equals(user2)
        ));
    }

    @Test
    void handlesUnknownNotificationTypeBySkipping() {
        Map<String, Object> event = Map.of(
                "eventType", "UNKNOWN_EVENT",
                "incidentId", UUID.randomUUID().toString()
        );

        when(processedEventRepository.existsById(eventIdFor(
                (String) event.get("incidentId"), "UNKNOWN_EVENT"))).thenReturn(false);
        when(routingService.resolveNotificationType("UNKNOWN_EVENT")).thenReturn(null);

        listener.handleIncidentEvent(event);

        verify(notificationRepository, never()).save(any());
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void handlesMalformedIncidentIdWithoutCrashing() {
        UUID assigneeId = UUID.randomUUID();
        String incidentId = "not-a-uuid";
        Map<String, Object> event = Map.of(
                "eventType", "INCIDENT_ASSIGNED",
                "incidentId", incidentId,
                "assigneeId", assigneeId.toString()
        );

        when(processedEventRepository.existsById(eventIdFor(incidentId, "INCIDENT_ASSIGNED")))
                .thenReturn(false);
        when(routingService.resolveTargets(event)).thenReturn(Set.of(assigneeId));
        when(routingService.resolveNotificationType("INCIDENT_ASSIGNED"))
                .thenReturn(NotificationType.INCIDENT_ASSIGNED);
        when(routingService.buildTitle(NotificationType.INCIDENT_ASSIGNED))
                .thenReturn("You have been assigned to incident");

        assertDoesNotThrow(() -> listener.handleIncidentEvent(event));

        // Notification is created with a null incidentId instead of crashing
        verify(notificationRepository, times(2)).save(argThat(n ->
                n.getIncidentId() == null &&
                 n.getUserId().equals(assigneeId) &&
                 n.getType() == NotificationType.INCIDENT_ASSIGNED
        ));
    }
}
