package com.lautarorisso.incident_service.adapter.out.messaging;

import com.lautarorisso.incident_service.adapter.persistence.entity.OutboxEventEntity;
import com.lautarorisso.incident_service.adapter.persistence.repository.OutboxEventJpaRepository;
import com.lautarorisso.incident_service.domain.model.IncidentEvent;
import com.lautarorisso.incident_service.domain.model.IncidentId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Tests for OutboxPoller — reads unpublished outbox events, publishes them,
 * and marks them as published.
 */
@ExtendWith(MockitoExtension.class)
class OutboxPollerTest {

    @Mock
    private OutboxEventJpaRepository outboxRepo;

    @Mock
    private RabbitMqEventPublisher eventPublisher;

    private OutboxPoller poller;

    @Captor
    private ArgumentCaptor<IncidentEvent> eventTypeCaptor;

    @Captor
    private ArgumentCaptor<IncidentId> incidentIdCaptor;

    @BeforeEach
    void setUp() {
        poller = new OutboxPoller(outboxRepo, eventPublisher);
    }

    @Test
    void shouldPublishUnpublishedEventsAndMarkThemPublished() {
        UUID incidentId = UUID.randomUUID();
        OutboxEventEntity event = OutboxEventEntity.builder()
                .id(UUID.randomUUID())
                .aggregateId(incidentId)
                .eventType(IncidentEvent.INCIDENT_CREATED.name())
                .payload("{\"incidentId\":\"" + incidentId + "\"}")
                .published(false)
                .createdAt(Instant.now())
                .build();
        when(outboxRepo.findByPublishedFalse()).thenReturn(List.of(event));

        poller.processOutbox();

        verify(eventPublisher).publish(IncidentEvent.INCIDENT_CREATED, new IncidentId(incidentId));
        assertThat(event.isPublished()).isTrue();
        verify(outboxRepo).save(event);
    }

    @Test
    void shouldPublishMultipleEventsInOneBatch() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        OutboxEventEntity event1 = OutboxEventEntity.builder()
                .id(UUID.randomUUID()).aggregateId(id1)
                .eventType(IncidentEvent.INCIDENT_CREATED.name()).payload("{}")
                .published(false).createdAt(Instant.now()).build();
        OutboxEventEntity event2 = OutboxEventEntity.builder()
                .id(UUID.randomUUID()).aggregateId(id2)
                .eventType(IncidentEvent.INCIDENT_ASSIGNED.name()).payload("{}")
                .published(false).createdAt(Instant.now()).build();
        when(outboxRepo.findByPublishedFalse()).thenReturn(List.of(event1, event2));

        poller.processOutbox();

        verify(eventPublisher, times(2)).publish(any(), any());
        verify(outboxRepo, times(2)).save(any());
    }

    @Test
    void shouldDoNothingWhenNoUnpublishedEvents() {
        when(outboxRepo.findByPublishedFalse()).thenReturn(List.of());

        poller.processOutbox();

        verify(eventPublisher, never()).publish(any(), any());
        verify(outboxRepo, never()).save(any());
    }

    @Test
    void shouldHandleDifferentEventTypes() {
        UUID incidentId = UUID.randomUUID();
        OutboxEventEntity event = OutboxEventEntity.builder()
                .id(UUID.randomUUID()).aggregateId(incidentId)
                .eventType(IncidentEvent.INCIDENT_STATUS_CHANGED.name()).payload("{}")
                .published(false).createdAt(Instant.now()).build();
        when(outboxRepo.findByPublishedFalse()).thenReturn(List.of(event));

        poller.processOutbox();

        verify(eventPublisher).publish(
                eventTypeCaptor.capture(),
                incidentIdCaptor.capture()
        );
        assertThat(eventTypeCaptor.getValue()).isEqualTo(IncidentEvent.INCIDENT_STATUS_CHANGED);
        assertThat(incidentIdCaptor.getValue().getValue()).isEqualTo(incidentId);
    }
}
