## Exploration: API-first token relay + validación JWT interna en servicios

### Current State

- `services/api-gateway` is the only JWT resource server: WebFlux `SecurityConfig`
  (`SecurityWebFilterChain` + `JwtRoleConverter` mapping `realm_access.roles`
  `ims-*` → `ROLE_ADMIN/AGENT/USER`), route rules per role, public
  `/actuator/**`, `/scalar/**`, `/v3/api-docs/**`, `/eureka/**`.
  `UserIdHeaderFilter` (order −60) injects `X-User-Id` from the `sub` claim.
  No `RemoveRequestHeader` filter exists, so the gateway **already forwards**
  the incoming `Authorization` header downstream by default — downstream
  services simply ignore it today.
- `incident-service`, `notification-service`, `user-service` (Spring Boot 3.5.16,
  servlet stack via `spring-boot-starter-web`) have **no** `spring-security` /
  `oauth2-resource-server` dependency and no `SecurityFilterChain` (verified by
  dependency grep on both poms, exit 1).
- `UserServiceClient` (`@FeignClient(name="user-service",
  url="${user-service.url:}")`) has **no** `RequestInterceptor`; the
  incident → user hop carries no `Authorization`. It is called synchronously
  from `IncidentService.assignIncident` (`findUserById`, `findTeamById`), i.e.
  inside an HTTP request thread whose JWT lives only in the raw
  `Authorization` header (no `SecurityContext` downstream).
- `services/shared` (`com.ims.shared` packages: DTOs, `OpenApiConfigFactory`,
  `BaseGlobalExceptionHandler`, `SharedRabbitMqConfig`) is the natural home
  for shared security code, but business apps live under `com.lautarorisso.*`
  with **no explicit `@ComponentScan`/`scanBasePackages`** — a `@Configuration`
  in `com.ims.shared` will NOT be auto-detected and must be imported
  explicitly (e.g. `@Import` or `AutoConfiguration.imports`).
- Issuer config (`spring.security.oauth2.resourceserver.jwt.issuer-uri`,
  `${KEYCLOAK_ISSUER_URI:.../realms/ims}`) exists **only** in
  `config-server/api-gateway.yaml`; the three business-service yamls have no
  `security.oauth2` block.
- Async path needs no token: `notification-service` consumes incident events
  via RabbitMQ (`@RabbitListener` in `IncidentEventListener`); `OutboxPoller`
  in incident-service is `@Scheduled` and only publishes to RabbitMQ. Neither
  performs an HTTP call, so token relay does not apply there — but it means
  HTTP-layer JWT validation never covers the event path (trust boundary = the
  broker/network).
- Tests: gateway has a stub-`ReactiveJwtDecoder` authorization matrix
  (`GatewayAuthorizationTest`); incident-service has `UserServiceClientWireMockTest`
  (WireMock stubs assert no auth headers today); E2E (`TokenProvider`,
  password grant against `ims-frontend`) already uses real tokens and will
  exercise the new validation transparently.
- **Premise correction for the orchestrator**: "el gateway no reenvía el
  Authorization" is inaccurate — nothing strips it, so it already propagates
  on the gateway → service hop. The real gaps are (a) services don't validate
  it, and (b) the Feign hop incident → user drops it (Feign never forwards
  incoming headers without an interceptor).

### Affected Areas

- `services/shared/src/main/java/com/ims/shared/...` (new: `security/` —
  role converter + resource-server config factory + Feign relay interceptor)
  — shared implementation, one place to maintain.
- `services/shared/pom.xml` — new deps (`spring-boot-starter-security`,
  `spring-boot-starter-oauth2-resource-server`, `spring-cloud-starter-openfeign`
  or `feign-core` for the interceptor type).
- `services/incident-service/.../client/UserServiceClient.java` — attach
  relay interceptor (via `configuration =` or default Feign config).
- `services/incident-service`, `user-service`, `notification-service`
  `pom.xml` — consume the secured shared starter (or direct security deps).
- `services/config-server/{incident,user,notification}-service.yaml` — add
  `spring.security.oauth2.resourceserver.jwt.issuer-uri`.
- `services/api-gateway/.../config/SecurityConfig.java` — decide whether the
  public `/eureka/**` route and docs routes stay as-is (reviewed, likely keep
  docs public, reconsider Eureka dashboard exposure).
- Tests: `UserServiceClientWireMockTest` (assert `Authorization` forwarded),
  new per-service `@SpringBootTest` auth-matrix tests (mirror
  `GatewayAuthorizationTest` with servlet `spring-security-test`), E2E unchanged.
