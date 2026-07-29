package com.lautarorisso.notification_service.adapter.in.rest.controller;

import com.lautarorisso.notification_service.adapter.in.rest.dto.NotificationListItem;
import com.lautarorisso.notification_service.adapter.in.rest.dto.NotificationResponse;
import com.lautarorisso.notification_service.adapter.in.rest.mapper.NotificationRestMapper;
import com.lautarorisso.notification_service.domain.model.Notification;
import com.lautarorisso.notification_service.domain.model.NotificationId;
import com.lautarorisso.notification_service.domain.model.NotificationStatus;
import com.lautarorisso.notification_service.domain.port.out.NotificationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for notification read and update operations.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notification management endpoints")
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final NotificationRestMapper mapper;

    @GetMapping
    @Operation(summary = "Get notifications for a user",
            description = "Returns notifications for the specified user, optionally filtered by status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of notifications",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = NotificationListItem.class)))),
            @ApiResponse(responseCode = "400", description = "Missing required userId parameter")
    })
    public ResponseEntity<List<NotificationListItem>> getNotifications(
            @Parameter(description = "User ID to get notifications for", required = true)
            @RequestParam UUID userId,
            @Parameter(description = "Filter by status (UNREAD, READ)")
            @RequestParam(required = false) String status) {

        List<Notification> notifications;
        if (status != null) {
            notifications = notificationRepository.findByUserIdAndStatus(userId, NotificationStatus.valueOf(status));
        } else {
            notifications = notificationRepository.findByUserId(userId);
        }

        return ResponseEntity.ok(mapper.toListItemList(notifications));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a notification by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notification found",
                    content = @Content(schema = @Schema(implementation = NotificationResponse.class))),
            @ApiResponse(responseCode = "404", description = "Notification not found")
    })
    public ResponseEntity<NotificationResponse> getNotificationById(
            @Parameter(description = "Notification ID", required = true)
            @PathVariable UUID id) {

        return notificationRepository.findById(new NotificationId(id))
                .map(mapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark a notification as read",
            description = "Sets the notification status to READ and returns the updated notification")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notification marked as read",
                    content = @Content(schema = @Schema(implementation = NotificationResponse.class))),
            @ApiResponse(responseCode = "404", description = "Notification not found")
    })
    public ResponseEntity<NotificationResponse> markAsRead(
            @Parameter(description = "Notification ID", required = true)
            @PathVariable UUID id) {

        Optional<Notification> found = notificationRepository.findById(new NotificationId(id));
        if (found.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Notification updated = found.get().withStatus(NotificationStatus.READ);
        notificationRepository.save(updated);

        return ResponseEntity.ok(mapper.toResponse(updated));
    }
}
