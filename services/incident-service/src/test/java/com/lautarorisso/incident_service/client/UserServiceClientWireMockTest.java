package com.lautarorisso.incident_service.client;

import com.ims.shared.dto.TeamDto;
import com.ims.shared.dto.UserDto;
import com.lautarorisso.incident_service.entity.Incident;
import com.lautarorisso.incident_service.entity.IncidentPriority;
import com.lautarorisso.incident_service.entity.IncidentStatus;
import com.lautarorisso.incident_service.repository.IncidentRepository;
import com.lautarorisso.incident_service.support.AbstractPostgresTestBase;
import feign.FeignException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.github.tomakehurst.wiremock.client.WireMock;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

/**
 * WireMock contract test for {@link UserServiceClient}.
 * <p>
 * Verifies that the Feign client correctly maps HTTP responses to DTOs,
 * surfaces error responses as {@link FeignException}, and — through the shared
 * {@code FeignTokenRelayConfig} attached to the client — forwards the incoming
 * {@code Authorization} header to user-service.
 * <p>
 * The {@link JwtDecoder} is mocked so the full HTTP path can run an end-to-end
 * relay + propagation scenario (PUT assign with a real {@code Authorization}
 * header and an AGENT token) without needing Keycloak: the mocked decoder lets
 * incident-service validate the header locally, the relay interceptor forwards
 * it to WireMock, and the downstream 401 must surface as a 401 to the caller —
 * never as a 503.
 */
@SpringBootTest(properties = {
        "user-service.url=${wiremock.base-url}",
        "spring.autoconfigure.exclude[0]=",
        "eureka.client.enabled=false",
        "spring.cloud.config.enabled=false"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
class UserServiceClientWireMockTest extends AbstractPostgresTestBase {

    @RegisterExtension
    static WireMockExtension wiremock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @Autowired
    private UserServiceClient userServiceClient;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IncidentRepository incidentRepository;

    @MockitoBean
    private JwtDecoder jwtDecoder;

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
                        .withBody(userResponseBody(userId, keycloakId))));

        UserDto result = userServiceClient.findUserById(userId);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.username()).isEqualTo("jdoe");
        assertThat(result.displayName()).isEqualTo("John Doe");
        assertThat(result.email()).isEqualTo("jdoe@example.com");
        assertThat(result.active()).isTrue();

        // No servlet request context on the test thread → the relay interceptor
        // must be a no-op: no Authorization header may reach user-service
        // (spec: "No Authorization header results in no header forwarded").
        wiremock.verify(getRequestedFor(urlEqualTo("/api/users/" + userId))
                .withoutHeader("Authorization"));
    }

    @Test
    void shouldForwardAuthorizationHeaderWhenRequestContextPresent() {
        UUID userId = UUID.randomUUID();
        UUID keycloakId = UUID.randomUUID();
        wiremock.stubFor(WireMock.get(urlEqualTo("/api/users/" + userId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(userResponseBody(userId, keycloakId))));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer relay-token");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        try {
            userServiceClient.findUserById(userId);
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }

        wiremock.verify(getRequestedFor(urlEqualTo("/api/users/" + userId))
                .withHeader("Authorization", equalTo("Bearer relay-token")));
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
    void shouldThrowUnauthorizedWhenDownstreamReturns401() {
        UUID userId = UUID.randomUUID();
        wiremock.stubFor(WireMock.get(urlEqualTo("/api/users/" + userId))
                .willReturn(aResponse().withStatus(401)));

        assertThatThrownBy(() -> userServiceClient.findUserById(userId))
                .isInstanceOf(FeignException.Unauthorized.class);
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

    /**
     * Full-path proof of the token relay + 401 propagation contract
     * (spec feign-token-relay: expired/malformed token rejected downstream
     * → incident-service propagates the 401, never a 503):
     * <ol>
     *   <li>AGENT JWT is locally validated (mocked decoder, no Keycloak needed)</li>
     *   <li>{@code PUT /api/incidents/{id}/assign} reaches the controller and the
     *       service calls user-service via Feign</li>
     *   <li>the relay interceptor forwards the original {@code Authorization} —
     *       WireMock asserts the header on the downstream request</li>
     *   <li>user-service answers 401 → the handler propagates 401 to the caller</li>
     * </ol>
     */
    @Test
    void shouldPropagateDownstream401As401AndRelayAuthorization() throws Exception {
        UUID assigneeId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();
        incidentRepository.save(Incident.builder()
                .id(incidentId)
                .title("Relay incident")
                .description("Downstream auth propagation")
                .status(IncidentStatus.OPEN)
                .priority(IncidentPriority.MEDIUM)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());

        wiremock.stubFor(WireMock.get(urlEqualTo("/api/users/" + assigneeId))
                .willReturn(aResponse().withStatus(401)));

        when(jwtDecoder.decode(anyString()))
                .thenReturn(Jwt.withTokenValue("test-token")
                        .header("alg", "RS256")
                        .subject("agent-001")
                        .claim("realm_access", Map.of("roles", List.of("ims-agent")))
                        .build());

        mockMvc.perform(put("/api/incidents/{id}/assign", incidentId)
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assigneeId\":\"" + assigneeId + "\",\"teamId\":null}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        wiremock.verify(getRequestedFor(urlEqualTo("/api/users/" + assigneeId))
                .withHeader("Authorization", equalTo("Bearer test-token")));
    }

    private String userResponseBody(UUID userId, UUID keycloakId) {
        return """
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
                """.formatted(userId, keycloakId);
    }
}