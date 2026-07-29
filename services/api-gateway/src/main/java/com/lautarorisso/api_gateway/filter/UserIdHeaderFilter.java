package com.lautarorisso.api_gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global filter that extracts the JWT {@code sub} claim and sets it as the
 * {@code X-User-Id} header on the downstream request.
 * <p>
 * This allows backend services to identify the authenticated user without
 * parsing the JWT themselves.
 */
@Component
public class UserIdHeaderFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(UserIdHeaderFilter.class);
    private static final String USER_ID_HEADER = "X-User-Id";

    @Override
    public int getOrder() {
        return -60;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
                .filter(auth -> auth instanceof JwtAuthenticationToken)
                .cast(JwtAuthenticationToken.class)
                .map(JwtAuthenticationToken::getToken)
                .flatMap(jwt -> Mono.justOrEmpty(jwt.getClaimAsString("sub")))
                .filter(sub -> !sub.isBlank())
                .flatMap(sub -> {
                    ServerHttpRequest request = exchange.getRequest().mutate()
                            .header(USER_ID_HEADER, sub)
                            .build();
                    log.debug("Set X-User-Id: {}", sub);
                    return chain.filter(
                            exchange.mutate().request(request).build());
                })
                .switchIfEmpty(chain.filter(exchange));
    }
}
