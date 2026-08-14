package com.lautarorisso.notification_service.service;

import com.lautarorisso.notification_service.entity.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NotificationRoutingServiceTest {

    private NotificationRoutingService routingService;

    @BeforeEach
    void setUp() {
        routingService = new NotificationRoutingService();
    }

    @Test
    void incidentAssignedRoutesToAssignee() {
        UUID assigneeId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();

        Map<String, Object> event = Map.of(
                "eventType", "INCIDENT_ASSIGNED",
                "incidentId", UUID.randomUUID().toString(),
                "assigneeId", assigneeId.toString(),
                "teamId", teamId.toString(),
                "changedBy", UUID.randomUUID().toString()
        );

        Set<UUID> targets = routingService.resolveTargets(event);

        assertEquals(1, targets.size());
        assertTrue(targets.contains(assigneeId));
    }

    @Test
    void incidentAssignedRoutesToAssigneeAndExcludesChangedBy() {
        UUID changedBy = UUID.randomUUID();
        UUID assigneeId = UUID.randomUUID();

        Map<String, Object> event = Map.of(
                "eventType", "INCIDENT_ASSIGNED",
                "incidentId", UUID.randomUUID().toString(),
                "assigneeId", assigneeId.toString(),
                "changedBy", changedBy.toString()
        );

        Set<UUID> targets = routingService.resolveTargets(event);

        assertTrue(targets.contains(assigneeId));
        assertFalse(targets.contains(changedBy));
    }

    @Test
    void incidentStatusChangedRoutesToAssignee() {
        UUID assigneeId = UUID.randomUUID();

        Map<String, Object> event = Map.of(
                "eventType", "INCIDENT_STATUS_CHANGED",
                "incidentId", UUID.randomUUID().toString(),
                "assigneeId", assigneeId.toString(),
                "changedBy", UUID.randomUUID().toString()
        );

        Set<UUID> targets = routingService.resolveTargets(event);

        assertEquals(1, targets.size());
        assertTrue(targets.contains(assigneeId));
    }

    @Test
    void incidentStatusChangedWithNoAssigneeReturnsEmpty() {
        Map<String, Object> event = Map.of(
                "eventType", "INCIDENT_STATUS_CHANGED",
                "incidentId", UUID.randomUUID().toString(),
                "changedBy", UUID.randomUUID().toString()
        );

        Set<UUID> targets = routingService.resolveTargets(event);

        assertTrue(targets.isEmpty());
    }

    @Test
    void unknownEventTypeReturnsEmpty() {
        Map<String, Object> event = Map.of(
                "eventType", "UNKNOWN_EVENT",
                "incidentId", UUID.randomUUID().toString()
        );

        Set<UUID> targets = routingService.resolveTargets(event);

        assertTrue(targets.isEmpty());
    }

    @Test
    void eventWithoutEventTypeReturnsEmpty() {
        Map<String, Object> event = Map.of(
                "incidentId", UUID.randomUUID().toString()
        );

        Set<UUID> targets = routingService.resolveTargets(event);

        assertTrue(targets.isEmpty());
    }

    @Test
    void malformedAssigneeIdReturnsEmptyInsteadOfThrowing() {
        Map<String, Object> event = Map.of(
                "eventType", "INCIDENT_ASSIGNED",
                "incidentId", UUID.randomUUID().toString(),
                "assigneeId", "not-a-uuid"
        );

        Set<UUID> targets = routingService.resolveTargets(event);

        assertTrue(targets.isEmpty());
    }

    @Test
    void resolveNotificationTypeReturnsCorrectType() {
        assertEquals(NotificationType.INCIDENT_ASSIGNED,
                routingService.resolveNotificationType("INCIDENT_ASSIGNED"));
        assertEquals(NotificationType.INCIDENT_STATUS_CHANGED,
                routingService.resolveNotificationType("INCIDENT_STATUS_CHANGED"));
    }

    @Test
    void resolveNotificationTypeReturnsNullForUnknownType() {
        assertNull(routingService.resolveNotificationType("UNKNOWN_EVENT"));
    }

    @Test
    void buildTitleReturnsDescriptiveTitle() {
        assertEquals("You have been assigned to incident",
                routingService.buildTitle(NotificationType.INCIDENT_ASSIGNED));
        assertEquals("Incident status changed",
                routingService.buildTitle(NotificationType.INCIDENT_STATUS_CHANGED));
    }
}
