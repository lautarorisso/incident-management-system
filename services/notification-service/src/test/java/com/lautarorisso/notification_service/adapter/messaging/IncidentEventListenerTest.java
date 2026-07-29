package com.lautarorisso.notification_service.adapter.messaging;

import com.lautarorisso.notification_service.adapter.out.messaging.IncidentEventListener;
import com.lautarorisso.notification_service.domain.model.NotificationType;
import com.lautarorisso.notification_service.domain.model.ProcessedEvent;
import com.lautarorisso.notification_service.domain.port.out.NotificationRepository;
import com.lautarorisso.notification_service.domain.port.out.ProcessedEventRepository;
import com.lautarorisso.notification_service.domain.service.NotificationRoutingService;
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

    private IncidentEventListener listener;

    @Captor
    private ArgumentCaptor<ProcessedEvent> processedEventCaptor;

    private final String eventId = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        listener = new IncidentEventListener(
                processedEventRepository, notificationRepository, routingService);
    }

    @Test
    void handlesNewEventAndCreatesNotifications() {
        UUID assigneeId = UUID.randomUUID();
        Map<String, Object> event = Map.of(
                "eventType", "INCIDENT_ASSIGNED",
                "incidentId", UUID.randomUUID().toString(),
                "assigneeId", assigneeId.toString(),
                "changedBy", UUID.randomUUID().toString()
        );

        when(processedEventRepository.existsByEventId(eventId)).thenReturn(false);
        when(routingService.resolveTargets(event)).thenReturn(Set.of(assigneeId));
        when(routingService.resolveNotificationType("INCIDENT_ASSIGNED"))
                .thenReturn(NotificationType.INCIDENT_ASSIGNED);
        when(routingService.buildTitle(NotificationType.INCIDENT_ASSIGNED))
                .thenReturn("You have been assigned to incident");

        listener.handleIncidentEvent(event, eventId);

        verify(notificationRepository, times(1)).save(argThat(n ->
                n.getUserId().equals(assigneeId) &&
                n.getType() == NotificationType.INCIDENT_ASSIGNED
        ));
        verify(processedEventRepository).save(processedEventCaptor.capture());
        assertEquals(eventId, processedEventCaptor.getValue().getEventId());
    }

    @Test
    void skipsAlreadyProcessedEvent() {
        when(processedEventRepository.existsByEventId(eventId)).thenReturn(true);

        listener.handleIncidentEvent(Map.of("eventType", "INCIDENT_ASSIGNED"), eventId);

        verify(notificationRepository, never()).save(any());
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void skipsWhenNoTargetsResolved() {
        Map<String, Object> event = Map.of(
                "eventType", "INCIDENT_STATUS_CHANGED",
                "incidentId", UUID.randomUUID().toString()
        );

        when(processedEventRepository.existsByEventId(eventId)).thenReturn(false);
        when(routingService.resolveNotificationType("INCIDENT_STATUS_CHANGED"))
                .thenReturn(NotificationType.INCIDENT_STATUS_CHANGED);
        when(routingService.resolveTargets(event)).thenReturn(Set.of());

        listener.handleIncidentEvent(event, eventId);

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

        when(processedEventRepository.existsByEventId(eventId)).thenReturn(false);
        when(routingService.resolveTargets(event)).thenReturn(Set.of(user1, user2));
        when(routingService.resolveNotificationType("INCIDENT_ASSIGNED"))
                .thenReturn(NotificationType.INCIDENT_ASSIGNED);
        when(routingService.buildTitle(NotificationType.INCIDENT_ASSIGNED))
                .thenReturn("You have been assigned to incident");

        listener.handleIncidentEvent(event, eventId);

        verify(notificationRepository, times(2)).save(argThat(n ->
                n.getUserId().equals(user1) || n.getUserId().equals(user2)
        ));
    }

    @Test
    void handlesUnknownNotificationTypeBySkipping() {
        Map<String, Object> event = Map.of(
                "eventType", "UNKNOWN_EVENT",
                "incidentId", UUID.randomUUID().toString()
        );

        when(processedEventRepository.existsByEventId(eventId)).thenReturn(false);
        when(routingService.resolveNotificationType("UNKNOWN_EVENT")).thenReturn(null);

        listener.handleIncidentEvent(event, eventId);

        verify(notificationRepository, never()).save(any());
        verify(processedEventRepository, never()).save(any());
    }
}
