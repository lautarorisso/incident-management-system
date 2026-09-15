package com.lautarorisso.notification_service.enums;

/**
 * Types of notifications that can be generated from incident events.
 */
public enum NotificationType {
    INCIDENT_ASSIGNED,
    INCIDENT_STATUS_CHANGED;

    /**
     * Maps an incident event type string to a NotificationType, or {@code null}
     * when the event is unknown. Single source of truth for event-to-type
     * resolution — the routing service must delegate here rather than
     * duplicating the mapping in its own switch.
     *
     * @param eventType the raw event type string from the outbox payload
     * @return the matching {@link NotificationType}, or {@code null}
     */
    public static NotificationType fromEventType(String eventType) {
        if (eventType == null) {
            return null;
        }
        return switch (eventType) {
            case "INCIDENT_ASSIGNED" -> INCIDENT_ASSIGNED;
            case "INCIDENT_STATUS_CHANGED" -> INCIDENT_STATUS_CHANGED;
            default -> null;
        };
    }
}
