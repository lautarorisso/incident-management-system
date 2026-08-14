package com.lautarorisso.incident_service.config;

import com.ims.shared.config.OpenApiConfigFactory;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * SpringDoc OpenAPI configuration for the Incident Service.
 * <p>
 * Provides API metadata and groups endpoints by tags for the Scalar.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI incidentServiceOpenApi() {
        return OpenApiConfigFactory.createOpenAPI(
                "Incident Service API",
                "REST API for managing incidents — CRUD, state machine transitions, and assignment",
                "1.0.0",
                new Contact()
                        .name("Incident Management Team")
                        .email("ops@example.com"),
                new License()
                        .name("Proprietary")
                        .url("https://example.com/license"),
                List.of(
                        new Tag().name("Incidents").description("Incident management endpoints — create, read, transition, assign"),
                        new Tag().name("Health").description("Health check and monitoring endpoints")
                )
        );
    }
}
