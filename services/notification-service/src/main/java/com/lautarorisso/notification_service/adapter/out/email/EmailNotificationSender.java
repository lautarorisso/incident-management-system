package com.lautarorisso.notification_service.adapter.out.email;

import com.lautarorisso.notification_service.domain.model.Notification;
import com.lautarorisso.notification_service.domain.port.out.NotificationSender;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationSender implements NotificationSender {

    private final JavaMailSender mailSender;

    @Value("${notification.email.from:no-reply@ims.local}")
    private String fromAddress;

    @Value("${notification.email.enabled:true}")
    private boolean emailEnabled;

    @Override
    public void send(Notification notification) {
        if (!emailEnabled) {
            log.info("Email sending disabled, logging notification instead: {}", notification.getId());
            logNotification(notification);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(resolveEmail(notification.getUserId()));
            helper.setSubject(notification.getTitle());
            helper.setText(buildHtmlBody(notification), true);

            mailSender.send(message);
            log.info("Email sent for notification {} to user {}", notification.getId(), notification.getUserId());

        } catch (MessagingException e) {
            log.error("Failed to send email for notification {}: {}", notification.getId(), e.getMessage(), e);
            throw new NotificationDeliveryException("Email delivery failed", e);
        }
    }

    private String resolveEmail(UUID userId) {
        // TODO: Integrate with user-service to resolve actual email from userId
        // For MVP, use a deterministic test email based on userId
        return "user-" + userId + "@ims.local";
    }

    private String buildHtmlBody(Notification notification) {
        return """
            <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #2c3e50;">%s</h2>
                    <p>%s</p>
                    <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;">
                    <p style="font-size: 12px; color: #888;">
                        Incident ID: %s<br>
                        Notification ID: %s<br>
                        Sent at: %s
                    </p>
                </div>
            </body>
            </html>
            """.formatted(
            notification.getTitle(),
            notification.getMessage(),
            notification.getIncidentId() != null ? notification.getIncidentId() : "N/A",
            notification.getId(),
            notification.getCreatedAt()
        );
    }

    private void logNotification(Notification notification) {
        log.info("""
            ==========================================
            NOTIFICATION (email disabled - logged only)
            ==========================================
            To: {}
            Type: {}
            Title: {}
            Message: {}
            Incident: {}
            Notification ID: {}
            Created: {}
            ==========================================
            """,
            resolveEmail(notification.getUserId()),
            notification.getType(),
            notification.getTitle(),
            notification.getMessage(),
            notification.getIncidentId(),
            notification.getId(),
            notification.getCreatedAt()
        );
    }

    public static class NotificationDeliveryException extends RuntimeException {
        public NotificationDeliveryException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}