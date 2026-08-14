package com.lautarorisso.incident_service.messaging;

import com.ims.shared.config.SharedRabbitMqConfig;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for incident domain events.
 * <p>
 * Declares the exchange, queue, and binding used by the outbox poller
 * to publish incident events to the message broker.
 * Infrastructure beans (Jackson2JsonMessageConverter, RabbitTemplate)
 * are inherited from {@link SharedRabbitMqConfig}.
 */
@Configuration
public class RabbitMqConfig extends SharedRabbitMqConfig {

    static final String EXCHANGE_NAME = "incident.events";
    static final String QUEUE_NAME = "incident.events.queue";

    @Bean
    TopicExchange incidentExchange() {
        return new TopicExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    Queue incidentQueue() {
        return new Queue(QUEUE_NAME, true);
    }

    @Bean
    Binding incidentBinding(Queue incidentQueue, TopicExchange incidentExchange) {
        return BindingBuilder.bind(incidentQueue)
                .to(incidentExchange)
                .with("#");
    }
}