- `IncidentService.assignIncident` error mapping — downstream 401/403 from
  user-service will surface as `FeignException`; `GlobalExceptionHandler`
  currently maps downstream failures to 503, which would mask auth errors.

### Approaches

1. **Shared security library in `services/shared` (recommended)** — one
   servlet `SecurityFilterChain` factory + one `JwtGrantedAuthoritiesConverter`
   (reuse the `ims-*` → `ROLE_*` logic, ported from reactive to servlet) +
   one Feign `RequestInterceptor` relaying `Authorization` from
   `RequestContextHolder`; each service imports it explicitly.
   - Pros: single implementation, consistent role matrix, one place to fix CVEs/config; matches existing `shared` pattern (`OpenApiConfigFactory`).
   - Cons: package-scan gotcha (`com.ims.shared` not scanned — needs explicit `@Import`/auto-config registration); `shared` gains security+feign deps (heavier for all consumers); cross-service change requires coordinated rollout.
   - Effort: Medium

2. **Per-service copy-paste `SecurityConfig`** — duplicate a small
   `SecurityFilterChain` + converter in each of the 3 services, plus an
   interceptor only in incident-service.
   - Pros: no coupling to `shared`, each service evolves auth independently, smallest blast radius per PR.
   - Cons: role-mapping logic triplicated (drift risk — the exact bug class this change should kill); 3× test maintenance.
   - Effort: Medium (slightly more code, less coordination)

3. **Gateway-only hardening (no per-service JWT)** — keep services open,
   rely on network isolation (compose network, no published ports) + optional
   mTLS or shared-secret header between gateway and services.
   - Pros: least code, no Keycloak/JWKS dependency in 3 services, no latency added.
   - Cons: no defense in depth (any container/network access = full API access);
     secret-header is weaker than JWT and rotates poorly; contradicts the
     change objective; Eureka + actuator exposure stays risky.
   - Effort: Low

### Recommendation

Approach 1 (shared library), with these concretions for the proposal phase:
- Port `JwtRoleConverter` logic to a servlet `Converter<Jwt, Collection<GrantedAuthority>>`
  in `com.ims.shared.security`, shared by gateway (keep reactive one) and services.
- Feign interceptor reads `Authorization` from `RequestContextHolder`
  (`RequestContextHolder.getRequestAttributes()` → `NativeWebRequest`), forwards
  as-is; no-op when absent (WireMock/scheduler threads). Only incident-service
  needs it today.
- Each service declares its own role matrix (mirroring gateway route rules:
  `POST /api/incidents` any role; assign/transition ADMIN+AGENT; users/teams
  ADMIN+AGENT; notifications: owner-or-staff TBD — **open decision**).
- Stop trusting `X-User-Id` for authorization once JWT is validated; derive
  identity from `sub` (keep header transiently for compat, document removal).
- Add `issuer-uri` to the three config-server yamls; fix `GlobalExceptionHandler`
  to propagate 401/403 from Feign instead of mapping to 503.
- Tests: per-service auth matrix (servlet equivalent of
  `GatewayAuthorizationTest` using `@WithJwt`/`spring-security-test`),
  WireMock assertion on `Authorization`, E2E as regression net.

### Risks

- **Open decision: notification authorization model** — `GET /api/notifications?userId=X`
  takes identity as a query param; JWT validation alone doesn't stop user A
  reading user B's notifications. Needs owner-check (`sub` == `userId` or
  staff role). Unresolved → proposal must define it.
- **Open decision: Eureka dashboard via gateway** — `/eureka/**` is public
  today; with defense in depth this is the weakest public surface. Decide:
  restrict, remove route, or accept.
- **Feign 401/403 masking** — without handler changes, an expired token on
  the incident → user hop returns 503 instead of 401, hiding auth failures
  from clients and E2E.
- **`X-User-Id` spoofing window** — until services derive identity from the
  JWT, any caller bypassing the gateway can impersonate via the header.
  Mitigated by the change itself; order of rollout matters.
- **JWKS/issuer availability** — services become dependent on Keycloak
  reachability at startup/first-request (decoder initialization); compose
  ordering and E2E health gates must account for it.
- **`shared` dependency weight** — adding security+feign to `shared` affects
  every consumer; keep the security piece in a separate artifact or optional
  import if the team prefers lean modules (minor).

### Ready for Proposal

Yes. Proposal should confirm: (a) notification owner-check rule, (b) Eureka
public-route fate, (c) shared-library vs per-service if the team rejects the
recommendation, (d) whether `X-User-Id` removal is in scope or follow-up.
Tell the user the orchestrator premise about the gateway not forwarding
`Authorization` was corrected: it forwards; services ignore.
