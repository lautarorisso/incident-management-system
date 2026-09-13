# Incident Management System

Microservices-based incident management system built with Spring Boot 3.5, Spring Cloud 2025, and Java 21.

> **Note**: This is a work-in-progress MVP for portfolio demonstration. It runs fully containerized with Keycloak authentication (realm, users and roles are provisioned automatically).

## Architecture

```
                    ┌──────────────┐
                    │  api-gateway │ :8080
                    │  (Spring     │
                    │   Cloud GW)  │
                    └──────┬───────┘
                           │
           ┌────────────────┼────────────────┐
           │                │                │
┌─────────▼──────┐ ┌──────▼───────┐ ┌──────▼───────┐
│ incident-      │ │ notification-│ │ user-service │
│ service        │ │ service      │ │              │
│ :8081          │ │ :8083        │ │ :8082        │
└───────┬────────┘ └──────┬───────┘ └──────┬───────┘
        │                 │                │
   ┌────▼────┐      ┌────▼────┐     ┌────▼────┐
   │PostgreSQL│     │ MongoDB  │    │PostgreSQL│
   │incident_db│   │notification_db│ │ user_db  │
   └─────────┘     └─────────┘    └─────────┘
        │                 │
        └────────┬────────┘
            ┌────▼────┐
            │RabbitMQ │
            └─────────┘

┌──────────────────────────────────────────────────────┐
│ discovery-service (Eureka) :8761                      │
│ config-server :8888                                   │
│ PostgreSQL :5432 | MongoDB :27017 | RabbitMQ :5672    │
└──────────────────────────────────────────────────────┘
```

All domain services use a **layered architecture** (controller → service → repository), where the JPA entity serves as both the persistence and domain model. Cross-service DTOs and shared infrastructure beans live in the `services/shared` module.

## Services

| Service | Port | Description |
|---------|------|-------------|
| api-gateway | 8080 | Routing, rate limiting, circuit breakers, request logging, JWT validation (Keycloak) |
| config-server | 8888 | Spring Cloud Config Server — central configuration for all services |
| discovery-service | 8761 | Eureka Service Discovery Server |
| incident-service | 8081 | Incident CRUD, self-validating domain model, outbox pattern, RabbitMQ events |
| notification-service | 8083 | Consumes incident events from RabbitMQ, persists and delivers notifications |
| user-service | 8082 | Read-only user profiles and teams |

## Infrastructure

| Component | Port | UI |
|-----------|------|----|
| PostgreSQL | 5432 | — |
| MongoDB | 27017 | — |
| RabbitMQ | 5672 | http://localhost:15672 |

## Prerequisites

- Docker & Docker Compose

## Quick Start

