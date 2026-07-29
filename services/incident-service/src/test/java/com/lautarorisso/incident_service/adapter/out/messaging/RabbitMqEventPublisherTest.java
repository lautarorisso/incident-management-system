package com.lautarorisso.incident_service.adapter.out.messaging;

import com.lautarorisso.incident_service.domain.model.IncidentEvent;
import com.lautarorisso.incident_service.domain.model.IncidentId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * Tests for RabbitMqEventPublisher — verifies RabbitTemplate is called
 * with the correct exchange, routing key, and payload.
 */
@ExtendWith(MockitoExtension.class)
class RabbitMqEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private RabbitMqEventPublisher publisher;

    @Captor
    private ArgumentCaptor<String> routingKeyCaptor;

    @Captor
    private ArgumentCaptor<Object> messageCaptor;

    @BeforeEach
    void setUp() {
        publisher = new RabbitMqEventPublisher(rabbitTemplate);
    }

    @Test
    void shouldPublishEventToRabbitExchange() {
        IncidentId incidentId = new IncidentId(UUID.randomUUID());

        publisher.publish(IncidentEvent.INCIDENT_CREATED, incidentId);

        verify(rabbitTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq("incident.events"),
                routingKeyCaptor.capture(),
                messageCaptor.capture()
        );
    }

    @Test
    void shouldUseRoutingKeyBasedOnEventType() {
        publisher.publish(IncidentEvent.INCIDENT_ASSIGNED, new IncidentId(UUID.randomUUID()));

        verify(rabbitTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq("incident.events"),
                routingKeyCaptor.capture(),
                messageCaptor.capture()
        );
        assertThat(routingKeyCaptor.getValue()).isEqualTo("incident.assigned");
    }

    @Test
    void shouldPublishIncidentCreatedWithCorrectRoutingKey() {
        publisher.publish(IncidentEvent.INCIDENT_CREATED, new IncidentId(UUID.randomUUID()));

        verify(rabbitTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq("incident.events"),
                routingKeyCaptor.capture(),
                messageCaptor.capture()
        );
        assertThat(routingKeyCaptor.getValue()).isEqualTo("incident.created");
    }

    @Test
    void shouldIncludeIncidentIdInMessagePayload() {
        UUID id = UUID.randomUUID();
        IncidentId incidentId = new IncidentId(id);

        publisher.publish(IncidentEvent.INCIDENT_STATUS_CHANGED, incidentId);

        verify(rabbitTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq("incident.events"),
                routingKeyCaptor.capture(),
                messageCaptor.capture()
        );
        assertThat(messageCaptor.getValue()).isInstanceOf(RabbitMqEventPublisher.EventMessage.class);
        RabbitMqEventPublisher.EventMessage msg = (RabbitMqEventPublisher.EventMessage) messageCaptor.getValue();
        assertThat(msg.incidentId()).isEqualTo(id);
        assertThat(msg.eventType()).isEqualTo("INCIDENT_STATUS_CHANGED");
    }
}
