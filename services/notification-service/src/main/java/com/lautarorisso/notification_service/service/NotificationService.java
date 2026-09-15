package com.lautarorisso.notification_service.service;

import com.lautarorisso.notification_service.entity.Notification;
import com.lautarorisso.notification_service.enums.NotificationReadStatus;
import com.lautarorisso.notification_service.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Application-layer service for notification read and update use cases.
 * <p>
 * Owns the owner-check ({@code JWT sub == userId} or {@code ROLE_ADMIN}) so
 * the controller stays a thin HTTP mapping layer. HTTP semantics are
 * preserved exactly: 404 when the notification does not exist, 403 when it
 * exists but the caller is not its owner — never 404 for a found-but-forbidden
 * notification, so existence is not leaked.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /**
     * Returns the notifications of {@code userId}, optionally filtered by read
     * status. Throws {@link ResponseStatusException} (403) when the caller is
     * neither the owner nor an admin.
     */
    public List<Notification> getNotifications(Jwt jwt, Authentication authentication,
                                               UUID userId, NotificationReadStatus readStatus) {
        if (!isOwnerOrAdmin(jwt, authentication, userId)) {
            throw forbidden();
        }
        if (readStatus != null) {
            return notificationRepository.findByUserIdAndReadStatusOrderByCreatedAtDesc(userId, readStatus);
        }
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Returns the notification by id, or empty when it does not exist. Throws
     * {@link ResponseStatusException} (403) when it exists but the caller is
     * neither the owner nor an admin.
     */
    public Optional<Notification> getById(Jwt jwt, Authentication authentication, UUID id) {
        Optional<Notification> found = notificationRepository.findById(id);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        if (!isOwnerOrAdmin(jwt, authentication, found.get().getUserId())) {
            throw forbidden();
        }
        return found;
    }

    /**
     * Marks a notification as read (read status preserved as READ; delivery
     * status untouched). Returns empty when the notification does not exist,
     * throws {@link ResponseStatusException} (403) when it exists but the
     * caller is neither the owner nor an admin.
     */
    public Optional<Notification> markAsRead(Jwt jwt, Authentication authentication, UUID id) {
        Optional<Notification> found = notificationRepository.findById(id);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        Notification notification = found.get();
        if (!isOwnerOrAdmin(jwt, authentication, notification.getUserId())) {
            throw forbidden();
        }
        Notification updated = notification.withReadStatus(NotificationReadStatus.READ);
        return Optional.of(notificationRepository.save(updated));
    }

    private ResponseStatusException forbidden() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Not the notification owner and not an admin");
    }

    /**
     * Owner-check for notification access: a caller may read or update the
     * notifications of {@code userId} only when the JWT {@code sub} claim
     * equals it, or when the caller holds {@code ROLE_ADMIN}. Violations
     * surface as 403 (never 404) so notification existence is not leaked.
     */
    private boolean isOwnerOrAdmin(Jwt jwt, Authentication authentication, UUID userId) {
        String sub = jwt != null ? jwt.getSubject() : null;
        boolean admin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        return userId.toString().equals(sub) || admin;
    }
}