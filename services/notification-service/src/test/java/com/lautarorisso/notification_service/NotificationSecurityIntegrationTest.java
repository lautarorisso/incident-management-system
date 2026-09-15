package com.lautarorisso.notification_service;

import com.lautarorisso.notification_service.entity.Notification;
import com.lautarorisso.notification_service.enums.NotificationReadStatus;
import com.lautarorisso.notification_service.enums.NotificationType;
import com.lautarorisso.notification_service.repository.NotificationRepository;
import com.lautarorisso.notification_service.support.AbstractMongoTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.RabbitMQContainer;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Notification-service auth matrix against the FULL Spring context (real
 * {@code SecurityConfig} + shared JWT resource server): public endpoints are
 * reachable without a token, everything under {@code /api/notifications}
 * requires a valid JWT, and the list, by-id and read endpoints enforce the
 * owner-check ({@code sub == userId} or {@code ROLE_ADMIN}, 403 otherwise).
 * <p>
 * The {@code jwt()} request post-processor injects a pre-authenticated
 * {@code JwtAuthenticationToken} WITHOUT an {@code Authorization} header, so
 * the bearer-token filter leaves the context untouched — no Keycloak needs to
 * be reachable. JWT signature/expiry validation itself is covered by the
 * decoder, not by this matrix.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationSecurityIntegrationTest extends AbstractMongoTestBase {

    /**
     * Real broker so the auto-configured {@code RabbitAdmin} can declare the
     * exchange/queue/binding at startup instead of emitting connect-refused
     * retries (same rationale as {@code NotificationServiceIntegrationTest}).
     */
    @ServiceConnection
    static final RabbitMQContainer RABBITMQ = startRabbitMq();

    private static RabbitMQContainer startRabbitMq() {
        RabbitMQContainer container = new RabbitMQContainer("rabbitmq:3-management");
        container.start();
        return container;
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationRepository notificationRepository;

    private Notification seedNotification(UUID ownerId) {
        return notificationRepository.save(Notification.builder()
                .id(UUID.randomUUID())
                .type(NotificationType.INCIDENT_ASSIGNED)
                .userId(ownerId)
                .incidentId(UUID.randomUUID())
                .title("Security test notification")
                .message("Test message")
                .readStatus(NotificationReadStatus.UNREAD)
                .createdAt(Instant.now())
                .build());
    }

    @Test
    void apiDocsArePublic() throws Exception {
        // /v3/api-docs exercises the same permitAll rule as /actuator/{health,info}
        // against a controller-mapped endpoint (the actuator handler mapping is
        // not resolvable through MockMvc in this context — see apply notes).
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    void notificationsWithoutTokenReturn401() throws Exception {
        mockMvc.perform(get("/api/notifications")
                        .param("userId", UUID.randomUUID().toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ownerListsOwnNotifications() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(get("/api/notifications")
                        .param("userId", userId.toString())
                        .with(jwt().jwt(builder -> builder.subject(userId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void otherUserListingNotificationsReturns403() throws Exception {
        mockMvc.perform(get("/api/notifications")
                        .param("userId", UUID.randomUUID().toString())
                        .with(jwt().jwt(builder -> builder.subject(UUID.randomUUID().toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminListsAnyUserNotifications() throws Exception {
        mockMvc.perform(get("/api/notifications")
                        .param("userId", UUID.randomUUID().toString())
                        .with(jwt().jwt(builder -> builder.subject("admin-001"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void nonOwnerGetNotificationByIdReturns403() throws Exception {
        Notification notification = seedNotification(UUID.randomUUID());

        mockMvc.perform(get("/api/notifications/{id}", notification.getId())
                        .with(jwt().jwt(builder -> builder.subject(UUID.randomUUID().toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerGetNotificationByIdReturns200() throws Exception {
        UUID ownerId = UUID.randomUUID();
        Notification notification = seedNotification(ownerId);

        mockMvc.perform(get("/api/notifications/{id}", notification.getId())
                        .with(jwt().jwt(builder -> builder.subject(ownerId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(notification.getId().toString()))
                .andExpect(jsonPath("$.title").value("Security test notification"));
    }

    @Test
    void adminGetNotificationByIdReturns200() throws Exception {
        Notification notification = seedNotification(UUID.randomUUID());

        mockMvc.perform(get("/api/notifications/{id}", notification.getId())
                        .with(jwt().jwt(builder -> builder.subject("admin-001"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(notification.getId().toString()));
    }

    @Test
    void nonOwnerMarkAsReadReturns403AndNotificationStaysUnread() throws Exception {
        Notification notification = seedNotification(UUID.randomUUID());

        mockMvc.perform(patch("/api/notifications/{id}/read", notification.getId())
                        .with(jwt().jwt(builder -> builder.subject(UUID.randomUUID().toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());

        Notification reloaded = notificationRepository.findById(notification.getId()).orElseThrow();
        assertEquals(NotificationReadStatus.UNREAD, reloaded.getReadStatus());
    }

    @Test
    void ownerMarkAsReadReturns200WithStatusRead() throws Exception {
        UUID ownerId = UUID.randomUUID();
        Notification notification = seedNotification(ownerId);

        mockMvc.perform(patch("/api/notifications/{id}/read", notification.getId())
                        .with(jwt().jwt(builder -> builder.subject(ownerId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readStatus").value("READ"));

        Notification reloaded = notificationRepository.findById(notification.getId()).orElseThrow();
        assertEquals(NotificationReadStatus.READ, reloaded.getReadStatus());
    }
}