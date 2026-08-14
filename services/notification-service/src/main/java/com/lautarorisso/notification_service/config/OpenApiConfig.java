package com.lautarorisso.notification_service.config;

import com.ims.shared.config.OpenApiConfigFactory;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI / SpringDoc configuration for the Notification Service.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI notificationServiceOpenApi() {
        return OpenApiConfigFactory.createOpenAPI(
                "Notification Service API",
                "Async notifications – consume events, persist notifications",
                "1.0.0",
                null,
                null,
                List.of(
                        new Tag().name("Notifications")
                                .description("Notification management endpoints")
                )
        );
    }
}
