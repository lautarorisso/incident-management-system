package com.lautarorisso.notification_service.notifier;

import com.ims.shared.exception.NotificationDeliveryException;
import com.lautarorisso.notification_service.entity.Notification;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class EmailNotificationSender {

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final boolean emailEnabled;

    public EmailNotificationSender(
            JavaMailSender mailSender,
            @Value("${notification.email.from:no-reply@ims.local}") String fromAddress,
            @Value("${notification.email.enabled:true}") boolean emailEnabled) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.emailEnabled = emailEnabled;
    }

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
            helper.setTo(resolveEmail(notification));
            helper.setSubject(notification.getTitle());
            helper.setText(buildHtmlBody(notification), true);

            mailSender.send(message);
            log.info("Email sent for notification {} to user {}", notification.getId(), notification.getUserId());

        } catch (MessagingException e) {
            log.error("Failed to send email for notification {}: {}", notification.getId(), e.getMessage(), e);
            throw new NotificationDeliveryException("Email delivery failed", e);
        }
    }

    /**
     * Resolves the email address for the notification. Prefers the
     * denormalized {@code recipientEmail} carried by the event payload;
     * falls back to the placeholder address for legacy or assignee-less
     * events (which carry no email).
     */
    private String resolveEmail(Notification notification) {
        String realEmail = notification.getRecipientEmail();
        if (realEmail != null && !realEmail.isBlank()) {
            return realEmail;
        }
        log.warn("No recipient email for notification {} (legacy/no-assignee event), using placeholder",
                notification.getId());
        return placeholderEmail(notification.getUserId());
    }

    private String placeholderEmail(UUID userId) {
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
            placeholderEmail(notification.getUserId()),
            notification.getType(),
            notification.getTitle(),
            notification.getMessage(),
            notification.getIncidentId(),
            notification.getId(),
            notification.getCreatedAt()
        );
    }
}
