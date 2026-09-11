package com.ims.shared.security;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Feign {@link RequestInterceptor} that relays the inbound {@code Authorization}
 * header to outgoing Feign calls.
 * <p>
 * The header is read from {@link RequestContextHolder}, which Spring fills with
 * the servlet request attributes while a request thread is active. Feign clients
 * configured with {@link FeignTokenRelayConfig} therefore forward the JWT the
 * gateway validated, so downstream JWTs-validating services accept the call.
 * <p>
 * Thread-local context safety: when no servlet request context exists (Feign
 * call from a {@code @Scheduled} method, a test, or any non-HTTP thread), the
 * interceptor is a strict no-op — it adds no header and never throws, so
 * internal callers keep working unauthenticated.
 */
public class FeignTokenRelayInterceptor implements RequestInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";

    @Override
    public void apply(RequestTemplate template) {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
            return; // no HTTP request context (scheduler, tests, non-servlet thread)
        }

        String authHeader = attrs.getRequest().getHeader(AUTHORIZATION_HEADER);
        if (authHeader != null && !authHeader.isBlank()) {
            template.header(AUTHORIZATION_HEADER, authHeader);
        }
    }
}