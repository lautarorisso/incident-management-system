package com.lautarorisso.incident_service.adapter.out.feign;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.lautarorisso.incident_service.adapter.out.feign.dto.TeamDto;
import com.lautarorisso.incident_service.adapter.out.feign.dto.UserDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * WireMock contract test for UserServiceClient.
 * Verifies that the Feign client correctly maps HTTP responses to DTOs
 * and handles error responses via fallback.
 */
@SpringBootTest(properties = {
        "user-service.url=${wiremock.base-url}",
        "spring.cloud.openfeign.circuitbreaker.enabled=true",
        "spring.autoconfigure.exclude[0]=",
        "eureka.client.enabled=false",
        "spring.cloud.config.enabled=false",
        "resilience4j.circuitbreaker.instances.user-service.minimumNumberOfCalls=1",
        "resilience4j.circuitbreaker.instances.user-service.failureRateThreshold=99",
        "resilience4j.circuitbreaker.instances.user-service.slidingWindowSize=1"
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
        wiremock.stubFor(WireMock.get(urlEqualTo("/api/users/" + userId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "id": "%s",
                                    "username": "jdoe",
                                    "firstName": "John",
                                    "lastName": "Doe",
                                    "email": "jdoe@example.com",
                                    "teamId": "%s",
                                    "active": true
                                }
                                """.formatted(userId, UUID.randomUUID()))));

        UserDto result = userServiceClient.findUserById(userId);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.username()).isEqualTo("jdoe");
        assertThat(result.firstName()).isEqualTo("John");
        assertThat(result.email()).isEqualTo("jdoe@example.com");
        assertThat(result.active()).isTrue();
    }

    @Test
    void shouldReturnNullWhenUserNotFound() {
        UUID userId = UUID.randomUUID();
        wiremock.stubFor(WireMock.get(urlEqualTo("/api/users/" + userId))
                .willReturn(aResponse().withStatus(404)));

        UserDto result = userServiceClient.findUserById(userId);

        assertThat(result).isNull();
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
                                    "active": true
                                }
                                """.formatted(teamId))));

        TeamDto result = userServiceClient.findTeamById(teamId);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(teamId);
        assertThat(result.name()).isEqualTo("Backend Team");
        assertThat(result.active()).isTrue();
    }

    @Test
    void shouldReturnNullWhenTeamNotFound() {
        UUID teamId = UUID.randomUUID();
        wiremock.stubFor(WireMock.get(urlEqualTo("/api/teams/" + teamId))
                .willReturn(aResponse().withStatus(404)));

        TeamDto result = userServiceClient.findTeamById(teamId);

        assertThat(result).isNull();
    }

    @Test
    void shouldHandleServerErrorWithFallback() {
        UUID userId = UUID.randomUUID();
        wiremock.stubFor(WireMock.get(urlEqualTo("/api/users/" + userId))
                .willReturn(aResponse().withStatus(500)));

        UserDto result = userServiceClient.findUserById(userId);

        assertThat(result).isNull();
    }
}
