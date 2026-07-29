package com.lautarorisso.incident_service.adapter.in.rest.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * SpringDoc OpenAPI configuration for the Incident Service.
 * <p>
 * Provides API metadata and groups endpoints by tags for the Swagger UI.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI incidentServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Incident Service API")
                        .description("REST API for managing incidents — CRUD, state machine transitions, and assignment")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Incident Management Team")
                                .email("ops@example.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://example.com/license")))
                .tags(List.of(
                        new Tag().name("Incidents").description("Incident management endpoints — create, read, transition, assign"),
                        new Tag().name("Health").description("Health check and monitoring endpoints")));
    }
}
