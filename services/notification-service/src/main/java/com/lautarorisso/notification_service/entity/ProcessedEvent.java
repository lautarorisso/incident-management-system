package com.lautarorisso.notification_service.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * MongoDB document mapping the processed_events collection for idempotency tracking.
 * <p>
 * Stores the unique event ID from RabbitMQ messages so that duplicate
 * deliveries can be detected and ignored.
 */
@Document(collection = "processed_events")
@Getter
@ToString
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEvent {

    @Id
    private String eventId;

    private Instant processedAt;
}
