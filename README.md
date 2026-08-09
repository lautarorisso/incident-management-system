# Incident Management System

Microservices-based incident management system built with Spring Boot 3.5, Spring Cloud 2025, and Java 21.

> **Note**: This is a work-in-progress MVP for portfolio demonstration. Keycloak and RabbitMQ integrations are planned but not yet implemented. The system runs with a simplified local stack (PostgreSQL, Eureka, API Gateway + 3 domain services).

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
   │PostgreSQL│     │PostgreSQL│    │PostgreSQL│
   │incident_db│   │notific._db│   │ user_db  │
   └─────────┘     └─────────┘    └─────────┘
                        │
                   ┌────▼────┐
                   │RabbitMQ │
                   └─────────┘

┌─────────────────────────────────────────────┐
│ discovery-service (Eureka) :8761            │
│ PostgreSQL :5432  |  RabbitMQ :5672/15672   │
└─────────────────────────────────────────────┘
```

## Services

| Service | Port | Description |
|---------|------|-------------|
| api-gateway | 8080 | Routing, rate limiting, circuit breakers (JWT validation pending Keycloak) |
| discovery-service | 8761 | Eureka Service Discovery Server |
| incident-service | 8081 | Incident CRUD, state machine, events, outbox pattern |
| notification-service | 8083 | Async notification consumption and persistence |
| user-service | 8082 | User profiles, teams, departments (Keycloak sync pending) |

## Infrastructure

| Component | Port | UI |
|-----------|------|----|
| PostgreSQL | 5432 | — |
| RabbitMQ | 5672 | http://localhost:15672 |
| Keycloak | *planned* | *not yet deployed* |

## Prerequisites

- Java 21
- Docker & Docker Compose (for infrastructure: PostgreSQL, RabbitMQ, Keycloak)
- The project includes a Maven Wrapper (`./mvnw`) — no manual Maven installation needed

## Quick Start

### 1. Start infrastructure

```bash
docker-compose up -d postgres rabbitmq
```

### 2. Build the project

```bash
./mvnw clean install -DskipTests
```

### 3. Start services (in order)

Open separate terminals for each service. Start them in this order:

```bash
# Discovery first — all other services register here
./mvnw spring-boot:run -pl services/discovery-service

# Gateway
./mvnw spring-boot:run -pl services/api-gateway

# Domain services (order independent)
./mvnw spring-boot:run -pl services/incident-service
./mvnw spring-boot:run -pl services/notification-service
./mvnw spring-boot:run -pl services/user-service
```

### Or with Docker (full stack)

```bash
docker-compose up -d --build
```

This builds and starts all 7 containers (2 infra + 5 services).

## Running Tests

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

Run a specific test class:

```bash
./mvnw test -pl services/incident-service -Dtest=IncidentControllerTest
```

## Smoke Test

After starting all services, run the end-to-end smoke test:

```bash
./scripts/smoke-test.sh
```

The smoke test validates:
- Health checks for all 5 services
- Create an incident (via API Gateway)
- Retrieve and list incidents
- Verify notification was created
- Check user service availability

To test against Docker deployments:

```bash
GATEWAY_URL=http://localhost:8080 ./scripts/smoke-test.sh
```

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

### api-gateway

| Attribute | Value |
|-----------|-------|
| Port | 8080 |
| Package | `services/api-gateway` |
| Role | Spring Cloud Gateway — routing, rate limiting, circuit breakers (JWT validation pending Keycloak) |
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

**Filters applied globally**:
- `CorrelationIdFilter` (order -100) — injects `X-Correlation-Id`
- `RequestLoggingFilter` (order -90) — logs method/path/status/duration
- `RateLimitFilter` (order -80) — token bucket per client IP
- `UserIdHeaderFilter` (order -60) — JWT `sub` → `X-User-Id` (pending Keycloak)

**Key config** (overridable via env vars):
| Env var | Default | Description |
|---------|---------|-------------|
| `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI` | *unset (pending Keycloak)* | Keycloak realm issuer URL |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | `http://localhost:8761/eureka` | Eureka server URL |
| `DISCOVERY_HOST` | `localhost` | Eureka host for service discovery |

> **Note**: JWT validation is disabled until Keycloak is deployed. The gateway currently permits all requests for local development.

---

### incident-service

| Attribute | Value |
|-----------|-------|
| Port | 8081 |
| Package | `services/incident-service` |
| Role | Incident CRUD, state machine, domain events, outbox pattern |
| Dependencies | `postgres`, `rabbitmq`, `discovery-service` |
| Test command | `./mvnw test -pl services/incident-service` |
| Start command | `./mvnw spring-boot:run -pl services/incident-service` |
| Health endpoint | http://localhost:8081/actuator/health |
| API docs | http://localhost:8081/scalar |

