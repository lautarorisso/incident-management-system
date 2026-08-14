package com.ims.shared.config;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Abstract RabbitMQ configuration shared across services.
 * <p>
 * Provides the two identical infrastructure beans that every service
 * needs: a {@link Jackson2JsonMessageConverter} and a configured
 * {@link RabbitTemplate}. Concrete service configurations extend this
 * class and add their own exchange, queue, and binding declarations.
 */
@Configuration
public abstract class SharedRabbitMqConfig {

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
