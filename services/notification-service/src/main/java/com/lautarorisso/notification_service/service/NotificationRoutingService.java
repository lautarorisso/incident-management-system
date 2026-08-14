package com.lautarorisso.notification_service.service;

import com.lautarorisso.notification_service.entity.NotificationType;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Service for resolving notification targets from incident events.
 * <p>
 * Given an incident event (event type + metadata), determines which users
 * should receive a notification and what type of notification to create.
 */
@Service
public class NotificationRoutingService {

    /**
     * Resolves the set of target user IDs that should be notified for an event.
     *
     * @param event a map of event data from the incident event message
     * @return set of user IDs to notify (never null, possibly empty)
     */
    public Set<UUID> resolveTargets(Map<String, Object> event) {
        String eventType = (String) event.get("eventType");
        if (eventType == null) {
            return Collections.emptySet();
        }

        return switch (eventType) {
            case "INCIDENT_ASSIGNED", "INCIDENT_STATUS_CHANGED" ->
                    parseUuid(event.get("assigneeId"));
            default -> Collections.emptySet();
        };
    }

    private Set<UUID> parseUuid(Object raw) {
        if (raw == null) {
            return Collections.emptySet();
        }
        try {
            return Set.of(UUID.fromString(raw.toString()));
        } catch (IllegalArgumentException e) {
            return Collections.emptySet();
        }
    }

    /**
     * Resolves the notification type from an event type string.
     *
     * @param eventType the event type string from the message
     * @return the matching NotificationType, or null if unknown
     */
    public NotificationType resolveNotificationType(String eventType) {
        if (eventType == null) {
            return null;
        }
        return switch (eventType) {
            case "INCIDENT_ASSIGNED" -> NotificationType.INCIDENT_ASSIGNED;
            case "INCIDENT_STATUS_CHANGED" -> NotificationType.INCIDENT_STATUS_CHANGED;
            default -> null;
        };
    }

    /**
     * Builds a human-readable title for a notification type.
     *
     * @param type the notification type
     * @return the title string
     */
    public String buildTitle(NotificationType type) {
        return switch (type) {
            case INCIDENT_ASSIGNED -> "You have been assigned to incident";
            case INCIDENT_STATUS_CHANGED -> "Incident status changed";
        };
    }
}
