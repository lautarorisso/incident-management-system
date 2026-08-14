package com.lautarorisso.user_service.config;

import com.ims.shared.config.OpenApiConfigFactory;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * SpringDoc OpenAPI configuration for the User Service.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI userServiceOpenApi() {
        return OpenApiConfigFactory.createOpenAPI(
                "User Service API",
                "User profiles, teams, and department management",
                "1.0.0",
                null,
                new License()
                        .name("MIT")
                        .url("https://opensource.org/licenses/MIT"),
                List.of(
                        new Tag().name("Users").description("User profile management")
                )
        );
    }
}
