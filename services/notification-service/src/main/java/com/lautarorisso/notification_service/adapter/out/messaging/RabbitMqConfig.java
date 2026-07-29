package com.lautarorisso.notification_service.adapter.out.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for consuming incident domain events.
 * <p>
 * Binds to the same exchange used by incident-service and declares
 * a dedicated queue for the notification service.
 */
@Configuration
public class RabbitMqConfig {

    static final String EXCHANGE_NAME = "incident.events";
    static final String QUEUE_NAME = "notification.events.queue";

    @Bean
    TopicExchange incidentExchange() {
        return new TopicExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    Queue notificationQueue() {
        return new Queue(QUEUE_NAME, true);
    }

    @Bean
    Binding notificationBinding(Queue notificationQueue, TopicExchange incidentExchange) {
        return BindingBuilder.bind(notificationQueue)
                .to(incidentExchange)
                .with("#");
    }

    @Bean
    Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                  Jackson2JsonMessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