**Architecture**: Hexagonal (ports & adapters)
- `domain/` — `Incident`, `IncidentId`, `IncidentStatus`, `IncidentPriority`, `IncidentDomainService`
- `domain/port/out/` — `IncidentRepository`, `IncidentEventPublisher` (outbox writer), `EventPublisher` (broker forwarder)
- `adapter/out/persistence/` — JPA entities, `IncidentPersistenceAdapter`
- `adapter/out/messaging/` — `RabbitMqEventPublisher` (implements `EventPublisher`), `OutboxPoller`
- `adapter/out/feign/` — `UserServiceClient` (Feign + CircuitBreaker)
- `adapter/in/rest/` — REST controller, DTOs, MapStruct mappers

**Use cases** (application service layer):
- `CreateIncidentUseCase` — validate → domain service → persist → publish event
- `AssignIncidentUseCase` — validate user/team via Feign → domain rules → persist → event
- `TransitionIncidentUseCase` — state machine transition (OPEN→IN_PROGRESS→RESOLVED→CLOSED)
- `GetIncidentUseCase`, `ListIncidentsUseCase` — read-only queries

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

**Test breakdown** (full suite):
| Layer | Test class | Count |
|-------|-----------|-------|
| Domain | `IncidentDomainTest` | 17 |
| Domain | `IncidentPriorityTest` | 5 |
| Persistence | `IncidentPersistenceAdapterTest` | 10 |
| Messaging | `RabbitMqEventPublisherTest` | 5 |
| Messaging | `OutboxPollerTest` | 6 |
| Feign | `UserServiceClientWireMockTest` | 6 |
| REST | `IncidentControllerTest` | 10 |
| Integration | `IncidentUseCaseIntegrationTest` | 6 |
| Context | `IncidentServiceApplicationTests` | 1 |

---

### notification-service

| Attribute | Value |
|-----------|-------|
| Port | 8083 |
| Package | `services/notification-service` |
| Role | Consume incident events from RabbitMQ, create and persist notifications |
| Dependencies | `postgres`, `rabbitmq`, `discovery-service` |
| Test command | `./mvnw test -pl services/notification-service` |
| Start command | `./mvnw spring-boot:run -pl services/notification-service` |
| Health endpoint | http://localhost:8083/actuator/health |
| API docs | http://localhost:8083/scalar |

**Architecture**: Hexagonal (ports & adapters)
- `domain/` — `Notification`, `NotificationId`, `NotificationType`, `NotificationStatus`, `ProcessedEvent`
- `domain/port/out/` — `NotificationRepository`, `ProcessedEventRepository`, `NotificationSender`
- `domain/service/` — `NotificationRoutingService` — resolve target userIds from events
- `adapter/out/persistence/` — JPA entities, persistence adapters
- `adapter/out/messaging/` — `IncidentEventListener` (@RabbitListener with idempotency)
- `adapter/in/rest/` — `NotificationController`, DTOs

**Event flow**: `IncidentEventListener` consumes → checks `ProcessedEvent` (idempotency) → resolves targets via `NotificationRoutingService` → persists `Notification` via `NotificationPersistenceAdapter`

**Key config** (overridable via env vars):
| Env var | Default | Description |
|---------|---------|-------------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/notification_db` | PostgreSQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | `postgres` | DB username |
| `SPRING_DATASOURCE_PASSWORD` | `postgres` | DB password |
| `SPRING_RABBITMQ_HOST` | `localhost` | RabbitMQ host |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | `http://localhost:8761/eureka` | Eureka server URL |

**Test breakdown** (full suite):
| Layer | Test class | Count |
|-------|-----------|-------|
| Domain | `NotificationDomainTest` | 11 |
| Domain | `NotificationRoutingServiceTest` | 10 |
| Persistence | `NotificationPersistenceAdapterTest` | 8 |
| Messaging | `IncidentEventListenerTest` | 5 |
| REST | `NotificationControllerTest` | 6 |
| Integration | `NotificationServiceIntegrationTest` | 3 |
| Context | `NotificationServiceApplicationTests` | 1 |

---

### user-service

| Attribute | Value |
|-----------|-------|
| Port | 8082 |
| Package | `services/user-service` |
| Role | User profiles, teams, departments — synced from Keycloak (sync pending Keycloak deployment) |
| Dependencies | `postgres`, `discovery-service`, `keycloak` (planned) |
| Test command | `./mvnw test -pl services/user-service` |
| Start command | `./mvnw spring-boot:run -pl services/user-service` |
| Health endpoint | http://localhost:8082/actuator/health |
| API docs | http://localhost:8082/scalar |

