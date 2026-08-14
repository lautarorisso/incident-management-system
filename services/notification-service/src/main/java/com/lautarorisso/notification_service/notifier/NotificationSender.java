package com.lautarorisso.notification_service.notifier;

/**
 * Driven port for delivering notifications to users.
 * <p>
 * Implementations may send emails, push notifications, or
 * integrate with external notification providers.
 */
public interface NotificationSender {

    void send(com.lautarorisso.notification_service.entity.Notification notification);
}
