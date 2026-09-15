package com.lautarorisso.notification_service.messaging;

import com.lautarorisso.notification_service.entity.Notification;
import com.lautarorisso.notification_service.enums.NotificationDeliveryStatus;
import com.lautarorisso.notification_service.enums.NotificationReadStatus;
import com.lautarorisso.notification_service.enums.NotificationType;
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
        String eventId = UUID.randomUUID().toString();
        Map<String, Object> event = Map.of(
                "eventType", "INCIDENT_ASSIGNED",
                "eventId", eventId,
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
        // Legacy event without eventId: falls back to (incidentId + eventType)
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
        String eventId = UUID.randomUUID().toString();
        Map<String, Object> event = Map.of(
                "eventType", "INCIDENT_STATUS_CHANGED",
                "eventId", eventId,
                "incidentId", UUID.randomUUID().toString()
        );

        when(processedEventRepository.existsById(eventId)).thenReturn(false);
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
        String eventId = UUID.randomUUID().toString();
        Map<String, Object> event = Map.of(
                "eventType", "INCIDENT_ASSIGNED",
                "eventId", eventId,
                "incidentId", UUID.randomUUID().toString(),
                "assigneeId", user1.toString()
        );

        when(processedEventRepository.existsById(eventId)).thenReturn(false);
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
        String eventId = UUID.randomUUID().toString();
        Map<String, Object> event = Map.of(
                "eventType", "UNKNOWN_EVENT",
                "eventId", eventId,
                "incidentId", UUID.randomUUID().toString()
        );

        when(processedEventRepository.existsById(eventId)).thenReturn(false);
        when(routingService.resolveNotificationType("UNKNOWN_EVENT")).thenReturn(null);

        listener.handleIncidentEvent(event);

        verify(notificationRepository, never()).save(any());
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void handlesMalformedIncidentIdWithoutCrashing() {
        UUID assigneeId = UUID.randomUUID();
        String incidentId = "not-a-uuid";
        String eventId = UUID.randomUUID().toString();
        Map<String, Object> event = Map.of(
                "eventType", "INCIDENT_ASSIGNED",
                "eventId", eventId,
                "incidentId", incidentId,
                "assigneeId", assigneeId.toString()
        );

        when(processedEventRepository.existsById(eventId)).thenReturn(false);
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

    @Test
    void twoStatusChangesOfSameIncidentAreNotDedupedWhenEventIdsDiffer() {
        UUID assigneeId = UUID.randomUUID();
        String incidentId = UUID.randomUUID().toString();
        String firstEventId = UUID.randomUUID().toString();
        String secondEventId = UUID.randomUUID().toString();

        Map<String, Object> firstChange = Map.of(
                "eventType", "INCIDENT_STATUS_CHANGED",
                "eventId", firstEventId,
                "incidentId", incidentId,
                "assigneeId", assigneeId.toString()
        );
        Map<String, Object> secondChange = Map.of(
                "eventType", "INCIDENT_STATUS_CHANGED",
                "eventId", secondEventId,
                "incidentId", incidentId,
                "assigneeId", assigneeId.toString()
        );

        when(processedEventRepository.existsById(firstEventId)).thenReturn(false);
        when(processedEventRepository.existsById(secondEventId)).thenReturn(false);
        when(routingService.resolveTargets(firstChange)).thenReturn(Set.of(assigneeId));
        when(routingService.resolveTargets(secondChange)).thenReturn(Set.of(assigneeId));
        when(routingService.resolveNotificationType("INCIDENT_STATUS_CHANGED"))
                .thenReturn(NotificationType.INCIDENT_STATUS_CHANGED);
        when(routingService.buildTitle(NotificationType.INCIDENT_STATUS_CHANGED))
                .thenReturn("Incident status has changed");

        listener.handleIncidentEvent(firstChange);
        listener.handleIncidentEvent(secondChange);

        // 2 notifications per event (UNREAD + SENT), never deduped away
        verify(notificationRepository, times(4)).save(any());
        verify(processedEventRepository).save(argThat(p ->
                p.getEventId().equals(firstEventId)));
        verify(processedEventRepository).save(argThat(p ->
                p.getEventId().equals(secondEventId)));
    }

    @Test
    void senderFailureMarksNotificationFailed() {
        UUID assigneeId = UUID.randomUUID();
        String incidentId = UUID.randomUUID().toString();
        String eventId = UUID.randomUUID().toString();
        Map<String, Object> event = Map.of(
                "eventType", "INCIDENT_ASSIGNED",
                "eventId", eventId,
                "incidentId", incidentId,
                "assigneeId", assigneeId.toString()
        );

        when(processedEventRepository.existsById(eventId)).thenReturn(false);
        when(routingService.resolveTargets(event)).thenReturn(Set.of(assigneeId));
        when(routingService.resolveNotificationType("INCIDENT_ASSIGNED"))
                .thenReturn(NotificationType.INCIDENT_ASSIGNED);
        when(routingService.buildTitle(NotificationType.INCIDENT_ASSIGNED))
                .thenReturn("You have been assigned to incident");
        doThrow(new RuntimeException("SMTP down"))
                .when(notificationSender).send(any(Notification.class));

        listener.handleIncidentEvent(event);

        // save called twice: UNREAD+PENDING (initial) + UNREAD+FAILED (sender error)
        verify(notificationRepository, times(2)).save(argThat(n ->
                n.getUserId().equals(assigneeId)));
        // Verify the exact (delivery, read) pairs saved: the initial UNREAD
        // notification (delivery still PENDING at save time) and the FAILED
        // replacement — never SENT.
        verify(notificationRepository).save(argThat(n ->
                n.getDeliveryStatus() == NotificationDeliveryStatus.PENDING
                        && n.getReadStatus() == NotificationReadStatus.UNREAD));
        verify(notificationRepository).save(argThat(n ->
                n.getDeliveryStatus() == NotificationDeliveryStatus.FAILED
                        && n.getReadStatus() == NotificationReadStatus.UNREAD));
    }

    @Test
    void buildMessageContainsIncidentIdForAssigned() {
        UUID assigneeId = UUID.randomUUID();
        String incidentId = UUID.randomUUID().toString();
        String eventId = UUID.randomUUID().toString();
        Map<String, Object> event = Map.of(
                "eventType", "INCIDENT_ASSIGNED",
                "eventId", eventId,
                "incidentId", incidentId,
                "assigneeId", assigneeId.toString()
        );

        when(processedEventRepository.existsById(eventId)).thenReturn(false);
        when(routingService.resolveTargets(event)).thenReturn(Set.of(assigneeId));
        when(routingService.resolveNotificationType("INCIDENT_ASSIGNED"))
                .thenReturn(NotificationType.INCIDENT_ASSIGNED);
        when(routingService.buildTitle(NotificationType.INCIDENT_ASSIGNED))
                .thenReturn("You have been assigned to incident");

        listener.handleIncidentEvent(event);

        verify(notificationRepository, atLeastOnce()).save(argThat(n ->
                n.getMessage().contains(incidentId) &&
                 n.getType() == NotificationType.INCIDENT_ASSIGNED));
    }

    @Test
    void buildMessageContainsIncidentIdForStatusChanged() {
        UUID assigneeId = UUID.randomUUID();
        String incidentId = UUID.randomUUID().toString();
        String eventId = UUID.randomUUID().toString();
        Map<String, Object> event = Map.of(
                "eventType", "INCIDENT_STATUS_CHANGED",
                "eventId", eventId,
                "incidentId", incidentId,
                "assigneeId", assigneeId.toString()
        );

        when(processedEventRepository.existsById(eventId)).thenReturn(false);
        when(routingService.resolveTargets(event)).thenReturn(Set.of(assigneeId));
        when(routingService.resolveNotificationType("INCIDENT_STATUS_CHANGED"))
                .thenReturn(NotificationType.INCIDENT_STATUS_CHANGED);
        when(routingService.buildTitle(NotificationType.INCIDENT_STATUS_CHANGED))
                .thenReturn("Incident status has changed");

        listener.handleIncidentEvent(event);

        verify(notificationRepository, atLeastOnce()).save(argThat(n ->
                n.getMessage().contains(incidentId) &&
                 n.getType() == NotificationType.INCIDENT_STATUS_CHANGED));
    }

    @Test
    void redeliveryAfterPartialCompletionDoesNotDuplicateExistingNotification() {
        UUID assigneeId = UUID.randomUUID();
        String incidentId = UUID.randomUUID().toString();
        String eventId = UUID.randomUUID().toString();
        Map<String, Object> event = Map.of(
                "eventType", "INCIDENT_ASSIGNED",
                "eventId", eventId,
                "incidentId", incidentId,
                "assigneeId", assigneeId.toString()
        );

        // No dedup row yet (a crash prevented the final save), but the user's
        // notification was already created during the partial completion.
        when(processedEventRepository.existsById(eventId)).thenReturn(false);
        when(notificationRepository.existsByEventIdAndUserId(eventId, assigneeId)).thenReturn(true);
        when(routingService.resolveTargets(event)).thenReturn(Set.of(assigneeId));
        when(routingService.resolveNotificationType("INCIDENT_ASSIGNED"))
                .thenReturn(NotificationType.INCIDENT_ASSIGNED);
        when(routingService.buildTitle(NotificationType.INCIDENT_ASSIGNED))
                .thenReturn("You have been assigned to incident");

        listener.handleIncidentEvent(event);

        // No new notification is created — the per-user pre-check bounds the duplicate.
        verify(notificationRepository, never()).save(any());
        // ... but the event IS now marked processed, completing the delivery.
        verify(processedEventRepository).save(processedEventCaptor.capture());
        assertEquals(eventId, processedEventCaptor.getValue().getEventId());
    }

    @Test
    void failedFirstAttemptLeavesNoDedupRowAndSecondAttemptCreatesExactlyOneNotification() {
        UUID assigneeId = UUID.randomUUID();
        String incidentId = UUID.randomUUID().toString();
        String eventId = UUID.randomUUID().toString();
        Map<String, Object> event = Map.of(
                "eventType", "INCIDENT_ASSIGNED",
                "eventId", eventId,
                "incidentId", incidentId,
                "assigneeId", assigneeId.toString()
        );

        when(processedEventRepository.existsById(eventId)).thenReturn(false);
        when(routingService.resolveNotificationType("INCIDENT_ASSIGNED"))
                .thenReturn(NotificationType.INCIDENT_ASSIGNED);
        when(routingService.buildTitle(NotificationType.INCIDENT_ASSIGNED))
                .thenReturn("You have been assigned to incident");

        // First attempt fails mid-processing BEFORE creating any notification
        // (target resolution blows up after the type resolved). No dedup row is written.
        doThrow(new RuntimeException("broker hiccup")).when(routingService).resolveTargets(event);
        assertThrows(RuntimeException.class, () -> listener.handleIncidentEvent(event));
        verify(notificationRepository, never()).save(any());
        verify(processedEventRepository, never()).save(any());

        // Redelivery: second attempt completes, creating exactly one notification
        // (UNREAD + SENT saves) and finally the dedup row. doReturn bypasses the
        // active throw-stub (a when() re-stub would invoke the method and throw).
        doReturn(Set.of(assigneeId)).when(routingService).resolveTargets(event);

        listener.handleIncidentEvent(event);

        verify(notificationRepository, times(2)).save(argThat(n ->
                n.getUserId().equals(assigneeId) &&
                 n.getType() == NotificationType.INCIDENT_ASSIGNED));
        verify(processedEventRepository).save(processedEventCaptor.capture());
        assertEquals(eventId, processedEventCaptor.getValue().getEventId());
    }

    @Test
    void copiesAssigneeEmailFromEventOntoNotification() {
        UUID assigneeId = UUID.randomUUID();
        String incidentId = UUID.randomUUID().toString();
        String eventId = UUID.randomUUID().toString();
        Map<String, Object> event = Map.of(
                "eventType", "INCIDENT_ASSIGNED",
                "eventId", eventId,
                "incidentId", incidentId,
                "assigneeId", assigneeId.toString(),
                "assigneeEmail", "jdoe@example.com"
        );

        when(processedEventRepository.existsById(eventId)).thenReturn(false);
        when(routingService.resolveTargets(event)).thenReturn(Set.of(assigneeId));
        when(routingService.resolveNotificationType("INCIDENT_ASSIGNED"))
                .thenReturn(NotificationType.INCIDENT_ASSIGNED);
        when(routingService.buildTitle(NotificationType.INCIDENT_ASSIGNED))
                .thenReturn("You have been assigned to incident");

        listener.handleIncidentEvent(event);

        verify(notificationRepository, atLeastOnce()).save(argThat(n ->
                "jdoe@example.com".equals(n.getRecipientEmail())));
    }

    @Test
    void legacyEventWithoutAssigneeEmailStoresNullRecipientEmail() {
        UUID assigneeId = UUID.randomUUID();
        String incidentId = UUID.randomUUID().toString();
        String eventId = UUID.randomUUID().toString();
        Map<String, Object> event = Map.of(
                "eventType", "INCIDENT_ASSIGNED",
                "eventId", eventId,
                "incidentId", incidentId,
                "assigneeId", assigneeId.toString()
        );

        when(processedEventRepository.existsById(eventId)).thenReturn(false);
        when(routingService.resolveTargets(event)).thenReturn(Set.of(assigneeId));
        when(routingService.resolveNotificationType("INCIDENT_ASSIGNED"))
                .thenReturn(NotificationType.INCIDENT_ASSIGNED);
        when(routingService.buildTitle(NotificationType.INCIDENT_ASSIGNED))
                .thenReturn("You have been assigned to incident");

        listener.handleIncidentEvent(event);

        verify(notificationRepository, atLeastOnce()).save(argThat(n ->
                n.getRecipientEmail() == null));
    }

    @Test
    void legacyEventWithoutEventIdCreatesNotificationWithoutEventIdField() {
        UUID assigneeId = UUID.randomUUID();
        String incidentId = UUID.randomUUID().toString();
        Map<String, Object> legacyEvent = Map.of(
                "eventType", "INCIDENT_ASSIGNED",
                "incidentId", incidentId,
                "assigneeId", assigneeId.toString()
        );

        when(processedEventRepository.existsById(eventIdFor(incidentId, "INCIDENT_ASSIGNED")))
                .thenReturn(false);
        when(routingService.resolveTargets(legacyEvent)).thenReturn(Set.of(assigneeId));
        when(routingService.resolveNotificationType("INCIDENT_ASSIGNED"))
                .thenReturn(NotificationType.INCIDENT_ASSIGNED);
        when(routingService.buildTitle(NotificationType.INCIDENT_ASSIGNED))
                .thenReturn("You have been assigned to incident");

        listener.handleIncidentEvent(legacyEvent);

        // Legacy events skip the per-user pre-check (no key) and store no eventId
        verify(notificationRepository, times(2)).save(argThat(n ->
                n.getUserId().equals(assigneeId) && n.getEventId() == null));
        verify(processedEventRepository).save(processedEventCaptor.capture());
        assertEquals(eventIdFor(incidentId, "INCIDENT_ASSIGNED"), processedEventCaptor.getValue().getEventId());
    }
}
