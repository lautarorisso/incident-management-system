package com.lautarorisso.notification_service.adapter.in.rest.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / SpringDoc configuration for the Notification Service.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI notificationServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Notification Service API")
                        .description("Async notifications – consume events, persist notifications")
                        .version("1.0.0"))
                .addTagsItem(new Tag().name("Notifications")
                        .description("Notification management endpoints"));
    }
}
