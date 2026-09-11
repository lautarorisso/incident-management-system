package com.ims.shared.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign client-local configuration registering {@link FeignTokenRelayInterceptor}.
 * <p>
 * Attach it only to Feign clients that call JWT-protected downstream services:
 *
 * <pre>{@code
 * @FeignClient(name = "user-service", url = "${user-service.url:}",
 *              configuration = FeignTokenRelayConfig.class)
 * public interface UserServiceClient { ... }
 * }</pre>
 *
 * The class is deliberately NOT a component-scanned Spring configuration:
 * {@code com.ims.shared} is outside the services' scan roots, and registering
 * the interceptor globally would leak bearer tokens into clients whose
 * downstream does not accept JWT. Feign instantiates this class only inside the
 * affected client's child context ({@code configuration = ...}).
 */
@Configuration
public class FeignTokenRelayConfig {

    /**
     * @return the shared relay interceptor instance for this Feign client
     */
    @Bean
    public FeignTokenRelayInterceptor feignTokenRelayInterceptor() {
        return new FeignTokenRelayInterceptor();
    }
}