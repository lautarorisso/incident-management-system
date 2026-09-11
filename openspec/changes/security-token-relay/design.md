# Design: Security Token Relay + JWT Validation in Business Services

## Technical Approach

Each business service (incident, notification, user) becomes a JWT resource server using a shared servlet `SecurityFilterChain` factory from `services/shared` (`com.ims.shared.security`). Services import the factory explicitly via `@Import` because `com.ims.shared` is not component-scanned. The gateway retains its reactive stack but drops `/eureka/**` from public routes and removes `UserIdHeaderFilter`. The Feign hop incident → user-service relays `Authorization` via a `RequestInterceptor` reading `RequestContextHolder`. Notification endpoint enforces `sub == userId` or `ROLE_ADMIN`.

---

## Architecture Decisions

### Decision: Shared Security Library in `services/shared`

| Option | Tradeoff | Decision |
|--------|----------|----------|
| Single `SecurityFilterChainFactory` + converter + interceptor in `com.ims.shared.security` | Couples all services to shared; heavier deps | **Chosen** — consistent role mapping, one CVE fix location, matches existing `OpenApiConfigFactory` pattern |
| Per-service copy-paste | No coupling, drift risk ×3 | Rejected — this change exists to eliminate drift |

**Rationale**: The exploration confirmed `com.ims.shared` is the established cross-service utility module. Services already depend on it. Adding `spring-boot-starter-security`, `spring-boot-starter-oauth2-resource-server`, and `feign-core` to `shared/pom.xml` keeps the security surface in one place. Explicit `@Import` in each service avoids fail-open silent misconfiguration.

### Decision: SecurityFilterChain Factory API (Parametrizable Builder)

| Option | Tradeoff | Decision |
|--------|----------|----------|
| Factory returns ready `SecurityFilterChain` with fixed permitAll list | Simpler, less flexible | Rejected — each service has different endpoints |
| Factory provides `SecurityFilterChain` builder with `permitAll()` and `authorize()` methods | Each service declares its own matrix using shared converter | **Chosen** — declarative, type-safe, no duplication of role logic |

```java
// In each service's SecurityConfig
@Import(SharedSecurityConfiguration.class)
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http,
                                    JwtGrantedAuthoritiesConverter converter) throws Exception {
        return SharedSecurityConfiguration.builder(http)
                .jwtAuthenticationConverter(converter)
                .permitAll("/actuator/health", "/actuator/info", "/v3/api-docs/**", "/scalar/**")
                .authorize("/api/**").hasAnyRole("ADMIN", "AGENT", "USER")
                .build();
    }
}
```

**Rationale**: Services have different public endpoints (user-service has `/api/users/**` behind roles, notification has owner-check). A builder lets each service configure its matrix while sharing the JWT decoder, converter, and session management.

### Decision: Per-Service Role Matrices