`config-server` is a git submodule (see [Config Server](#config-server)), so clone it recursively:

```bash
git clone --recursive https://github.com/lautarorisso/incident-management-system
cd incident-management-system
docker compose up -d --build
```

> Already cloned without `--recursive`? Run `git submodule update --init --recursive`.

This builds and starts all 10 containers (4 infra + 6 services). Wait ~60 seconds for all services to register in Eureka, then:

| URL | What |
|-----|------|
| http://localhost:8080 | API Gateway |
| http://localhost:8761 | Eureka Dashboard |
| http://localhost:8888 | Config Server Health |
| http://localhost:15672 | RabbitMQ Management UI (guest/guest) |

## Running Tests

Tests require Java 21 and Maven locally (Docker builds don't run tests).

The user-service and incident-service tests use Testcontainers with a real PostgreSQL 16 container, so Docker must be running.

Run all tests from the project root:

```bash
./mvnw test
```

Run tests for a specific service:

```bash
./mvnw test -pl services/incident-service
./mvnw test -pl services/notification-service
./mvnw test -pl services/user-service
./mvnw test -pl services/api-gateway
./mvnw test -pl services/discovery-service
```

> `config-server` is no longer a module of this reactor — it is a git submodule.
> Test it from inside the submodule: `cd services/config-server && ./mvnw test`.

Run a specific test class:

```bash
./mvnw test -pl services/incident-service -Dtest=IncidentControllerTest
```

## Smoke Test

After starting all services with Docker, run the end-to-end smoke test:

```bash
./scripts/smoke-test.sh
```

The smoke test validates:
- Health checks for all 5 services
- Keycloak authentication (password grant against the `ims` realm)
- Create an incident (via API Gateway, with bearer token)
- Retrieve and list incidents
- Verify notification was created
- Check user service availability

To test against Docker deployments:

```bash
GATEWAY_URL=http://localhost:8080 ./scripts/smoke-test.sh
```

### Authentication (Keycloak)

The gateway is a JWT resource server: every `/api/**` request requires a
bearer token issued by a Keycloak `ims` realm. Realm roles map to authorities as
`ims-admin → ROLE_ADMIN`, `ims-agent → ROLE_AGENT`, `ims-user → ROLE_USER`.

> **Note**: JWT validation is configured via Config Server (`KEYCLOAK_ISSUER_URI`).
> In the Docker stack, Keycloak is provisioned automatically (realm, client,
> roles and seed users) and the issuer URI points at the internal `keycloak`
> host.

| Route | Required roles |
|-------|----------------|
| `POST /api/incidents` | ADMIN, AGENT or USER (any authenticated role) |
| `GET /api/incidents` (list) | ADMIN, AGENT (USER → 403) |
| `GET /api/incidents/{id}` | any authenticated role |
| `PUT /api/incidents/{id}/assign`, `PUT /api/incidents/{id}/transition` | ADMIN, AGENT |
| `GET /api/notifications?userId={id}` | owner (JWT `sub` == userId) or ADMIN — others → 403 |
| `GET /api/notifications/{id}`, `PATCH /api/notifications/{id}/read` | owner or ADMIN — non-owner → 403 |
| `/api/users/**`, `/api/teams/**` | ADMIN, AGENT |
| actuator, scalar, api-docs | public |

To obtain a token manually (dev client with direct access grants enabled):

```bash
curl -s http://localhost:18080/realms/ims/protocol/openid-connect/token \
  -d grant_type=password -d client_id=ims-frontend \
  -d username=agente1 -d password=agente1234 | jq -r .access_token

# then call the API through the gateway
curl -s http://localhost:8080/api/users -H "Authorization: Bearer $TOKEN"
```

Seed users provisioned in the `ims` realm (re-created on a fresh `docker compose up -d --build`):

| Username | Password | Realm role |
|----------|----------|------------|
| `agente1` | `agente1234` | `ims-agent` |
| `lautaro` | `admin1234` | `ims-admin` |
| `usuario1` | `usuario1234` | `ims-user` |

Smoke test auth knobs (all optional): `KEYCLOAK_URL`, `KEYCLOAK_REALM`,
`KEYCLOAK_CLIENT`, `SMOKE_USER`, `SMOKE_PASSWORD`.

## E2E Tests (REST Assured)

The repo ships a black-box E2E module (`e2e-tests/`) that drives the real
docker-compose stack over HTTP with REST Assured — no Spring context, no
project artifacts, no container internals. It is the formalized replacement
for the fragile manual paths of `scripts/smoke-test.sh`, with hard assertions
(status codes + response bodies) instead of curl + sed UUID fallbacks.

Run the full cycle with:

```bash
./scripts/e2e-test.sh
```

The launcher:
1. Brings up the stack: `docker compose up -d --build` (idempotent — no-op if already up)
2. Waits until all 5 services report `/actuator/health` (bounded ~180s, 5s interval; one restart-and-recheck recovery pass handles cold-start config/discovery races)
3. Routing-readiness gate: waits for all 4 application services to register in Eureka, then probes the gateway routes until they resolve — 3 consecutive clean rounds, no 000/5xx, so the circuit breakers cannot flip to open right after the gate passes (bounded 120s)
4. Runs the suite: `./mvnw -pl e2e-tests test -DskipE2E=false`
5. Leaves the stack running so `./scripts/smoke-test.sh` still works after (tear down with `docker compose down`)

The module participates in the reactor but **never runs in the fast suite**:
surefire is gated by the `skipE2E` property (default `true`), so
`./scripts/test.sh test` compiles it and executes zero E2E tests. Suite only
(stack must already be up):

```bash
./mvnw -pl e2e-tests test -DskipE2E=false
```

Tests:

| Class | Asserts |
|-------|---------|
| `AuthE2E` | Keycloak password grant returns 200 + non-empty `access_token` |
| `HealthE2E` | `/actuator/health` returns 200 + `status: UP` for the 5 services |
| `IncidentFlowE2E` | create incident (unique title) → retrieve (OPEN) → appears in list → assign to first real user → transition to `IN_PROGRESS` → poll notifications until the assignee has ≥1 (60s bound, 2s interval) |
| `GatewaySecurityE2E` | role matrix: USER creates incidents (200/201) but cannot list them or read the user directory (403); AGENT lists incidents + user directory (200); notification owner-checks ignore spoofed `X-User-Id` (403); `/eureka/**` is no longer exposed through the gateway (401/404) |

Config surface (all optional; same defaults as `docker-compose.yml` and the
smoke test): `GATEWAY_URL`, `INCIDENT_URL`, `NOTIFICATION_URL`, `USER_URL`,
`DISCOVERY_URL`, `KEYCLOAK_URL`, `KEYCLOAK_REALM`, `KEYCLOAK_CLIENT`,
`E2E_USER`, `E2E_PASSWORD`.

## Per-Service Reference

### discovery-service

| Attribute | Value |
|-----------|-------|
| Port | 8761 |
| Package | `services/discovery-service` |
| Role | Eureka Service Registry — all services register here |
| Dependencies | None (standalone) |
| Test command | `./mvnw test -pl services/discovery-service` |
| Start command | `./mvnw spring-boot:run -pl services/discovery-service` |
| Health endpoint | http://localhost:8761/actuator/health |
| Dashboard | http://localhost:8761 |

**Key config** (`application.yaml`):
- `eureka.client.fetch-registry: false` — server mode
- `eureka.client.register-with-eureka: false` — does not self-register

---

### config-server

`services/config-server` is a **git submodule** pointing at
[`lautarorisso/config-server`](https://github.com/lautarorisso/config-server) —
a standalone Spring Boot project that owns its configuration. `docker compose`
builds it from this submodule via `docker/config-server.Dockerfile`.

| Attribute | Value |
|-----------|-------|
| Port | 8888 |
| Package | `services/config-server` (git submodule) |
| Role | Spring Cloud Config Server — serves centralized configuration to all services |
| Dependencies | `discovery-service` |
| Test command | `cd services/config-server && ./mvnw test` |
| Start command | `cd services/config-server && ./mvnw spring-boot:run` |
| Health endpoint | http://localhost:8888/actuator/health |

**Key config** (`application.properties`):
- `spring.profiles.active=native` — reads YAML files from filesystem
- `spring.cloud.config.server.native.search-locations=file:${CONFIG_DIR:./}` — config directory
- Config files live at the **root of the submodule** (`CONFIG_DIR=/app` in Docker)

**Config files** (served to other services):
| File | Service | Key settings |
|------|---------|-------------|
| `api-gateway.yaml` | api-gateway | Routes, JWT issuer URI, circuit breakers |
| `incident-service.yaml` | incident-service | PostgreSQL, RabbitMQ, Eureka |
| `notification-service.yaml` | notification-service | MongoDB, RabbitMQ, Eureka |
| `user-service.yaml` | user-service | PostgreSQL, Eureka |
| `discovery-service.yaml` | discovery-service | Eureka standalone config |

---

### api-gateway

| Attribute | Value |
|-----------|-------|
| Port | 8080 |
| Package | `services/api-gateway` |
| Role | Spring Cloud Gateway — routing, JWT validation (Keycloak `ims` realm), role-based authorization, rate limiting, circuit breakers |
| Dependencies | `discovery-service` |
| Test command | `./mvnw test -pl services/api-gateway` |
| Start command | `./mvnw spring-boot:run -pl services/api-gateway` |
| Health endpoint | http://localhost:8080/actuator/health |

**Routes** (via `application.yaml`):

| Route | Target | Circuit Breaker |
|-------|--------|-----------------|
| `/api/incidents/**` | `lb://incident-service` | `incident-service` |
| `/api/notifications/**` | `lb://notification-service` | `notification-service` |
| `/api/users/**` | `lb://user-service` | `user-service` |
| `/api/{svc}/v3/api-docs/**` | rewrites to `/{svc}/v3/api-docs/**` | — |

**Filters applied globally**:
- `CorrelationIdFilter` (order -100) — injects `X-Correlation-Id`
- `RequestLoggingFilter` (order -90) — logs method/path/status/duration
- `RateLimitFilter` (order -80) — token bucket per client IP

---

### incident-service

| Attribute | Value |
|-----------|-------|
| Port | 8081 |
| Package | `services/incident-service` |
| Role | Incident CRUD, state machine, outbox pattern, RabbitMQ events |
| Dependencies | `postgres`, `rabbitmq`, `discovery-service` |
| Test command | `./mvnw test -pl services/incident-service` |
| Start command | `./mvnw spring-boot:run -pl services/incident-service` |
| Health endpoint | http://localhost:8081/actuator/health |
| API docs | http://localhost:8081/scalar |

**Architecture**: Layered (controller → service → repository); the `Incident`
JPA entity is a **self-validating rich domain model** that owns its invariants
and mutation behavior (no public setters):
- `Incident.open(title, description, priority)` — blessed creation path (status `OPEN`)
- `assignTo(assigneeId, teamId)` — assignment
- `changeStatus(newStatus)` — lifecycle transition, validated against internal rules
- `controller/` — `IncidentController`, `GlobalExceptionHandler`
- `service/` — `IncidentService`, `OutboxPoller`
- `repository/` — Spring Data JPA repositories
- `entity/` — `Incident`, `OutboxEvent`
- `enums/` — `IncidentStatus`, `IncidentPriority`, `IncidentEvent`
- `messaging/` — `RabbitMqConfig`, `RabbitMqEventPublisher`
- `client/` — Feign client for the User Service

**Event flow**: mutations persist an `OutboxEvent` in the same transaction (transactional outbox) → `OutboxPoller` (every 5s) forwards unpublished events to RabbitMQ.

**State machine**: `OPEN → IN_PROGRESS → RESOLVED → CLOSED` (with `RESOLVED → OPEN` reopen). Enforced inside `Incident.changeStatus(...)` — invalid transitions throw `IllegalStateException`.

**Key config** (overridable via env vars):
| Env var | Default | Description |
|---------|---------|-------------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/incident_db` | PostgreSQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | `postgres` | DB username |
| `SPRING_DATASOURCE_PASSWORD` | `postgres` | DB password |
| `SPRING_RABBITMQ_HOST` | `localhost` | RabbitMQ host |
| `SPRING_RABBITMQ_PORT` | `5672` | RabbitMQ port |
| `SPRING_RABBITMQ_USERNAME` | `guest` | RabbitMQ username |
| `SPRING_RABBITMQ_PASSWORD` | `guest` | RabbitMQ password |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | `http://localhost:8761/eureka` | Eureka server URL |

**Database migrations**: Flyway (`V1__init_schema` … `V6__seed_data`) — schema, indexes, helper SQL functions/procedures, CHECK constraints + `updated_at` trigger, and demo seed data.

---

### notification-service

| Attribute | Value |
|-----------|-------|
| Port | 8083 |
| Package | `services/notification-service` |
| Role | Consume incident events from RabbitMQ, create and persist notifications |
| Dependencies | `mongo`, `rabbitmq`, `discovery-service` |
| Test command | `./mvnw test -pl services/notification-service` |
| Start command | `./mvnw spring-boot:run -pl services/notification-service` |
| Health endpoint | http://localhost:8083/actuator/health |
| API docs | http://localhost:8083/scalar |

**Architecture**: Layered (controller → service → repository)
- `controller/` — `NotificationController`, `GlobalExceptionHandler`
- `service/` — `NotificationRoutingService`
- `messaging/` — `IncidentEventListener` (@RabbitListener with idempotency), `RabbitMqConfig`
- `repository/` — Spring Data MongoDB repositories
- `entity/` — `Notification`, `ProcessedEvent`
- `enums/` — `NotificationStatus`, `NotificationType`
- `notifier/` — `EmailNotificationSender`

**Event flow**: `IncidentEventListener` consumes → dedupes via `ProcessedEvent` → resolves targets via `NotificationRoutingService` → persists `Notification` → sends via `NotificationSender`.

**Key config** (overridable via env vars):
| Env var | Default | Description |
|---------|---------|-------------|
| `SPRING_DATA_MONGODB_URI` | `mongodb://localhost:27017/notification_db` | MongoDB connection URI |
| `SPRING_RABBITMQ_HOST` | `localhost` | RabbitMQ host |
| `notification.email.enabled` | `true` | Set `false` to log notifications instead of sending email |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | `http://localhost:8761/eureka` | Eureka server URL |

---

### user-service

| Attribute | Value |
|-----------|-------|
| Port | 8082 |
| Package | `services/user-service` |
| Role | Read-only user profiles and teams |
| Dependencies | `postgres`, `discovery-service` |
| Test command | `./mvnw test -pl services/user-service` |
| Start command | `./mvnw spring-boot:run -pl services/user-service` |
| Health endpoint | http://localhost:8082/actuator/health |
| API docs | http://localhost:8082/scalar |

**Architecture**: Layered (controller → service → repository)
- `controller/` — `UserController`
- `service/` — `UserService`, `TeamService`
- `repository/` — Spring Data JPA repositories
- `entity/` — `User`, `Team`

**API**:
- `GET /api/users` (optional `teamId` filter), `GET /api/users/{id}`
- `GET /api/teams/{id}`

**Key config** (overridable via env vars):
| Env var | Default | Description |
|---------|---------|-------------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/user_db` | PostgreSQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | `postgres` | DB username |
| `SPRING_DATASOURCE_PASSWORD` | `postgres` | DB password |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | `http://localhost:8761/eureka` | Eureka server URL |

---

### shared

| Attribute | Value |
|-----------|-------|
| Package | `services/shared` |
| Role | Cross-service DTOs (`UserDto`, `TeamDto`) and shared infrastructure beans (`SharedRabbitMqConfig`, `OpenApiConfigFactory`) |

## Useful Endpoints

| Endpoint | Description |
|----------|-------------|
| http://localhost:8080 | API Gateway |
| http://localhost:8761 | Eureka Dashboard |
| http://localhost:8888 | Config Server Health |
| http://localhost:15672 | RabbitMQ Management UI |
| http://localhost:18080 | Keycloak Admin Console (realm `ims` → admin/admin) |
| http://localhost:8081/scalar | Incident Service API Docs |
| http://localhost:8083/scalar | Notification Service API Docs |
| http://localhost:8082/scalar | User Service API Docs |
| http://localhost:8080/scalar | Aggregated OpenAPI (via Gateway) |

## Infrastructure

| Component | Version | Internal Host | Port(s) | UI |
|-----------|---------|---------------|---------|-----|
| PostgreSQL | 16 | `postgres` | 5432 | — |
| MongoDB | 7 | `mongo` | 27017 | — |
| RabbitMQ | 3-management | `rabbitmq` | 5672, 15672 | http://localhost:15672 (guest/guest) |
| Keycloak | 26 | `keycloak` | 18080 | http://localhost:18080 (admin/admin) |

## Environment Variables

`docker compose up` works out of the box with built-in defaults (see `docker-compose.yml`). To override, create a `.env` file in the project root:

```bash
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
RABBITMQ_USER=guest
RABBITMQ_PASS=guest
```

## Configuration

Services use `spring.config.import: "optional:configserver:"` with Eureka discovery to pull configuration from the Config Server. When the Config Server is available (Docker Compose), services receive centralized config from the YAML files at the root of the `services/config-server` submodule.

Environment variables in `docker-compose.yml` override Config Server values (Spring precedence: env var > config server > local `application.yaml`).

## Project Structure

```
incident-management-system/
├── docker-compose.yml          # Full local stack (clone & run)
├── Dockerfile                  # Multi-stage build for domain services
├── docker/
│   └── config-server.Dockerfile# Config Server build (from submodule)
├── .dockerignore               # Keeps build context lean
├── .gitmodules                 # Config Server submodule pointer
├── pom.xml                     # Parent Maven POM (multi-module)
├── mvnw                        # Maven wrapper
├── e2e-tests/                  # Black-box E2E suite (REST Assured, gated by skipE2E)
├── scripts/
│   ├── e2e-test.sh             # E2E runner: stack up + health gates + suite
│   ├── init-db.sql             # Database initialization
│   └── smoke-test.sh           # End-to-end smoke test
└── services/
    ├── config-server/          # git submodule → Spring Cloud Config Server
    ├── api-gateway/            # Spring Cloud Gateway
    ├── discovery-service/      # Eureka Service Registry
    ├── incident-service/       # Incident domain (layered)
    ├── notification-service/   # Notification processing (layered)
    ├── shared/                 # Cross-service DTOs and shared config
    └── user-service/           # User profiles (layered)
```
