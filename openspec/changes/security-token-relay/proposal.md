# Proposal: Security Token Relay + JWT Validation in Business Services

## Intent

The gateway is the only JWT resource server today. Business services (incident, notification, user) accept unauthenticated HTTP traffic and trust `X-User-Id` headers — any caller bypassing the gateway can impersonate any user. The Feign hop from incident → user-service drops `Authorization` entirely (no interceptor), masking downstream auth failures as 503. This change adds defense-in-depth: each service validates JWTs locally and the Feign call relays tokens.

## Scope

### In Scope

- **Shared security library** (`services/shared`): servlet `SecurityFilterChain` factory, `JwtGrantedAuthoritiesConverter` (port `ims-*` → `ROLE_*` logic from gateway), Feign `RequestInterceptor` using `RequestContextHolder`
- **JWT validation** in incident-service, notification-service, user-service (add `oauth2-resource-server` dependency + `SecurityFilterChain` via shared lib)
- **Feign token relay** in incident-service (`UserServiceClient`) — propagate `Authorization` downstream
- **Notification owner-check**: `GET /api/notifications/{userId}` → `sub == userId` or `ROLE_ADMIN`; 403 otherwise
- **Remove `/eureka/**` from gateway public routes** — dashboard stays internal to compose network only
- **Remove `UserIdHeaderFilter`** from gateway + all `X-User-Id` consumption in services
- **Config-server**: add `issuer-uri` to `incident-service.yaml`, `notification-service.yaml`, `user-service.yaml`
- **Feign error mapping**: `GlobalExceptionHandler` propagates 401/403 from downstream instead of 503
- **Tests**: per-service auth matrix tests (`@WithJwt` / `spring-security-test`), update `UserServiceClientWireMockTest` to assert `Authorization` forwarded

### Out of Scope

- mTLS between gateway ↔ services (future hardening)
- Async/event-path JWT (RabbitMQ messages — trust boundary = broker)
- Custom security scopes beyond `ims-*` role mapping
- Config-server is a separate git submodule — its test coverage is deferred

## Capabilities

### New Capabilities

- `jwt-resource-server`: Servlet JWT validation via `oauth2-resource-server` + shared `SecurityFilterChain` factory in `com.ims.shared.security`
- `feign-token-relay`: `RequestInterceptor` that forwards `Authorization` from `RequestContextHolder` to outgoing Feign calls
- `role-mapping`: Port reactive `JwtRoleConverter` logic to servlet `Converter<Jwt, Collection<GrantedAuthority>>` — `ims-admin` → `ROLE_ADMIN`, `ims-agent` → `ROLE_AGENT`, `ims-user` → `ROLE_USER`
- `notification-owner-check`: Authorization rule: notification access requires `sub == userId` parameter or `ROLE_ADMIN`
- `gateway-cleanup`: Remove `/eureka/**` public route and `UserIdHeaderFilter` from gateway

### Modified Capabilities

None — no existing specs to modify (fresh `openspec/specs/`)

## Approach

Shared library in `services/shared` (`com.ims.shared.security`). Explicit `@Import` in each service (component-scan gap: `com.ims.shared` not auto-scanned). Per-service `SecurityConfig` imports shared factory, declares its own role matrix. Rollout order: (1) shared library, (2) user-service (no downstream Feign), (3) notification-service, (4) incident-service (has Feign hop — most risk), (5) gateway cleanup. This ordering minimizes spoofing window.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `services/shared/src/main/java/com/ims/shared/security/` | New | `SecurityFilterChainFactory`, `JwtRoleConverter`, `FeignTokenRelayInterceptor` |
| `services/shared/pom.xml` | Modified | Add `spring-boot-starter-security`, `oauth2-resource-server`, `feign-core` |
| `services/incident-service/.../client/UserServiceClient.java` | Modified | Attach relay interceptor |
| `services/incident-service/.../exception/GlobalExceptionHandler.java` | Modified | Propagate 401/403 from Feign |
| `services/incident-service/.../config/SecurityConfig.java` | New | Import shared factory |
| `services/notification-service/.../config/SecurityConfig.java` | New | Import shared + owner-check |
| `services/user-service/.../config/SecurityConfig.java` | New | Import shared factory |
| `services/api-gateway/.../config/SecurityConfig.java` | Modified | Remove `/eureka/**` |
| `services/api-gateway/.../filter/UserIdHeaderFilter.java` | Removed | Deleted entirely |
| `services/config-server/{incident,notification,user}-service.yaml` | Modified | Add `issuer-uri` |
| Tests across 3 services + gateway | Modified | Auth matrix, WireMock assertions |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| `X-User-Id` spoofing window during rollout | High | Rollout order: services first, gateway filter removal last |
| Feign 401/403 masked as 503 if handler not updated | Medium | Update `GlobalExceptionHandler` before enabling validation |
| `com.ims.shared` not auto-scanned | Certain | Explicit `@Import` in each service's main config class |
| Keycloak dependency at startup | Medium | Health gate in E2E already waits for Keycloak; decoder initializes lazy |
| `shared` gains heavier deps | Low | Isolate security in sub-package; services already pull transitive deps |

## Rollback Plan

1. Revert commit(s) touching `services/shared/security/`, per-service `SecurityConfig`, config-server yamls
2. Restore `UserIdHeaderFilter` and `/eureka/**` route in gateway
3. Remove `oauth2-resource-server` dependency from each service `pom.xml`
4. Rollback is non-destructive: no schema changes, no data migration, no breaking API contracts

## Dependencies

- Config-server is a separate git submodule — must commit `issuer-uri` changes there first
- E2E test stack must have Keycloak reachable at health-gate time (already the case)

## Success Criteria

- [ ] All 3 services return 401 for requests without `Authorization`
- [ ] All 3 services return 403 for requests with insufficient roles
- [ ] `GET /api/notifications/{userId}` returns 403 when `sub != userId` and role is not ADMIN
- [ ] Feign call incident → user-service forwards `Authorization` header (WireMock assertion)
- [ ] `/eureka/**` returns 401/403 at gateway level (not public)
- [ ] `UserIdHeaderFilter` deleted, no `X-User-Id` usage in any service
- [ ] Existing E2E tests pass unchanged (they already use real tokens)
- [ ] No H2/flapdoodle test replacements — all tests run against real Testcontainers