| Service | Public (permitAll) | Authenticated (any role) | ADMIN+AGENT | ADMIN only |
|---------|-------------------|--------------------------|-------------|------------|
| **user-service** | actuator/health, info, v3/api-docs/**, scalar/** | — | `/api/users/**`, `/api/teams/**` | — |
| **incident-service** | actuator/health, info, v3/api-docs/**, scalar/** | `POST /api/incidents` | `PUT /api/incidents/*/assign`, `PUT /api/incidents/*/transition`, `GET /api/incidents` (list) | — |
| **notification-service** | actuator/health, info, v3/api-docs/**, scalar/** | — | — | `GET /api/notifications/{userId}` (owner or ADMIN via custom check) |

**Feign-invoked endpoints** (`/api/users/{id}`, `/api/teams/{id}`) are called by incident-service with the **end-user's JWT** (relayed). They require `ROLE_ADMIN` or `ROLE_AGENT` per user-service matrix. No service-account tokens.

### Decision: Gateway Cleanup

| Change | Rationale |
|--------|-----------|
| Remove `/eureka/**` from `.permitAll()` in `SecurityConfig` | Eureka dashboard must not be public; services validate JWT locally |
| Delete `UserIdHeaderFilter` entirely | Services derive identity from JWT `sub`; header is spoofable |
| Keep `Authorization` forwarding (default) | No filter strips it; downstream services now validate it |

### Decision: Notification Owner-Check Implementation

```java
@GetMapping("/{userId}")
public ResponseEntity<List<NotificationListItem>> getNotifications(
        @PathVariable UUID userId,
        Authentication authentication) { // injected by Spring Security
    String sub = authentication.getName(); // JWT sub claim
    if (!sub.equals(userId.toString()) &&
        !authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    // ... fetch notifications
}
```

- Uses `Authentication` principal (populated by resource server filter)
- Returns **403** (not 404) when `sub != userId` — existence not leaked
- Applied only to notification endpoints; incident/user services use role matrix only

### Decision: GlobalExceptionHandler Feign 401/403 Propagation

Current `handleFeignException` maps all 5xx/connectivity to 503. Change to:

```java
@ExceptionHandler(FeignException.class)
protected ProblemDetail handleFeignException(FeignException ex) {
    int status = ex.status();
    if (status == 401 || status == 403) {
        return buildProblemDetail(HttpStatus.valueOf(status), sanitizeFeignMessage(ex));
    }
    if (status >= 400 && status < 500) {
        return buildProblemDetail(HttpStatus.valueOf(status), sanitizeFeignMessage(ex));
    }
    return buildProblemDetail(HttpStatus.SERVICE_UNAVAILABLE, "Downstream service unavailable");
}
```

**Order**: Update handler **before** enabling JWT validation in incident-service (rollback plan sequence).

### Decision: Config-Server `issuer-uri` in Submodule

Config-server is a separate git submodule. Add to each service YAML:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KEYCLOAK_ISSUER_URI:http://keycloak:8080/realms/ims}
```

- `incident-service.yaml`, `notification-service.yaml`, `user-service.yaml`
- Gateway already has it in `api-gateway.yaml`
- Default points to compose Keycloak; overridden via env in CI/prod

---

## Data Flow

```
Client → Gateway (validates JWT, maps roles, forwards Authorization)
           ↓
    incident-service (validates JWT, checks role matrix)
           ↓ Feign call with Authorization header
    user-service (validates JWT, checks role matrix)
```

```
Client → Gateway → notification-service (validates JWT, owner-check: sub == userId or ROLE_ADMIN)
```

---

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `services/shared/pom.xml` | Modify | Add `spring-boot-starter-security`, `spring-boot-starter-oauth2-resource-server`, `feign-core` |
| `services/shared/src/main/java/com/ims/shared/security/SharedSecurityConfiguration.java` | Create | `@Configuration` with `SecurityFilterChainBuilder`, `JwtGrantedAuthoritiesConverter` bean |
| `services/shared/src/main/java/com/ims/shared/security/JwtGrantedAuthoritiesConverter.java` | Create | `Converter<Jwt, Collection<GrantedAuthority>>` porting `ims-*` → `ROLE_*` logic |
| `services/shared/src/main/java/com/ims/shared/security/FeignTokenRelayInterceptor.java` | Create | `RequestInterceptor` reading `Authorization` from `RequestContextHolder` |
| `services/shared/src/main/java/com/ims/shared/security/FeignTokenRelayConfig.java` | Create | Feign `@Configuration` registering interceptor |
| `services/incident-service/pom.xml` | Modify | Add `spring-boot-starter-security`, `spring-boot-starter-oauth2-resource-server` (transitive via shared) |
| `services/incident-service/.../config/SecurityConfig.java` | Create | `@Import(SharedSecurityConfiguration.class)`, declares incident role matrix |
| `services/incident-service/.../client/UserServiceClient.java` | Modify | Add `configuration = FeignTokenRelayConfig.class` |
| `services/incident-service/.../exception/GlobalExceptionHandler.java` | Modify | Propagate 401/403 from Feign |
| `services/notification-service/pom.xml` | Modify | Add security starters |
| `services/notification-service/.../config/SecurityConfig.java` | Create | `@Import`, declares matrix + owner-check in controller |
| `services/notification-service/.../controller/NotificationController.java` | Modify | Add owner-check logic in `getNotifications` |
| `services/user-service/pom.xml` | Modify | Add security starters |
| `services/user-service/.../config/SecurityConfig.java` | Create | `@Import`, declares user/team role matrix |
| `services/api-gateway/.../config/SecurityConfig.java` | Modify | Remove `/eureka/**` from permitAll |
| `services/api-gateway/.../filter/UserIdHeaderFilter.java` | Delete | Remove entirely |
| `services/config-server/incident-service.yaml` | Modify | Add `issuer-uri` |
| `services/config-server/notification-service.yaml` | Modify | Add `issuer-uri` |
| `services/config-server/user-service.yaml` | Modify | Add `issuer-uri` |

---

## Interfaces / Contracts

### SharedSecurityConfiguration (factory)

```java
package com.ims.shared.security;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.web.SecurityFilterChain;

public final class SharedSecurityConfiguration {

    private SharedSecurityConfiguration() {}

    public static Builder builder(HttpSecurity http) {
        return new Builder(http);
    }

    public static class Builder {
        private final HttpSecurity http;
        private JwtGrantedAuthoritiesConverter converter;

        Builder(HttpSecurity http) { this.http = http; }

        public Builder jwtAuthenticationConverter(JwtGrantedAuthoritiesConverter converter) {
            this.converter = converter;
            return this;
        }

        public Builder permitAll(String... patterns) {
            // configure http.authorizeHttpRequests().requestMatchers(patterns).permitAll()
            return this;
        }

        public Builder authorize(String pattern).hasAnyRole(String... roles) {
            // configure http.authorizeHttpRequests().requestMatchers(pattern).hasAnyRole(roles)
            return this;
        }

        public SecurityFilterChain build() throws Exception {
            // wire converter into oauth2ResourceServer().jwt().jwtAuthenticationConverter()
            return http.build();
        }
    }

    @Bean
    public JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter() {
        return new JwtGrantedAuthoritiesConverter();
    }
}
```

### JwtGrantedAuthoritiesConverter

```java
package com.ims.shared.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.core.convert.converter.Converter;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class JwtGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String REALM_ACCESS_CLAIM = "realm_access";
    private static final String ROLES_KEY = "roles";
    private static final String ROLE_PREFIX = "ims-";
    private static final String AUTHORITY_PREFIX = "ROLE_";

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        List<String> realmRoles = extractRealmRoles(jwt);
        return realmRoles.stream()
                .filter(r -> r.startsWith(ROLE_PREFIX))
                .map(r -> new SimpleGrantedAuthority(
                        AUTHORITY_PREFIX + r.substring(ROLE_PREFIX.length()).toUpperCase(Locale.ROOT)))
                .collect(Collectors.toSet());
    }

    @SuppressWarnings("unchecked")
    private List<String> extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim(REALM_ACCESS_CLAIM);
        if (realmAccess == null) return List.of();
        Object roles = realmAccess.get(ROLES_KEY);
        return roles instanceof List ? (List<String>) roles : List.of();
    }
}
```

### FeignTokenRelayInterceptor

```java
package com.ims.shared.security;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class FeignTokenRelayInterceptor implements RequestInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";

    @Override
    public void apply(RequestTemplate template) {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return; // no HTTP request context (scheduler, tests)

        String authHeader = attrs.getRequest().getHeader(AUTHORIZATION_HEADER);
        if (authHeader != null && !authHeader.isBlank()) {
            template.header(AUTHORIZATION_HEADER, authHeader);
        }
    }
}
```

---

## Testing Strategy

| Layer | What to Test | Approach |
|-------|--------------|----------|
| **Unit** | `JwtGrantedAuthoritiesConverter` mapping table | Plain JUnit + `@ParameterizedTest` with crafted JWTs (no Spring context) |
| **Unit** | `FeignTokenRelayInterceptor` adds header when context exists, no-op when null | Mock `RequestContextHolder`, `RequestTemplate` |
| **Integration (per service)** | Auth matrix: public endpoints 200, business endpoints 401 without token, 403 wrong role, 200 correct role | `@SpringBootTest` + `MockMvc` + `spring-security-test` `@WithJwt` / `@WithMockUser(roles=...)`; Testcontainers for DB/Mongo/RabbitMQ |
| **Integration (incident)** | `UserServiceClientWireMockTest` asserts `Authorization` header forwarded | WireMock `verify(postRequestedFor(...).withHeader("Authorization", equalTo("Bearer <token>")))` |
| **Integration (notification)** | Owner-check: 200 when `sub==userId`, 200 when `ROLE_ADMIN`, 403 otherwise | `@WithJwt` with custom claims; assert status |
| **E2E** | Full flow: gateway → incident → user-service with real Keycloak tokens | Existing `e2e-tests` unchanged (already use real tokens) |

**Test Impact (existing tests needing updates):**
- `UserServiceClientWireMockTest`: add `Authorization` header assertion in stub verification
- Controller tests in all 3 services: add `@WithMockUser` or `@WithJwt` to every test hitting secured endpoints
- `GatewayAuthorizationTest`: update expected public routes (remove `/eureka/**`)
- Any test using `X-User-Id` header: remove / migrate to JWT-based identity

---

## Threat Matrix

N/A — no routing, shell, subprocess, VCS/PR automation, executable-file classification, or process-integration boundary changed.

---

## Migration / Rollout

1. **Config-server submodule**: Commit `issuer-uri` to 3 service YAMLs first (separate repo/PR)
2. **Shared library**: Add security deps, create `SharedSecurityConfiguration`, converter, interceptor
3. **user-service** (no downstream Feign): Add SecurityConfig, update tests
4. **notification-service**: Add SecurityConfig, implement owner-check in controller, update tests
5. **incident-service**: Add SecurityConfig, attach `FeignTokenRelayConfig` to `UserServiceClient`, update `GlobalExceptionHandler` **before** enabling validation, update tests
6. **api-gateway**: Remove `/eureka/**` from permitAll, delete `UserIdHeaderFilter`
7. **E2E verification**: Run full stack; existing E2E tests pass unchanged

Rollback: Revert commits in reverse order. No schema changes, no data migration.

---

## Open Questions — RESOLVED (orchestrator gate, 2026-09-11)

- [x] **Notification endpoint path**: KEEP `GET /api/notifications?userId=X` (current `@RequestParam` contract). No change to path — owner-check compares `sub` claim against the `@RequestParam userId`. Rationale: E2E already calls `?userId=` (`IncidentFlowE2E.java:168`); changing to path variable is cosmetic and breaks clients without security gain.
- [x] **FeignTokenRelayConfig placement**: IN SHARED (`com.ims.shared.security`), activated by explicit `@Import` in incident-service. Reusable for future inter-service Feign hops.
- [x] **`JwtGrantedAuthoritiesConverter` bean name**: distinct from gateway's reactive converter — no conflict if gateway imports shared.
- [x] **`KEYCLOAK_ISSUER_URI` default**: replicate the api-gateway pattern verbatim — config-server YAMLs use `${KEYCLOAK_ISSUER_URI:http://localhost:18080/realms/ims}` (local default) and docker-compose overrides with `http://keycloak:8080/realms/ims` via env for the 3 services.