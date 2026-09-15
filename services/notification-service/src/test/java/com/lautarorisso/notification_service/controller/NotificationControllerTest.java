package com.lautarorisso.notification_service.controller;

import com.lautarorisso.notification_service.entity.Notification;
import com.lautarorisso.notification_service.enums.NotificationReadStatus;
import com.lautarorisso.notification_service.enums.NotificationType;
import com.lautarorisso.notification_service.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    private Notification sampleNotification(UUID userId) {
        return Notification.builder()
                .id(UUID.randomUUID())
                .type(NotificationType.INCIDENT_ASSIGNED)
                .userId(userId)
                .incidentId(UUID.randomUUID())
                .title("Test notification")
                .message("Test message")
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void getNotificationsByUserIdReturnsList() throws Exception {
        UUID userId = UUID.randomUUID();
        Notification notification = sampleNotification(userId);

        when(notificationService.getNotifications(any(), any(), eq(userId), isNull()))
                .thenReturn(List.of(notification));

        mockMvc.perform(get("/api/notifications")
                        .param("userId", userId.toString())
                        .accept(MediaType.APPLICATION_JSON)
                        .with(jwt().jwt(builder -> builder.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Test notification"))
                .andExpect(jsonPath("$[0].deliveryStatus").value("PENDING"))
                .andExpect(jsonPath("$[0].readStatus").value("UNREAD"));
    }

    @Test
    void getNotificationsByUserIdAndReadStatusFilters() throws Exception {
        UUID userId = UUID.randomUUID();
        Notification notification = sampleNotification(userId);

        when(notificationService.getNotifications(
                any(), any(), eq(userId), eq(NotificationReadStatus.UNREAD)))
                .thenReturn(List.of(notification));

        mockMvc.perform(get("/api/notifications")
                        .param("userId", userId.toString())
                        .param("status", "UNREAD")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(jwt().jwt(builder -> builder.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Test notification"))
                .andExpect(jsonPath("$[0].readStatus").value("UNREAD"));
    }

    @Test
    void getNotificationsReadStatusFilterIsCaseInsensitive() throws Exception {
        UUID userId = UUID.randomUUID();
        Notification notification = sampleNotification(userId);

        when(notificationService.getNotifications(
                any(), any(), eq(userId), eq(NotificationReadStatus.READ)))
                .thenReturn(List.of(notification));

        mockMvc.perform(get("/api/notifications")
                        .param("userId", userId.toString())
                        .param("status", "read")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(jwt().jwt(builder -> builder.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].readStatus").value("UNREAD"));
    }

    @Test
    void getNotificationsWithInvalidStatusReturns400() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(get("/api/notifications")
                        .param("userId", userId.toString())
                        .param("status", "BOGUS")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(jwt().jwt(builder -> builder.subject(userId.toString()))))
                .andExpect(status().isBadRequest());

        verify(notificationService, never()).getNotifications(any(), any(), any(), any());
    }

    @Test
    void getNotificationByIdReturnsNotification() throws Exception {
        UUID notificationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Notification notification = Notification.builder()
                .id(notificationId)
                .type(NotificationType.INCIDENT_ASSIGNED)
                .userId(userId)
                .incidentId(UUID.randomUUID())
                .title("Test notification")
                .message("Test message")
                .createdAt(Instant.now())
                .build();

        when(notificationService.getById(any(), any(), eq(notificationId)))
                .thenReturn(Optional.of(notification));

        mockMvc.perform(get("/api/notifications/{id}", notificationId)
                        .accept(MediaType.APPLICATION_JSON)
                        .with(jwt().jwt(builder -> builder.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(notificationId.toString()))
                .andExpect(jsonPath("$.title").value("Test notification"))
                .andExpect(jsonPath("$.deliveryStatus").value("PENDING"))
                .andExpect(jsonPath("$.readStatus").value("UNREAD"));
    }

    @Test
    void getNotificationByIdReturns403WhenNotOwner() throws Exception {
        UUID notificationId = UUID.randomUUID();

        when(notificationService.getById(any(), any(), eq(notificationId)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        mockMvc.perform(get("/api/notifications/{id}", notificationId)
                        .accept(MediaType.APPLICATION_JSON)
                        .with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getNotificationByIdReturns404WhenNotFound() throws Exception {
        when(notificationService.getById(any(), any(), any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/notifications/{id}", UUID.randomUUID())
                        .accept(MediaType.APPLICATION_JSON)
                        .with(jwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    void markAsReadReturnsUpdatedNotification() throws Exception {
        UUID notificationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Notification notification = sampleNotification(userId);
        Notification updated = notification.withReadStatus(NotificationReadStatus.READ);

        when(notificationService.markAsRead(any(), any(), eq(notificationId)))
                .thenReturn(Optional.of(updated));

        mockMvc.perform(patch("/api/notifications/{id}/read", notificationId)
                        .accept(MediaType.APPLICATION_JSON)
                        .with(jwt().jwt(builder -> builder.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readStatus").value("READ"))
                // Delivery status is preserved by markAsRead.
                .andExpect(jsonPath("$.deliveryStatus").value("PENDING"));
    }

    @Test
    void markAsReadReturns403WhenNotOwner() throws Exception {
        UUID notificationId = UUID.randomUUID();

        when(notificationService.markAsRead(any(), any(), eq(notificationId)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        mockMvc.perform(patch("/api/notifications/{id}/read", notificationId)
                        .accept(MediaType.APPLICATION_JSON)
                        .with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void markAsReadReturns404WhenNotificationNotFound() throws Exception {
        when(notificationService.markAsRead(any(), any(), any())).thenReturn(Optional.empty());

        mockMvc.perform(patch("/api/notifications/{id}/read", UUID.randomUUID())
                        .accept(MediaType.APPLICATION_JSON)
                        .with(jwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnEmptyListWhenNoNotifications() throws Exception {
        UUID userId = UUID.randomUUID();
        when(notificationService.getNotifications(any(), any(), eq(userId), isNull()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/notifications")
                        .param("userId", userId.toString())
                        .accept(MediaType.APPLICATION_JSON)
                        .with(jwt().jwt(builder -> builder.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void listEndpointShouldNotExposeMessageField() throws Exception {
        UUID userId = UUID.randomUUID();
        Notification notification = Notification.builder()
                .id(UUID.randomUUID())
                .type(NotificationType.INCIDENT_ASSIGNED)
                .userId(userId)
                .incidentId(UUID.randomUUID())
                .title("Test")
                .message("Sensitive message content")
                .createdAt(Instant.now())
                .build();

        when(notificationService.getNotifications(any(), any(), eq(userId), isNull()))
                .thenReturn(List.of(notification));

        mockMvc.perform(get("/api/notifications")
                        .param("userId", userId.toString())
                        .accept(MediaType.APPLICATION_JSON)
                        .with(jwt().jwt(builder -> builder.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].message").doesNotExist())
                .andExpect(jsonPath("$[0].userId").doesNotExist())
                .andExpect(jsonPath("$[0].incidentId").doesNotExist())
                .andExpect(jsonPath("$[0].title").value("Test"));
    }

    @Test
    void byIdEndpointShouldExposeMessageField() throws Exception {
        UUID notificationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Notification notification = Notification.builder()
                .id(notificationId)
                .type(NotificationType.INCIDENT_ASSIGNED)
                .userId(userId)
                .incidentId(UUID.randomUUID())
                .title("Test")
                .message("Full message content")
                .createdAt(Instant.now())
                .build();

        when(notificationService.getById(any(), any(), eq(notificationId)))
                .thenReturn(Optional.of(notification));

        mockMvc.perform(get("/api/notifications/{id}", notificationId)
                        .accept(MediaType.APPLICATION_JSON)
                        .with(jwt().jwt(builder -> builder.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Full message content"))
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.incidentId").exists());
    }
}