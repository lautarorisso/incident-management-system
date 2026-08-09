package com.lautarorisso.notification_service.domain.model;

/**
 * Status indicating the delivery and read state of a notification.
 */
public enum NotificationStatus {
    UNREAD,
    SENT,
    DELIVERED,
    FAILED,
    READ
}
