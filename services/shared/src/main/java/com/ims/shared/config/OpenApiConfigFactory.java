package com.ims.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.tags.Tag;

import java.util.List;

/**
 * Static factory for building {@link OpenAPI} instances from the
 * common metadata parameters used across all services.
 */
public final class OpenApiConfigFactory {

    private OpenApiConfigFactory() {
    }

    /**
     * Creates an {@link OpenAPI} with the given service metadata.
     *
     * @param title       the API title
     * @param description the API description
     * @param version     the API version
     * @param contact     the contact (may be {@code null})
     * @param license     the license (may be {@code null})
     * @param tags        the API tags (may be {@code null} or empty)
     * @return a configured {@link OpenAPI}
     */
    public static OpenAPI createOpenAPI(String title, String description, String version,
                                        Contact contact, License license, List<Tag> tags) {
        var info = new io.swagger.v3.oas.models.info.Info()
                .title(title)
                .description(description)
                .version(version);
        if (contact != null) {
            info.contact(contact);
        }
        if (license != null) {
            info.license(license);
        }

        var openAPI = new OpenAPI().info(info);
        if (tags != null && !tags.isEmpty()) {
            openAPI.tags(tags);
        }
        return openAPI;
    }
}
