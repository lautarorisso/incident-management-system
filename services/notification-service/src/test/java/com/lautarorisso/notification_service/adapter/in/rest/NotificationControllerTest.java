package com.lautarorisso.notification_service.adapter.in.rest;

import com.lautarorisso.notification_service.adapter.in.rest.controller.NotificationController;
import com.lautarorisso.notification_service.adapter.in.rest.dto.NotificationListItem;
import com.lautarorisso.notification_service.adapter.in.rest.dto.NotificationResponse;
import com.lautarorisso.notification_service.adapter.in.rest.mapper.NotificationRestMapper;
import com.lautarorisso.notification_service.domain.model.Notification;
import com.lautarorisso.notification_service.domain.model.NotificationId;
import com.lautarorisso.notification_service.domain.model.NotificationStatus;
import com.lautarorisso.notification_service.domain.model.NotificationType;
import com.lautarorisso.notification_service.domain.port.out.NotificationRepository;
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

    @MockitoBean
    private NotificationRestMapper notificationRestMapper;

    @Test
    void getNotificationsByUserIdReturnsList() throws Exception {
        UUID userId = UUID.randomUUID();
        var listItem = NotificationListItem.builder()
                .id(UUID.randomUUID())
                .type("INCIDENT_ASSIGNED")
                .title("Test notification")
                .status("UNREAD")
                .build();

        when(notificationRepository.findByUserId(userId))
                .thenReturn(List.of(Notification.builder().build()));
        when(notificationRestMapper.toListItemList(any()))
                .thenReturn(List.of(listItem));

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
        var listItem = NotificationListItem.builder()
                .id(UUID.randomUUID())
                .type("INCIDENT_ASSIGNED")
                .title("Unread notification")
                .status("UNREAD")
                .build();

        when(notificationRepository.findByUserIdAndStatus(any(), any()))
                .thenReturn(List.of(Notification.builder().build()));
        when(notificationRestMapper.toListItemList(any()))
                .thenReturn(List.of(listItem));

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
        var response = NotificationResponse.builder()
                .id(notificationId)
                .type("INCIDENT_ASSIGNED")
                .userId(userId)
                .title("Test notification")
                .message("Test message")
                .status("UNREAD")
                .build();

        when(notificationRepository.findById(any()))
                .thenReturn(Optional.of(Notification.builder().build()));
        when(notificationRestMapper.toResponse(any()))
                .thenReturn(response);

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
        var response = NotificationResponse.builder()
                .id(notificationId)
                .type("INCIDENT_ASSIGNED")
                .userId(userId)
                .title("Test notification")
                .message("Test message")
                .status("READ")
                .build();

        Notification domain = Notification.builder()
                .id(new NotificationId(notificationId))
                .type(NotificationType.INCIDENT_ASSIGNED)
                .userId(userId)
                .title("Test notification")
                .message("Test message")
                .status(NotificationStatus.UNREAD)
                .createdAt(Instant.now())
                .build();

        when(notificationRepository.findById(new NotificationId(notificationId)))
                .thenReturn(Optional.of(domain));
        when(notificationRepository.save(any()))
                .thenReturn(domain.withStatus(NotificationStatus.READ));
        when(notificationRestMapper.toResponse(any()))
                .thenReturn(response);

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
