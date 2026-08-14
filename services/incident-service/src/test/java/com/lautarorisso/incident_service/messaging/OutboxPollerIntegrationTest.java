package com.lautarorisso.incident_service.messaging;

import com.lautarorisso.incident_service.entity.IncidentEvent;
import com.lautarorisso.incident_service.entity.OutboxEvent;
import com.lautarorisso.incident_service.repository.OutboxEventRepository;
import com.lautarorisso.incident_service.service.OutboxPoller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.mockito.Mockito;

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
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private OutboxPoller outboxPoller;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @MockBean
    private ConnectionFactory connectionFactory;

    private UUID incidentId;

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAll();
        incidentId = UUID.randomUUID();
    }

    @Test
    void shouldPublishOutboxEventAndMarkAsPublished() {
        OutboxEvent event = new OutboxEvent();
        event.setId(UUID.randomUUID());
        event.setAggregateId(incidentId);
        event.setEventType(IncidentEvent.INCIDENT_CREATED.name());
        event.setPayload("{\"incidentId\":\"" + incidentId + "\",\"title\":\"Test\",\"status\":\"OPEN\",\"priority\":\"HIGH\"}");
        event.setPublished(false);
        event.setCreatedAt(Instant.now());
        outboxEventRepository.save(event);
        assertThat(outboxEventRepository.findByPublishedFalse()).hasSize(1);

        outboxPoller.processOutbox();

        assertThat(outboxEventRepository.findByPublishedFalse()).isEmpty();
        assertThat(outboxEventRepository.count()).isEqualTo(1);
        var saved = outboxEventRepository.findAll().get(0);
        assertThat(saved.isPublished()).isTrue();

        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), Mockito.<Object>any());
    }

    @Test
    void shouldPublishMultipleEventsSequentially() {
        OutboxEvent event1 = new OutboxEvent();
        event1.setId(UUID.randomUUID());
        event1.setAggregateId(UUID.randomUUID());
        event1.setEventType(IncidentEvent.INCIDENT_CREATED.name());
        event1.setPayload("{}");
        event1.setPublished(false);
        event1.setCreatedAt(Instant.now());
        OutboxEvent event2 = new OutboxEvent();
        event2.setId(UUID.randomUUID());
        event2.setAggregateId(UUID.randomUUID());
        event2.setEventType(IncidentEvent.INCIDENT_ASSIGNED.name());
        event2.setPayload("{}");
        event2.setPublished(false);
        event2.setCreatedAt(Instant.now());
        outboxEventRepository.save(event1);
        outboxEventRepository.save(event2);

        outboxPoller.processOutbox();

        assertThat(outboxEventRepository.findByPublishedFalse()).isEmpty();
        assertThat(outboxEventRepository.count()).isEqualTo(2);
        verify(rabbitTemplate, times(2)).convertAndSend(anyString(), anyString(), Mockito.<Object>any());
    }

    @Test
    void shouldNotFailWhenNoEventsExist() {
        outboxPoller.processOutbox();

        assertThat(outboxEventRepository.findByPublishedFalse()).isEmpty();
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), Mockito.<Object>any());
    }
}