**Architecture**: Hexagonal (ports & adapters)
- `domain/` — `User`, `UserId`, `Team`, `TeamId`
- `domain/port/out/` — `UserRepository`, `TeamRepository`, `KeycloakAdminClient`
- `adapter/out/keycloak/` — `KeycloakAdminClientImpl` (syncs users, groups)
- `adapter/out/persistence/` — JPA entities, `UserPersistenceAdapter`, `TeamPersistenceAdapter`
- `adapter/out/sync/` — `KeycloakSyncScheduler` (@PostConstruct initial sync + @Scheduled every 5min)
- `adapter/in/rest/` — `UserController`, DTOs

> **Note**: Keycloak sync is implemented but requires a running Keycloak instance. Currently the service starts with a placeholder Keycloak URL; the sync will fail gracefully until Keycloak is deployed.

**Key config** (overridable via env vars):
| Env var | Default | Description |
|---------|---------|-------------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/user_db` | PostgreSQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | `postgres` | DB username |
| `SPRING_DATASOURCE_PASSWORD` | `postgres` | DB password |
| `KEYCLOAK_ADMIN_SERVER_URL` | `http://localhost:18080` | Keycloak server URL (placeholder — Keycloak not deployed) |
| `KEYCLOAK_CLIENT_SECRET` | `changeme` | Keycloak admin client secret |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | `http://localhost:8761/eureka` | Eureka server URL |

**Test breakdown** (full suite):
| Layer | Test class | Count |
|-------|-----------|-------|
| Domain | `UserDomainTest` | 4 |
| Keycloak | `KeycloakConfigTest` | 3 |
| Keycloak | `KeycloakAdminClientImplTest` | 6 |
| Keycloak | `KeycloakUserMapperTest` | 3 |
| Keycloak | `KeycloakGroupMapperTest` | 2 |
| Persistence | `UserPersistenceAdapterTest` | 8 |
| Persistence | `TeamPersistenceAdapterTest` | 6 |
| Persistence | `UserRestMapperTest` | 3 |
| REST | `UserControllerTest` | 6 |
| Sync | `KeycloakSyncSchedulerTest` | 6 |
| Context | `UserServiceApplicationTests` | 1 |

## Useful Endpoints

| Endpoint | Description |
|----------|-------------|
| http://localhost:8080 | API Gateway |
| http://localhost:8761 | Eureka Dashboard |
| http://localhost:15672 | RabbitMQ Management UI |
| http://localhost:8081/scalar | Incident Service API Docs |
| http://localhost:8083/scalar | Notification Service API Docs |
| http://localhost:8082/scalar | User Service API Docs |
| http://localhost:8080/scalar | Aggregated OpenAPI (via Gateway) |

## Infrastructure

| Component | Version | Internal Host | Port(s) | UI |
|-----------|---------|---------------|---------|-----|
| PostgreSQL | 16 | `postgres` | 5432 | — |
| RabbitMQ | 3-management | `rabbitmq` | 5672, 15672 | http://localhost:15672 |
| Keycloak | 25 | *planned* | *8081 (planned)* | *http://localhost:8081 (planned)* |

## Environment Variables

Copy `.env` from the project root to configure defaults:

```bash
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
RABBITMQ_USER=guest
RABBITMQ_PASS=guest
# KEYCLOAK_ADMIN=admin
# KEYCLOAK_ADMIN_PASSWORD=admin
```

> **Note**: Keycloak variables are commented out — Keycloak is not deployed in the current MVP stack. Uncomment when Keycloak is added.

## Configuration

Each service has a local `application.yaml` with default settings suitable for local development (localhost URLs, embedded H2 in tests). **Config Server integration is present but commented out** — services use `optional:configserver:` so they can start without the Config Server during local development. Uncomment the `spring.config.import` blocks when Config Server is deployed.

For Docker Compose deployments, environment variables in `docker-compose.yml` override the local defaults (e.g., `SPRING_DATASOURCE_URL` points to `postgres` hostname).

## Project Structure

```
incident-management-system/
├── docker-compose.yml          # Full stack orchestration
├── pom.xml                     # Parent Maven POM (multi-module)
├── mvnw                        # Maven wrapper
├── .env                        # Environment variable defaults
├── scripts/
│   ├── init-db.sql             # Database initialization
│   └── smoke-test.sh           # End-to-end smoke test
└── services/
    ├── api-gateway/            # Spring Cloud Gateway
    ├── discovery-service/      # Eureka Service Registry
    ├── incident-service/       # Incident domain (hexagonal)
    ├── notification-service/   # Notification processing (hexagonal)
    └── user-service/           # User profiles (hexagonal)
```
