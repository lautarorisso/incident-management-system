package com.lautarorisso.notification_service.controller;

import com.lautarorisso.notification_service.dto.NotificationListItem;
import com.lautarorisso.notification_service.dto.NotificationResponse;
import com.lautarorisso.notification_service.entity.Notification;
import com.lautarorisso.notification_service.entity.NotificationStatus;
import com.lautarorisso.notification_service.entity.NotificationType;
import com.lautarorisso.notification_service.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationRepository notificationRepository;

    @Test
    void getNotificationsByUserIdReturnsList() throws Exception {
        UUID userId = UUID.randomUUID();
        Notification notification = Notification.builder()
                .id(UUID.randomUUID())
                .type(NotificationType.INCIDENT_ASSIGNED)
                .userId(userId)
                .incidentId(UUID.randomUUID())
                .title("Test notification")
                .message("Test message")
                .status(NotificationStatus.UNREAD)
                .createdAt(Instant.now())
                .build();

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(notification));

        mockMvc.perform(get("/api/notifications")
                        .param("userId", userId.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Test notification"))
                .andExpect(jsonPath("$[0].status").value("UNREAD"));
    }

    @Test
    void getNotificationsByUserIdAndStatusFilters() throws Exception {
        UUID userId = UUID.randomUUID();
        Notification notification = Notification.builder()
                .id(UUID.randomUUID())
                .type(NotificationType.INCIDENT_ASSIGNED)
                .userId(userId)
                .incidentId(UUID.randomUUID())
                .title("Unread notification")
                .message("Test message")
                .status(NotificationStatus.UNREAD)
                .createdAt(Instant.now())
                .build();

        when(notificationRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, NotificationStatus.UNREAD))
                .thenReturn(List.of(notification));

        mockMvc.perform(get("/api/notifications")
                        .param("userId", userId.toString())
                        .param("status", "UNREAD")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Unread notification"))
                .andExpect(jsonPath("$[0].status").value("UNREAD"));
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
                .status(NotificationStatus.UNREAD)
                .createdAt(Instant.now())
                .build();

        when(notificationRepository.findById(notificationId))
                .thenReturn(Optional.of(notification));

        mockMvc.perform(get("/api/notifications/{id}", notificationId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(notificationId.toString()))
                .andExpect(jsonPath("$.title").value("Test notification"));
    }

    @Test
    void getNotificationByIdReturns404WhenNotFound() throws Exception {
        when(notificationRepository.findById(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/notifications/{id}", UUID.randomUUID())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void markAsReadReturnsUpdatedNotification() throws Exception {
        UUID notificationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Notification notification = Notification.builder()
                .id(notificationId)
                .type(NotificationType.INCIDENT_ASSIGNED)
                .userId(userId)
                .incidentId(UUID.randomUUID())
                .title("Test notification")
                .message("Test message")
                .status(NotificationStatus.UNREAD)
                .createdAt(Instant.now())
                .build();

        when(notificationRepository.findById(notificationId))
                .thenReturn(Optional.of(notification));
        when(notificationRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(patch("/api/notifications/{id}/read", notificationId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READ"));
    }

    @Test
    void markAsReadReturns404WhenNotificationNotFound() throws Exception {
        when(notificationRepository.findById(any())).thenReturn(Optional.empty());

        mockMvc.perform(patch("/api/notifications/{id}/read", UUID.randomUUID())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
