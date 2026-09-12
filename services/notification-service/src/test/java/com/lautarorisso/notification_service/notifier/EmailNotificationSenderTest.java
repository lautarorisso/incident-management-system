package com.lautarorisso.notification_service.notifier;

import com.ims.shared.exception.NotificationDeliveryException;
import com.lautarorisso.notification_service.entity.Notification;
import com.lautarorisso.notification_service.enums.NotificationStatus;
import com.lautarorisso.notification_service.enums.NotificationType;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailNotificationSenderTest {

    @Mock
    private JavaMailSender mailSender;

    private Notification sampleNotification() {
        return Notification.builder()
                .id(UUID.randomUUID())
                .type(NotificationType.INCIDENT_ASSIGNED)
                .userId(UUID.randomUUID())
                .incidentId(UUID.randomUUID())
                .title("Test Title")
                .message("Test message body")
                .status(NotificationStatus.UNREAD)
                .createdAt(Instant.now())
                .build();
    }

    private MimeMessage realMimeMessage() {
        return new MimeMessage(Session.getInstance(new Properties()));
    }

    @Test
    void shouldSendEmailWhenEnabled() {
        EmailNotificationSender sender = new EmailNotificationSender(mailSender, "from@test.local", true);
        MimeMessage mimeMessage = realMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        sender.send(sampleNotification());

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void shouldNotSendEmailWhenDisabled() {
        EmailNotificationSender sender = new EmailNotificationSender(mailSender, "from@test.local", false);

        sender.send(sampleNotification());

        verify(mailSender, never()).createMimeMessage();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void shouldThrowNotificationDeliveryExceptionOnMessagingFailure() throws Exception {
        EmailNotificationSender sender = new EmailNotificationSender(mailSender, "from@test.local", true);
        MimeMessage failingMessage = mock(MimeMessage.class);
        doThrow(new MessagingException("SMTP connection refused"))
                .when(failingMessage).setSubject(anyString(), anyString());
        when(mailSender.createMimeMessage()).thenReturn(failingMessage);

        NotificationDeliveryException ex = assertThrows(
                NotificationDeliveryException.class,
                () -> sender.send(sampleNotification()));

        assertTrue(ex.getMessage().contains("Email delivery failed"));
        assertTrue(ex.getCause() instanceof MessagingException);
    }

    /**
     * Verifies the wrapped {@link NotificationDeliveryException} preserves the
     * original {@link MessagingException} and its message. Production logging
     * deliberately includes the cause ({@code log.error(..., e)}) for diagnostics;
     * this test does not assert on logs.
     */
    @Test
    void shouldPreserveMessagingExceptionCauseAndMessage() throws Exception {
        EmailNotificationSender sender = new EmailNotificationSender(mailSender, "from@test.local", true);
        MimeMessage failingMessage = mock(MimeMessage.class);
        doThrow(new MessagingException("internal details"))
                .when(failingMessage).setSubject(anyString(), anyString());
        when(mailSender.createMimeMessage()).thenReturn(failingMessage);

        NotificationDeliveryException ex = assertThrows(
                NotificationDeliveryException.class,
                () -> sender.send(sampleNotification()));

        assertNotNull(ex.getCause());
        assertEquals("internal details", ex.getCause().getMessage());
    }
}
