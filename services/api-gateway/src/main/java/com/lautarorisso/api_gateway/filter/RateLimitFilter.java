package com.lautarorisso.api_gateway.filter;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Global filter that rate-limits requests per client using Resilience4j's
 * {@link RateLimiter}. The client is identified by the X-Forwarded-For header,
 * or falls back to the remote address.
 * <p>
 * When the rate limit is exceeded, responds with HTTP 429 Too Many Requests.
 */
@Slf4j
@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";

    private final RateLimiterRegistry rateLimiterRegistry;
    private final ConcurrentMap<String, RateLimiter> buckets = new ConcurrentHashMap<>();

    /**
     * Creates a rate-limit filter backed by Resilience4j.
     *
     * @param limitForPeriod maximum requests per refresh period per client
     * @param refreshPeriod  ISO-8601 duration of each rate-limit window (e.g. "5s")
     */
    public RateLimitFilter(
            @Value("${app.rate-limit.limit-for-period:10}") int limitForPeriod,
            @Value("${app.rate-limit.refresh-period:5s}") Duration refreshPeriod) {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(limitForPeriod)
                .limitRefreshPeriod(refreshPeriod)
                .timeoutDuration(Duration.ZERO)
                .build();
        this.rateLimiterRegistry = RateLimiterRegistry.of(config);
    }

    @Override
    public int getOrder() {
        return -80;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String clientKey = resolveClientKey(exchange.getRequest());
        RateLimiter rateLimiter = buckets.computeIfAbsent(clientKey,
                k -> rateLimiterRegistry.rateLimiter(k));

        if (rateLimiter.acquirePermission()) {
            return chain.filter(exchange);
        }

        log.warn("Rate limit exceeded for client: {}", clientKey);
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        return exchange.getResponse().setComplete();
    }

    private static String resolveClientKey(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst(X_FORWARDED_FOR);
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // Take the first IP in the chain
            return xForwardedFor.split(",")[0].trim();
        }
        if (request.getRemoteAddress() != null) {
            java.net.InetAddress address = request.getRemoteAddress().getAddress();
            if (address != null) {
                return address.getHostAddress();
            }
            return request.getRemoteAddress().getHostString();
        }
        return "unknown";
    }
}
