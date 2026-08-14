package com.lautarorisso.incident_service.messaging;

import com.lautarorisso.incident_service.entity.IncidentEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link RabbitMqEventPublisher} — verifies RabbitTemplate is called
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
        Map<String, Object> eventData = new LinkedHashMap<>();
        eventData.put("incidentId", "test-id");
        eventData.put("title", "Test incident");

        publisher.publish(IncidentEvent.INCIDENT_CREATED, eventData);

        verify(rabbitTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq("incident.events"),
                routingKeyCaptor.capture(),
                messageCaptor.capture()
        );
        assertThat(routingKeyCaptor.getValue()).isEqualTo("incident.created");
    }

    @Test
    void shouldUseRoutingKeyBasedOnEventType() {
        Map<String, Object> eventData = new LinkedHashMap<>();
        eventData.put("incidentId", "test-id");

        publisher.publish(IncidentEvent.INCIDENT_ASSIGNED, eventData);

        verify(rabbitTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq("incident.events"),
                routingKeyCaptor.capture(),
                messageCaptor.capture()
        );
        assertThat(routingKeyCaptor.getValue()).isEqualTo("incident.assigned");
    }

    @Test
    void shouldPublishIncidentCreatedWithCorrectRoutingKey() {
        Map<String, Object> eventData = new LinkedHashMap<>();
        eventData.put("incidentId", "test-id");

        publisher.publish(IncidentEvent.INCIDENT_CREATED, eventData);

        verify(rabbitTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq("incident.events"),
                routingKeyCaptor.capture(),
                messageCaptor.capture()
        );
        assertThat(routingKeyCaptor.getValue()).isEqualTo("incident.created");
    }

    @Test
    void shouldIncludeEventTypeAndTimestampInMessagePayload() {
        String incidentId = "test-incident-id";
        Map<String, Object> eventData = new LinkedHashMap<>();
        eventData.put("incidentId", incidentId);
        eventData.put("title", "Test");
        eventData.put("status", "OPEN");
        eventData.put("priority", "HIGH");

        publisher.publish(IncidentEvent.INCIDENT_STATUS_CHANGED, eventData);

        verify(rabbitTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq("incident.events"),
                routingKeyCaptor.capture(),
                messageCaptor.capture()
        );
        assertThat(routingKeyCaptor.getValue()).isEqualTo("incident.status_changed");
        assertThat(messageCaptor.getValue()).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> sent = (Map<String, Object>) messageCaptor.getValue();
        assertThat(sent.get("eventType")).isEqualTo("INCIDENT_STATUS_CHANGED");
        assertThat(sent.get("incidentId")).isEqualTo(incidentId);
        assertThat(sent.get("timestamp")).isNotNull();
    }
}
