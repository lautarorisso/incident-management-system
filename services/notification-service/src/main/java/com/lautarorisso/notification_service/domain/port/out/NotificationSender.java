package com.lautarorisso.notification_service.domain.port.out;

import com.lautarorisso.notification_service.domain.model.Notification;

/**
 * Driven port (outbound) for delivering notifications to users.
 * <p>
 * Implementations may send emails, push notifications, or
 * integrate with external notification providers.
 */
public interface NotificationSender {

    void send(Notification notification);
}
