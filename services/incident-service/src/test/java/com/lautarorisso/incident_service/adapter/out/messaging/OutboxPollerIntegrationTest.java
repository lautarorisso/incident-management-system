package com.lautarorisso.incident_service.adapter.out.messaging;

import com.lautarorisso.incident_service.adapter.persistence.entity.OutboxEventEntity;
import com.lautarorisso.incident_service.adapter.persistence.repository.OutboxEventJpaRepository;
import com.lautarorisso.incident_service.domain.model.IncidentEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Integration test for outbox → publish flow using H2 database and mocked RabbitMQ infrastructure.
 * Verifies that the OutboxPoller correctly reads from the database, publishes via
 * RabbitMqEventPublisher, and marks events as published.
 */
@SpringBootTest
@ActiveProfiles("test")
class OutboxPollerIntegrationTest {

    @Autowired
    private OutboxEventJpaRepository outboxRepo;

    @Autowired
    private OutboxPoller outboxPoller;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @MockBean
    private ConnectionFactory connectionFactory;

    private UUID incidentId;

    @BeforeEach
    void setUp() {
        outboxRepo.deleteAll();
        incidentId = UUID.randomUUID();
    }

    @Test
    void shouldPublishOutboxEventAndMarkAsPublished() {
        OutboxEventEntity event = OutboxEventEntity.builder()
                .id(UUID.randomUUID())
                .aggregateId(incidentId)
                .eventType(IncidentEvent.INCIDENT_CREATED.name())
                .payload("{\"incidentId\":\"" + incidentId + "\"}")
                .published(false)
                .createdAt(Instant.now())
                .build();
        outboxRepo.save(event);
        assertThat(outboxRepo.findByPublishedFalse()).hasSize(1);

        outboxPoller.processOutbox();

        // Event should be marked as published
        assertThat(outboxRepo.findByPublishedFalse()).isEmpty();
        assertThat(outboxRepo.count()).isEqualTo(1);
        var saved = outboxRepo.findAll().get(0);
        assertThat(saved.isPublished()).isTrue();

        // RabbitTemplate should have been called
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), Mockito.<Object>any());
    }

    @Test
    void shouldPublishMultipleEventsSequentially() {
        OutboxEventEntity event1 = OutboxEventEntity.builder()
                .id(UUID.randomUUID()).aggregateId(UUID.randomUUID())
                .eventType(IncidentEvent.INCIDENT_CREATED.name()).payload("{}")
                .published(false).createdAt(Instant.now()).build();
        OutboxEventEntity event2 = OutboxEventEntity.builder()
                .id(UUID.randomUUID()).aggregateId(UUID.randomUUID())
                .eventType(IncidentEvent.INCIDENT_ASSIGNED.name()).payload("{}")
                .published(false).createdAt(Instant.now()).build();
        outboxRepo.save(event1);
        outboxRepo.save(event2);

        outboxPoller.processOutbox();

        // Both should be published
        assertThat(outboxRepo.findByPublishedFalse()).isEmpty();
        assertThat(outboxRepo.count()).isEqualTo(2);
        verify(rabbitTemplate, times(2)).convertAndSend(anyString(), anyString(), Mockito.<Object>any());
    }

    @Test
    void shouldNotFailWhenNoEventsExist() {
        outboxPoller.processOutbox();

        assertThat(outboxRepo.findByPublishedFalse()).isEmpty();
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), Mockito.<Object>any());
    }
}
