package com.lautarorisso.notification_service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;

/**
 * Domain model representing a processed event for idempotency tracking.
 * <p>
 * Stores the unique event ID from RabbitMQ messages so that duplicate
 * deliveries can be detected and ignored.
 */
@Getter
@ToString
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEvent {

    private String eventId;
    private Instant processedAt;
}
