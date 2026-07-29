package com.lautarorisso.notification_service.adapter.out.messaging;

import com.lautarorisso.notification_service.domain.model.Notification;
import com.lautarorisso.notification_service.domain.model.NotificationId;
import com.lautarorisso.notification_service.domain.model.NotificationStatus;
import com.lautarorisso.notification_service.domain.model.NotificationType;
import com.lautarorisso.notification_service.domain.model.ProcessedEvent;
import com.lautarorisso.notification_service.domain.port.out.NotificationRepository;
import com.lautarorisso.notification_service.domain.port.out.ProcessedEventRepository;
import com.lautarorisso.notification_service.domain.service.NotificationRoutingService;
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
 */
@Component
@RequiredArgsConstructor
public class IncidentEventListener {

    private static final Logger log = LoggerFactory.getLogger(IncidentEventListener.class);

    private final ProcessedEventRepository processedEventRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationRoutingService routingService;

    /**
     * Handles an incoming incident event from RabbitMQ.
     *
     * @param event   the event payload as a map
     * @param eventId the unique event/message ID for idempotency
     */
    @RabbitListener(queues = "${notification.rabbitmq.queue:notification.events.queue}")
    public void handleIncidentEvent(Map<String, Object> event, String eventId) {
        log.debug("Received incident event: {} (id={})", event.get("eventType"), eventId);

        // Idempotency check — skip if already processed
        if (processedEventRepository.existsByEventId(eventId)) {
            log.debug("Event {} already processed, skipping", eventId);
            return;
        }

        String eventType = (String) event.get("eventType");
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
        String incidentId = (String) event.get("incidentId");

        for (UUID userId : targets) {
            Notification notification = Notification.builder()
                    .id(new NotificationId(UUID.randomUUID()))
                    .type(notificationType)
                    .userId(userId)
                    .incidentId(incidentId != null ? UUID.fromString(incidentId) : null)
                    .title(title)
                    .message(buildMessage(notificationType, incidentId))
                    .status(NotificationStatus.UNREAD)
                    .createdAt(Instant.now())
                    .build();

            notificationRepository.save(notification);
            log.info("Created notification {} for user {}", notification.getId(), userId);
        }

        // Mark event as processed
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
            case INCIDENT_PRIORITY_CHANGED ->
                    "Incident " + (incidentId != null ? incidentId : "") + " priority has changed";
            case INCIDENT_COMMENT_ADDED ->
                    "A new comment was added to incident " + (incidentId != null ? incidentId : "");
        };
    }
}
