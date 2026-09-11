# Tasks: Security Token Relay + JWT Validation in Business Services

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 500-650 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 → PR 2 → PR 3 → PR 4 → PR 5 |
| Delivery strategy | ask-on-risk |
| Chain strategy | stacked-to-main (resolved by orchestrator, apply batch 1) |

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Focused test command | Runtime harness | Rollback boundary |
|------|------|-----------|----------------------|-----------------|-------------------|
| 1 | Shared library + config-server YAMls | PR 1 | `./mvnw test -pl services/shared` | `mvn spring-boot:run` | Revert shared security |
| 2 | user-service SecurityConfig | PR 2 | `./mvnw test -pl services/user-service` | `mvn spring-boot:run` | Revert user-service |
| 3 | notification-service + owner-check | PR 3 | `./mvnw test -pl services/notification-service` | `mvn spring-boot:run` | Revert notification-service |
| 4 | incident-service + Feign + Handler | PR 4 | `./mvnw test -pl services/incident-service` | `mvn spring-boot:run` | Revert incident-service |
| 5 | Gateway cleanup + E2E | PR 5 | `./scripts/e2e-test.sh` | docker-compose | Revert gateway changes |

## Phase 1: Foundation / Infrastructure

- [x] 1.1 Modify `services/shared/pom.xml` — add security starters
- [x] 1.2 Create `SharedSecurityConfiguration.java` — builder factory
- [x] 1.3 Create `JwtGrantedAuthoritiesConverter.java` — `ims-*` → `ROLE_*` mapping
- [x] 1.4 Create `FeignTokenRelayInterceptor.java` — `RequestContextHolder` header
- [x] 1.5 Create `FeignTokenRelayConfig.java` — Feign `@Configuration` interceptor

## Phase 2: Core Implementation

- [x] 2.1 Modify `services/user-service/pom.xml` — add security starters (resolved: starters transitives via shared per design; pom adds `spring-security-test` test scope)
- [x] 2.2 Create `services/user-service/.../config/SecurityConfig.java` — `@Import` + role matrix
- [x] 2.3 Modify `services/notification-service/pom.xml` — add security starters (resolved: transitives via shared per design; pom adds `spring-security-test` test scope)
- [x] 2.4 Create `services/notification-service/.../config/SecurityConfig.java` — `@Import` + authenticated `/api/notifications/**` (owner-check en controller)
- [x] 2.5 Modify `NotificationController.java` — owner-check: `sub == userId` or `ROLE_ADMIN` → 403 exacto, `@RequestParam userId` mantenido

## Phase 3: Integration / Wiring

- [x] 3.1 Modify `services/incident-service/pom.xml` — add security starters (resolved: starters transitives via shared per design; pom adds `spring-security-test` test scope)
- [x] 3.2 Create `incident-service/.../config/SecurityConfig.java` — `@Import` + incident matrix
- [x] 3.3 Modify `UserServiceClient.java` — add `FeignTokenRelayConfig.class`
- [x] 3.4 Modify `GlobalExceptionHandler.java` — propagate 401/403 before enabling JWT

## Phase 4: Gateway Cleanup

- [ ] 4.1 Modify `api-gateway/.../config/SecurityConfig.java` — remove `/eureka/**` from permitAll
- [ ] 4.2 Delete `api-gateway/.../filter/UserIdHeaderFilter.java` — remove entirely

## Phase 5: Config Server

- [x] 5.1 Modify `config-server/incident-service.yaml` — add `issuer-uri`
- [x] 5.2 Modify `config-server/notification-service.yaml` — add `issuer-uri`
- [x] 5.3 Modify `config-server/user-service.yaml` — add `issuer-uri`

## Phase 6: Testing

- [x] 6.1 Update `@SpringBootTest` controller tests — add `@WithJwt`/`@WithMockUser` (resolved: `SecurityMockMvcRequestPostProcessors.jwt()` en los 16 requests de `IncidentControllerTest`; matriz de roles en full-context `IncidentSecurityIntegrationTest`)
- [x] 6.2 Update `UserServiceClientWireMockTest` — assert `Authorization` forwarded (relay + propagación 401 e2e con `@MockitoBean JwtDecoder`)
- [x] 6.3 Create auth matrix tests (`@WithJwt`/`@WithMockUser`): 200/public, 401, 403/role (resolved: `IncidentSecurityIntegrationTest` full-context: 8 casos — público, sin-token 401, USER create+read 200, USER list/assign/transition 403, AGENT/ADMIN list 200)
- [x] 6.4 Create notification owner-check tests (`@WithJwt` claims): own→200, admin any→200, other→403 (+401 sin token, full-context matrix)
- [ ] 6.5 Update `GatewayAuthorizationTest` — remove `/eureka/**` from public routes

## Phase 7: Cleanup

- [ ] 7.1 Remove temporary debugging code in services
- [ ] 7.2 Verify no `X-User-Id` consumption; identity from JWT `sub`