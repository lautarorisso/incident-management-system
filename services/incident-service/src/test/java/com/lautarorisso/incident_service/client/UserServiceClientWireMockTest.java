package com.lautarorisso.incident_service.client;

import com.ims.shared.dto.TeamDto;
import com.ims.shared.dto.UserDto;
import feign.FeignException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Instant;
import java.util.UUID;

import com.github.tomakehurst.wiremock.client.WireMock;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

/**
 * WireMock contract test for {@link UserServiceClient}.
 * Verifies that the Feign client correctly maps HTTP responses to DTOs
 * and surfaces error responses as {@link FeignException}.
 */
@SpringBootTest(properties = {
        "user-service.url=${wiremock.base-url}",
        "spring.autoconfigure.exclude[0]=",
        "eureka.client.enabled=false",
        "spring.cloud.config.enabled=false"
})
@ActiveProfiles("test")
class UserServiceClientWireMockTest {

    @RegisterExtension
    static WireMockExtension wiremock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @Autowired
    private UserServiceClient userServiceClient;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("wiremock.base-url", wiremock::baseUrl);
    }

    @Test
    void shouldFindUserById() {
        UUID userId = UUID.randomUUID();
        UUID keycloakId = UUID.randomUUID();
        wiremock.stubFor(WireMock.get(urlEqualTo("/api/users/" + userId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "id": "%s",
                                    "keycloakId": "%s",
                                    "username": "jdoe",
                                    "displayName": "John Doe",
                                    "email": "jdoe@example.com",
                                    "active": true,
                                    "teamIds": [],
                                    "createdAt": "2026-01-01T00:00:00Z",
                                    "updatedAt": "2026-01-01T00:00:00Z"
                                }
                                """.formatted(userId, keycloakId))));

        UserDto result = userServiceClient.findUserById(userId);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.username()).isEqualTo("jdoe");
        assertThat(result.displayName()).isEqualTo("John Doe");
        assertThat(result.email()).isEqualTo("jdoe@example.com");
        assertThat(result.active()).isTrue();
    }

    @Test
    void shouldThrowFeignNotFoundWhenUserNotFound() {
        UUID userId = UUID.randomUUID();
        wiremock.stubFor(WireMock.get(urlEqualTo("/api/users/" + userId))
                .willReturn(aResponse().withStatus(404)));

        assertThatThrownBy(() -> userServiceClient.findUserById(userId))
                .isInstanceOf(FeignException.NotFound.class);
    }

    @Test
    void shouldFindTeamById() {
        UUID teamId = UUID.randomUUID();
        wiremock.stubFor(WireMock.get(urlEqualTo("/api/teams/" + teamId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "id": "%s",
                                    "name": "Backend Team",
                                    "description": "Backend engineering team",
                                    "createdAt": "2026-01-01T00:00:00Z",
                                    "updatedAt": "2026-01-01T00:00:00Z"
                                }
                                """.formatted(teamId))));

        TeamDto result = userServiceClient.findTeamById(teamId);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(teamId);
        assertThat(result.name()).isEqualTo("Backend Team");
    }

    @Test
    void shouldThrowFeignNotFoundWhenTeamNotFound() {
        UUID teamId = UUID.randomUUID();
        wiremock.stubFor(WireMock.get(urlEqualTo("/api/teams/" + teamId))
                .willReturn(aResponse().withStatus(404)));

        assertThatThrownBy(() -> userServiceClient.findTeamById(teamId))
                .isInstanceOf(FeignException.NotFound.class);
    }

    @Test
    void shouldThrowFeignExceptionOnServerError() {
        UUID userId = UUID.randomUUID();
        wiremock.stubFor(WireMock.get(urlEqualTo("/api/users/" + userId))
                .willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> userServiceClient.findUserById(userId))
                .isInstanceOf(FeignException.class);
    }
}
