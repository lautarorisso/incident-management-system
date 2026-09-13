package com.lautarorisso.incident_service.messaging;

import com.ims.shared.config.SharedRabbitMqConfig;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for incident domain events.
 * <p>
 * The service only DECLARES the {@code incident.events} exchange; it does not own
 * any queue. Consumers bind their own queues: notification-service binds
 * {@code notification.events.queue} with the {@code #} routing key, and tests
 * bind transient queues. This keeps the broker free of the unbounded,
 * no-consumer {@code incident.events.queue}.
 * Infrastructure beans (Jackson2JsonMessageConverter, RabbitTemplate)
 * are inherited from {@link SharedRabbitMqConfig}.
 */
@Configuration
public class RabbitMqConfig extends SharedRabbitMqConfig {

    static final String EXCHANGE_NAME = "incident.events";

    @Bean
    TopicExchange incidentExchange() {
        return new TopicExchange(EXCHANGE_NAME, true, false);
    }
}
