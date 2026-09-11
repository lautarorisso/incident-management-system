package com.ims.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Unit tests for {@link FeignTokenRelayInterceptor} — plain JUnit, no Spring
 * context. Covers the relay, blank-header and no-context scenarios from the
 * feign-token-relay spec.
 */
class FeignTokenRelayInterceptorTest {

    private final FeignTokenRelayInterceptor interceptor = new FeignTokenRelayInterceptor();

    @AfterEach
    void resetRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void shouldForwardAuthorizationHeaderFromCurrentRequest() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer token-123");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        RequestTemplate template = new RequestTemplate();
        interceptor.apply(template);

        assertThat(template.headers().get("Authorization"))
                .containsExactly("Bearer token-123");
    }

    @Test
    void shouldNotForwardBlankAuthorizationHeader() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("   ");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        RequestTemplate template = new RequestTemplate();
        interceptor.apply(template);

        assertThat(template.headers()).doesNotContainKey("Authorization");
    }

    @Test
    void shouldNoOpWhenNoRequestContextIsActive() {
        RequestContextHolder.resetRequestAttributes();

        RequestTemplate template = new RequestTemplate();

        assertThatCode(() -> interceptor.apply(template)).doesNotThrowAnyException();
        assertThat(template.headers()).doesNotContainKey("Authorization");
    }
}