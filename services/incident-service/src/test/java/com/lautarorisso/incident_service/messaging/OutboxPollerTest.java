package com.lautarorisso.incident_service.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lautarorisso.incident_service.entity.IncidentEvent;
import com.lautarorisso.incident_service.entity.OutboxEvent;
import com.lautarorisso.incident_service.repository.OutboxEventRepository;
import com.lautarorisso.incident_service.service.OutboxPoller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link OutboxPoller} — reads unpublished outbox events, publishes them,
 * and marks them as published.
 */
@ExtendWith(MockitoExtension.class)
class OutboxPollerTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private RabbitMqEventPublisher eventPublisher;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private OutboxPoller poller;

    @Captor
    private ArgumentCaptor<IncidentEvent> eventTypeCaptor;

    @Captor
    private ArgumentCaptor<Map<String, Object>> eventDataCaptor;

    @BeforeEach
    void setUp() {
        poller = new OutboxPoller(outboxEventRepository, eventPublisher, objectMapper);
    }

    @Test
    void shouldPublishUnpublishedEventsAndMarkThemPublished() {
        UUID incidentId = UUID.randomUUID();
        OutboxEvent event = new OutboxEvent();
        event.setId(UUID.randomUUID());
        event.setAggregateId(incidentId);
        event.setEventType(IncidentEvent.INCIDENT_CREATED.name());
        event.setPayload("{\"incidentId\":\"" + incidentId + "\",\"title\":\"Test\",\"status\":\"OPEN\",\"priority\":\"HIGH\"}");
        event.setPublished(false);
        event.setCreatedAt(Instant.now());
        when(outboxEventRepository.findByPublishedFalse()).thenReturn(List.of(event));

        poller.processOutbox();

        verify(eventPublisher).publish(eventTypeCaptor.capture(), eventDataCaptor.capture());
        assertThat(eventTypeCaptor.getValue()).isEqualTo(IncidentEvent.INCIDENT_CREATED);
        assertThat(eventDataCaptor.getValue()).containsEntry("incidentId", incidentId.toString());
        assertThat(event.isPublished()).isTrue();
        verify(outboxEventRepository).save(event);
    }

    @Test
    void shouldPublishMultipleEventsInOneBatch() {
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
        when(outboxEventRepository.findByPublishedFalse()).thenReturn(List.of(event1, event2));

        poller.processOutbox();

        verify(eventPublisher, times(2)).publish(any(), any());
        verify(outboxEventRepository, times(2)).save(any());
    }

    @Test
    void shouldDoNothingWhenNoUnpublishedEvents() {
        when(outboxEventRepository.findByPublishedFalse()).thenReturn(List.of());

        poller.processOutbox();

        verify(eventPublisher, never()).publish(any(), any());
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    void shouldHandleDifferentEventTypes() {
        UUID incidentId = UUID.randomUUID();
        OutboxEvent event = new OutboxEvent();
        event.setId(UUID.randomUUID());
        event.setAggregateId(incidentId);
        event.setEventType(IncidentEvent.INCIDENT_STATUS_CHANGED.name());
        event.setPayload("{\"incidentId\":\"" + incidentId + "\"}");
        event.setPublished(false);
        event.setCreatedAt(Instant.now());
        when(outboxEventRepository.findByPublishedFalse()).thenReturn(List.of(event));

        poller.processOutbox();

        verify(eventPublisher).publish(
                eventTypeCaptor.capture(),
                eventDataCaptor.capture()
        );
        assertThat(eventTypeCaptor.getValue()).isEqualTo(IncidentEvent.INCIDENT_STATUS_CHANGED);
        assertThat(eventDataCaptor.getValue()).containsEntry("incidentId", incidentId.toString());
    }

    @Test
    void shouldContinueProcessingAfterError() {
        UUID incidentId1 = UUID.randomUUID();
        UUID incidentId2 = UUID.randomUUID();

        OutboxEvent badEvent = new OutboxEvent();
        badEvent.setId(UUID.randomUUID());
        badEvent.setAggregateId(incidentId1);
        badEvent.setEventType("INVALID_EVENT_TYPE");
        badEvent.setPayload("{}");
        badEvent.setPublished(false);
        badEvent.setCreatedAt(Instant.now());

        OutboxEvent goodEvent = new OutboxEvent();
        goodEvent.setId(UUID.randomUUID());
        goodEvent.setAggregateId(incidentId2);
        goodEvent.setEventType(IncidentEvent.INCIDENT_CREATED.name());
        goodEvent.setPayload("{\"incidentId\":\"" + incidentId2 + "\"}");
        goodEvent.setPublished(false);
        goodEvent.setCreatedAt(Instant.now());

        when(outboxEventRepository.findByPublishedFalse()).thenReturn(List.of(badEvent, goodEvent));

        poller.processOutbox();

        verify(eventPublisher).publish(eq(IncidentEvent.INCIDENT_CREATED), any());
        verify(outboxEventRepository, never()).save(badEvent);
        verify(outboxEventRepository).save(goodEvent);
    }
}
