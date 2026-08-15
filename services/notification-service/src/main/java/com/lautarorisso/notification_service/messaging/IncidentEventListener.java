package com.lautarorisso.notification_service.messaging;

import com.lautarorisso.notification_service.entity.Notification;
import com.lautarorisso.notification_service.entity.NotificationStatus;
import com.lautarorisso.notification_service.entity.NotificationType;
import com.lautarorisso.notification_service.entity.ProcessedEvent;
import com.lautarorisso.notification_service.notifier.EmailNotificationSender;
import com.lautarorisso.notification_service.repository.NotificationRepository;
import com.lautarorisso.notification_service.repository.ProcessedEventRepository;
import com.lautarorisso.notification_service.service.NotificationRoutingService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * RabbitMQ message listener for incident events.
 * <p>
 * Consumes events from the incident.events exchange, resolves notification
 * targets, and persists notifications. Idempotency is ensured by tracking
 * processed event IDs.
 */
@Component
@RequiredArgsConstructor
public class IncidentEventListener {

    private static final Logger log = LoggerFactory.getLogger(IncidentEventListener.class);

    private final ProcessedEventRepository processedEventRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationRoutingService routingService;
    private final EmailNotificationSender notificationSender;

    /**
     * Handles an incoming incident event from RabbitMQ.
     *
     * @param event the event payload as a map
     */
    @RabbitListener(queues = "${notification.rabbitmq.queue:notification.events.queue}")
    @Transactional
    public void handleIncidentEvent(Map<String, Object> event) {
        String incidentId = (String) event.get("incidentId");
        String eventType = (String) event.get("eventType");
        // Stable idempotency key: the outbox publisher stamps each event with a
        // unique eventId. The fallback (incidentId + eventType) only applies to
        // legacy events published before that field existed; two status changes of
        // the same incident must NOT share a key, or the second one is dropped.
        String eventId = event.get("eventId") != null
                ? event.get("eventId").toString()
                : incidentId + ":" + eventType;
        log.debug("Received incident event: {} (id={})", eventType, eventId);

        // Idempotency check — skip if already processed
        if (processedEventRepository.existsById(eventId)) {
            log.debug("Event {} already processed, skipping", eventId);
            return;
        }

        NotificationType notificationType = routingService.resolveNotificationType(eventType);
        if (notificationType == null) {
            log.warn("Unknown event type: {}, skipping", eventType);
            return;
        }

        Set<UUID> targets = routingService.resolveTargets(event);
        if (targets.isEmpty()) {
            log.debug("No targets resolved for event {}, skipping", eventId);
            return;
        }

        // Mark event as processed (fail-fast idempotency: write dedup row before
        // creating notifications within the same tx so a crash/rollback leaves no
        // row for clean retry; a success commits atomically and a redelivery hits
        // existsById=true). Only events that yield notifications are marked.
        ProcessedEvent processed = ProcessedEvent.builder()
                .eventId(eventId)
                .processedAt(Instant.now())
                .build();
        processedEventRepository.save(processed);

        String title = routingService.buildTitle(notificationType);

        for (UUID userId : targets) {
            Notification notification = Notification.builder()
                    .id(UUID.randomUUID())
                    .type(notificationType)
                    .userId(userId)
                    .incidentId(parseUuid(incidentId))
                    .title(title)
                    .message(buildMessage(notificationType, incidentId))
                    .status(NotificationStatus.UNREAD)
                    .createdAt(Instant.now())
                    .build();

            notificationRepository.save(notification);
            log.info("Created notification {} for user {}", notification.getId(), userId);

            // Send the notification via email (or other channels)
            try {
                notificationSender.send(notification);
                notification = notification.markAsSent();
                notificationRepository.save(notification);
            } catch (Exception e) {
                log.error("Failed to deliver notification {}: {}", notification.getId(), e.getMessage());
                notificationRepository.save(notification.markAsFailed());
            }
        }
    }

    private String buildMessage(NotificationType type, String incidentId) {
        return switch (type) {
            case INCIDENT_ASSIGNED ->
                    "You have been assigned to incident " + (incidentId != null ? incidentId : "");
            case INCIDENT_STATUS_CHANGED ->
                    "Incident " + (incidentId != null ? incidentId : "") + " status has changed";
        };
    }

    private UUID parseUuid(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid incidentId in event: {}", raw);
            return null;
        }
    }
}
