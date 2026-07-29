package com.lautarorisso.notification_service.domain.model;

/**
 * Types of notifications that can be generated from incident events.
 */
public enum NotificationType {
    INCIDENT_ASSIGNED,
    INCIDENT_STATUS_CHANGED,
    INCIDENT_PRIORITY_CHANGED,
    INCIDENT_COMMENT_ADDED
}
