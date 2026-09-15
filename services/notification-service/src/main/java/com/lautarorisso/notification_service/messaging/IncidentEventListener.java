package com.lautarorisso.notification_service.messaging;

import com.lautarorisso.notification_service.entity.Notification;
import com.lautarorisso.notification_service.entity.ProcessedEvent;
import com.lautarorisso.notification_service.enums.NotificationType;
import com.lautarorisso.notification_service.notifier.EmailNotificationSender;
import com.lautarorisso.notification_service.repository.NotificationRepository;
import com.lautarorisso.notification_service.repository.ProcessedEventRepository;
import com.lautarorisso.notification_service.service.NotificationRoutingService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

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
 * <p>
 * Delivery semantics are at-least-once without MongoDB transactions: the dedup
 * row is written after all notifications are persisted, so a crash mid-processing
 * leaves no dedup row and the broker redelivers the message. Duplicates are
 * bounded by the per-user eventId pre-check (see {@link #handleIncidentEvent}).
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
     * <p>
     * At-least-once semantics WITHOUT transactions: standalone Mongo has no
     * transaction support, so there is no atomicity between notification
     * creation and the dedup row. The order below is what bounds duplicates:
     * <ol>
     *   <li>the {@code existsById(eventId)} skip catches redeliveries of events
     *       that COMPLETED (dedup row present);</li>
     *   <li>the per-user {@code existsByEventIdAndUserId} pre-check catches
     *       redeliveries after a crash mid-processing (some notifications already
     *       persisted, no dedup row yet — creating again would duplicate them);</li>
     *   <li>the dedup row is written LAST, only after every notification is
     *       persisted and sent, so a crash mid-processing leaves NO dedup row
     *       and the broker redelivers the message (at-least-once). With the
     *       default prefetch/concurrency of 1 the pre-check is race-free in
     *       practice.</li>
     * </ol>
     *
     * @param event the event payload as a map
     */
    @RabbitListener(queues = "${notification.rabbitmq.queue:notification.events.queue}")
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
        Object rawEventId = event.get("eventId");
        log.debug("Received incident event: {} (id={})", eventType, eventId);

        // Idempotency check — skip if already processed (dedup row from a
        // PREVIOUS successful completion).
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

        String title = routingService.buildTitle(notificationType);

        for (UUID userId : targets) {
            // Per-user redelivery pre-check: a prior partial completion may already
            // have created this user's notification while the crash prevented the
            // dedup row from being written. Legacy events (null eventId) keep the
            // old create-always path — there is no key to dedupe on.
            if (rawEventId != null && notificationRepository.existsByEventIdAndUserId(eventId, userId)) {
                log.debug("Notification for event {} and user {} already exists, skipping", eventId, userId);
                continue;
            }

            Notification notification = Notification.builder()
                    .id(UUID.randomUUID())
                    .type(notificationType)
                    .userId(userId)
                    .incidentId(parseUuid(incidentId))
                    .eventId(rawEventId != null ? rawEventId.toString() : null)
                    .title(title)
                    .message(buildMessage(notificationType, incidentId))
                    .recipientEmail((String) event.get("assigneeEmail"))
                    .createdAt(Instant.now())
                    .build();

            notificationRepository.save(notification);
            log.info("Created notification {} for user {}", notification.getId(), userId);

            // Send the notification via email (or other channels). Email failures
            // do NOT abort the batch or trigger listener retries: the notification
            // stays FAILED and the event is still marked processed, because a
            // redelivery cannot fix an email outage (it would only duplicate work).
            try {
                notificationSender.send(notification);
                notification = notification.markDelivered();
                notificationRepository.save(notification);
            } catch (Exception e) {
                log.warn("Failed to deliver notification {}: {}", notification.getId(), e.getMessage());
                notificationRepository.save(notification.markFailed());
            }
        }

        // Dedup marker written LAST. The old comment claiming "commits atomically"
        // was FALSE — there is no transaction manager here. The row is only written
        // after ALL notifications are persisted and sent, so a crash mid-processing
        // leaves no dedup row and the message is redelivered; duplicate creation is
        // bounded by the existsByEventIdAndUserId pre-check above. Only events that
        // yield notifications are marked.
        ProcessedEvent processed = ProcessedEvent.builder()
                .eventId(eventId)
                .processedAt(Instant.now())
                .build();
        processedEventRepository.save(processed);
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
