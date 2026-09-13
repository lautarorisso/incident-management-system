package com.lautarorisso.incident_service.messaging;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests that {@link RabbitMqConfig} creates the required beans for incident event messaging.
 * <p>
 * The service only declares the exchange; queues are owned by consumers, so no
 * queue or binding bean is expected.
 */
@SpringJUnitConfig(RabbitMqConfigTest.TestConfig.class)
class RabbitMqConfigTest {

    @Autowired(required = false)
    private TopicExchange incidentExchange;

    @Autowired(required = false)
    private RabbitTemplate rabbitTemplate;

    @Test
    void shouldCreateTopicExchange() {
        assertThat(incidentExchange).isNotNull();
        assertThat(incidentExchange.getName()).isEqualTo("incident.events");
        assertThat(incidentExchange.isDurable()).isTrue();
    }

    @Test
    void shouldConfigureJacksonMessageConverter() {
        assertThat(rabbitTemplate).isNotNull();
        assertThat(rabbitTemplate.getMessageConverter())
                .isInstanceOf(Jackson2JsonMessageConverter.class);
    }

    @TestConfiguration
    @Import(RabbitMqConfig.class)
    static class TestConfig {

        @Bean
        ConnectionFactory connectionFactory() {
            return mock(ConnectionFactory.class);
        }
    }
}
