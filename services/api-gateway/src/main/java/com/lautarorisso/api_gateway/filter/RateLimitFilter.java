package com.lautarorisso.api_gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Global filter that rate-limits requests per client using a token bucket
 * algorithm. The client is identified by the X-Forwarded-For header, or
 * falls back to the remote address.
 * <p>
 * When the bucket is empty, responds with HTTP 429 Too Many Requests.
 */
@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final String X_FORWARDED_FOR = "X-Forwarded-For";

    private final ConcurrentMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    private final int capacity;
    private final int refillRatePerSecond;

    /**
     * Creates a rate-limit filter with the given capacity and refill rate.
     *
     * @param capacity           maximum tokens per client
     * @param refillRatePerSecond tokens added per second
     */
    public RateLimitFilter(int capacity, int refillRatePerSecond) {
        this.capacity = capacity;
        this.refillRatePerSecond = refillRatePerSecond;
    }

    /** Default constructor used by Spring — 10 tokens capacity, 2 refill per second. */
    public RateLimitFilter() {
        this(10, 2);
    }

    @Override
    public int getOrder() {
        return -80;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String clientKey = resolveClientKey(exchange.getRequest());
        TokenBucket bucket = buckets.computeIfAbsent(clientKey,
                k -> new TokenBucket(capacity, refillRatePerSecond));

        if (bucket.tryConsume()) {
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

    /**
     * Simple token bucket with time-based refill. Not thread-safe per bucket;
     * callers must synchronize access per bucket instance.
     */
    static class TokenBucket {
        private final int capacity;
        private final long refillIntervalNanos;
        private double tokens;
        private long lastRefillNanos;

        TokenBucket(int capacity, int refillRatePerSecond) {
            this.capacity = capacity;
            this.refillIntervalNanos = 1_000_000_000L / refillRatePerSecond;
            this.tokens = capacity;
            this.lastRefillNanos = System.nanoTime();
        }

        synchronized boolean tryConsume() {
            refill();
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.nanoTime();
            long elapsed = now - lastRefillNanos;
            if (elapsed >= refillIntervalNanos) {
                double tokensToAdd = (double) elapsed / refillIntervalNanos;
                tokens = Math.min(capacity, tokens + tokensToAdd);
                lastRefillNanos = now;
            }
        }

        // Visible for testing
        double getTokens() {
            return tokens;
        }
    }
}
