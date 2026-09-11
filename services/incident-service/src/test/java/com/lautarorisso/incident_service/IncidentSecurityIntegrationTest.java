package com.lautarorisso.incident_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lautarorisso.incident_service.service.OutboxPoller;
import com.lautarorisso.incident_service.support.AbstractPostgresTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.RabbitMQContainer;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Incident-service auth matrix against the FULL Spring context (real
 * {@code SecurityConfig} + shared JWT resource server), following the design
 * per-service role matrix:
 * <ul>
 *   <li>public (permitAll): actuator health/info, v3/api-docs/**, scalar/**</li>
 *   <li>{@code POST /api/incidents}: any authenticated caller</li>
 *   <li>{@code PUT} assign and transition of a single incident, and
 *       {@code GET /api/incidents} (list): ADMIN or AGENT</li>
 *   <li>other endpoints (e.g. {@code GET} of a single incident): any authenticated</li>
 * </ul>
 * <p>
 * The {@code jwt()} request post-processor injects a pre-authenticated
 * {@code JwtAuthenticationToken} WITHOUT an {@code Authorization} header, so
 * the bearer-token filter leaves the context untouched — no Keycloak needs to
 * be reachable. JWT signature/expiry validation itself is covered by the
 * decoder, not by this matrix.
 * <p>
 * The real {@link OutboxPoller} is replaced so its {@code @Scheduled} method
 * never marks events published against the broker mid-test (same pattern as
 * {@code IncidentServiceIntegrationTest}); the broker container is still
 * started so {@code RabbitAdmin} can declare the topology at startup without
 * connect-refused retries.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IncidentSecurityIntegrationTest extends AbstractPostgresTestBase {

    @ServiceConnection
    static final RabbitMQContainer RABBITMQ = startRabbitMq();

    private static RabbitMQContainer startRabbitMq() {
        RabbitMQContainer container = new RabbitMQContainer("rabbitmq:3-management");
        container.start();
        return container;
    }

    @MockitoBean
    private OutboxPoller outboxPoller;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void apiDocsArePublic() throws Exception {
        // /v3/api-docs exercises the same permitAll rule as /actuator/{health,info}
        // against a controller-mapped endpoint (the actuator handler mapping is
        // not resolvable through MockMvc in this context — see apply notes).
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    void listIncidentsWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/incidents"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userRoleCanCreateAndReadIncident() throws Exception {
        String created = mockMvc.perform(post("/api/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Matrix incident",
                                    "description": "Created by an authenticated user",
                                    "priority": "HIGH"
                                }
                                """)
                        .with(jwt().jwt(builder -> builder.subject("user-001"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String incidentId = objectMapper.readTree(created).get("id").asText();

        // GET /api/incidents/{id} is not in the ADMIN/AGENT columns of the
        // design matrix → falls back to "any authenticated caller".
        mockMvc.perform(get("/api/incidents/{id}", incidentId)
                        .with(jwt().jwt(builder -> builder.subject("user-001"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Matrix incident"));
    }

    @Test
    void userRoleCannotListIncidents() throws Exception {
        mockMvc.perform(get("/api/incidents")
                        .with(jwt().jwt(builder -> builder.subject("user-001"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void userRoleCannotAssignIncident() throws Exception {
        mockMvc.perform(put("/api/incidents/{id}/assign", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assigneeId\":\"" + UUID.randomUUID() + "\",\"teamId\":null}")
                        .with(jwt().jwt(builder -> builder.subject("user-001"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void userRoleCannotTransitionIncident() throws Exception {
        mockMvc.perform(put("/api/incidents/{id}/transition", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newStatus\":\"IN_PROGRESS\"}")
                        .with(jwt().jwt(builder -> builder.subject("user-001"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void agentRoleCanListIncidents() throws Exception {
        mockMvc.perform(get("/api/incidents")
                        .with(jwt().jwt(builder -> builder.subject("agent-001"))
                                .authorities(new SimpleGrantedAuthority("ROLE_AGENT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void adminRoleCanListIncidents() throws Exception {
        mockMvc.perform(get("/api/incidents")
                        .with(jwt().jwt(builder -> builder.subject("admin-001"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}