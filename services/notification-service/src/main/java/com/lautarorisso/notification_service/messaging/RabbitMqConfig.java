package com.lautarorisso.notification_service.messaging;

import com.ims.shared.config.SharedRabbitMqConfig;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for consuming incident domain events.
 * <p>
 * Binds to the same exchange used by incident-service and declares
 * a dedicated queue for the notification service.
 * Infrastructure beans (Jackson2JsonMessageConverter, RabbitTemplate)
 * are inherited from {@link SharedRabbitMqConfig}.
 * <p>
 * Dead-letter topology: {@code notification.events.queue} is declared with an
 * {@code x-dead-letter-exchange} pointing at a dedicated DLX, so messages whose
 * listener retries are exhausted (or that are rejected with requeue disabled)
 * land on {@code notification.events.dlq} instead of being dropped or requeued
 * forever. The DLQ is inspectable and can be replayed manually.
 */
@Configuration
public class RabbitMqConfig extends SharedRabbitMqConfig {

    static final String EXCHANGE_NAME = "incident.events";
    static final String QUEUE_NAME = "notification.events.queue";
    static final String DLX_NAME = "notification.events.dlx";
    static final String DLQ_NAME = "notification.events.dlq";

    @Bean
    TopicExchange incidentExchange() {
        return new TopicExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    TopicExchange notificationDeadLetterExchange() {
        return new TopicExchange(DLX_NAME, true, false);
    }

    @Bean
    Queue notificationDeadLetterQueue() {
        return new Queue(DLQ_NAME, true);
    }

    @Bean
    Queue notificationQueue() {
        return QueueBuilder.durable(QUEUE_NAME)
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                .withArgument("x-dead-letter-routing-key", DLQ_NAME)
                .build();
    }

    @Bean
    Binding notificationBinding(Queue notificationQueue, TopicExchange incidentExchange) {
        return BindingBuilder.bind(notificationQueue)
                .to(incidentExchange)
                .with("#");
    }

    @Bean
    Binding notificationDeadLetterBinding(Queue notificationDeadLetterQueue,
                                          TopicExchange notificationDeadLetterExchange) {
        return BindingBuilder.bind(notificationDeadLetterQueue)
                .to(notificationDeadLetterExchange)
                .with("#");
    }
}
