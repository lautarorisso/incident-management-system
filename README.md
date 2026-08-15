# Incident Management System

Microservices-based incident management system built with Spring Boot 3.5, Spring Cloud 2025, and Java 21.

> **Note**: This is a work-in-progress MVP for portfolio demonstration. JWT validation via Keycloak is planned but not yet implemented. The system runs with a simplified local stack (PostgreSQL, RabbitMQ, Eureka, API Gateway + 3 domain services).

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

All domain services use a **layered architecture** (controller → service → repository), where the JPA entity serves as both the persistence and domain model. Cross-service DTOs and shared infrastructure beans live in the `services/shared` module.

## Services

| Service | Port | Description |
|---------|------|-------------|
| api-gateway | 8080 | Routing, rate limiting, circuit breakers, request logging (JWT validation pending Keycloak) |
| discovery-service | 8761 | Eureka Service Discovery Server |
| incident-service | 8081 | Incident CRUD, state machine, outbox pattern, RabbitMQ events |
| notification-service | 8083 | Consumes incident events from RabbitMQ, persists and delivers notifications |
| user-service | 8082 | Read-only user profiles and teams |

## Infrastructure

| Component | Port | UI |
|-----------|------|----|
| PostgreSQL | 5432 | — |
| RabbitMQ | 5672 | http://localhost:15672 |

## Prerequisites

- Java 21
- Docker & Docker Compose (for infrastructure: PostgreSQL, RabbitMQ)

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
| `/api/{svc}/v3/api-docs/**` | rewrites to `/{svc}/v3/api-docs/**` | — |

**Filters applied globally**:
- `CorrelationIdFilter` (order -100) — injects `X-Correlation-Id`
- `RequestLoggingFilter` (order -90) — logs method/path/status/duration
- `RateLimitFilter` (order -80) — token bucket per client IP
- `UserIdHeaderFilter` (order -60) — JWT `sub` → `X-User-Id` (pending Keycloak)

> **Note**: JWT validation is disabled until Keycloak is deployed. The gateway currently permits all requests for local development.

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

**Architecture**: Layered (controller → service → repository)
- `controller/` — `IncidentController`, `GlobalExceptionHandler`
- `service/` — `IncidentService`, `IncidentStateMachine`, `OutboxPoller`
- `repository/` — Spring Data JPA repositories
- `entity/` — `Incident`, `OutboxEvent`, enums
- `messaging/` — `RabbitMqConfig`, `RabbitMqEventPublisher`
- `client/` — Feign client for the User Service

**Event flow**: mutations persist an `OutboxEvent` in the same transaction (transactional outbox) → `OutboxPoller` (every 5s) forwards unpublished events to RabbitMQ.

**State machine**: `OPEN → IN_PROGRESS → RESOLVED → CLOSED` (with `RESOLVED → OPEN` reopen). Enforced in `IncidentStateMachine`.

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
| Dependencies | `postgres`, `rabbitmq`, `discovery-service` |
| Test command | `./mvnw test -pl services/notification-service` |
| Start command | `./mvnw spring-boot:run -pl services/notification-service` |
| Health endpoint | http://localhost:8083/actuator/health |
| API docs | http://localhost:8083/scalar |

**Architecture**: Layered (controller → service → repository)
- `controller/` — `NotificationController`, `GlobalExceptionHandler`
- `service/` — `NotificationRoutingService`
- `messaging/` — `IncidentEventListener` (@RabbitListener with idempotency), `RabbitMqConfig`
- `repository/` — Spring Data JPA repositories
- `entity/` — `Notification`, `ProcessedEvent`, enums
- `notifier/` — `EmailNotificationSender`

**Event flow**: `IncidentEventListener` consumes → dedupes via `ProcessedEvent` → resolves targets via `NotificationRoutingService` → persists `Notification` → sends via `NotificationSender`.

**Key config** (overridable via env vars):
| Env var | Default | Description |
|---------|---------|-------------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/notification_db` | PostgreSQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | `postgres` | DB username |
| `SPRING_DATASOURCE_PASSWORD` | `postgres` | DB password |
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

## Environment Variables

Copy `.env` from the project root to configure defaults:

```bash
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
RABBITMQ_USER=guest
RABBITMQ_PASS=guest
```

For deployments against the homelab infrastructure, also set `HOMELAB_IP` (see `docker-compose.homelab.yml`).

## Configuration

Each service has a local `application.yaml` with default settings suitable for local development (localhost URLs, embedded H2 in tests). Services use `spring.config.import: "optional:configserver:http://homelab:8888"`, so they start fine without a Config Server and pick up remote config when it is available.

For Docker Compose deployments, environment variables in `docker-compose.yml` override the local defaults (e.g., `SPRING_DATASOURCE_URL` points to the `postgres` hostname).

## Project Structure

```
incident-management-system/
├── docker-compose.yml          # Full local stack orchestration
├── docker-compose.homelab.yml  # Deployment against homelab infra
├── Dockerfile                  # Multi-stage build for all services (SERVICE arg)
├── pom.xml                     # Parent Maven POM (multi-module)
├── mvnw                        # Maven wrapper
├── .env                        # Environment variable defaults
├── scripts/
│   ├── init-db.sql             # Database initialization
│   └── smoke-test.sh           # End-to-end smoke test
└── services/
    ├── api-gateway/            # Spring Cloud Gateway
    ├── discovery-service/      # Eureka Service Registry
    ├── incident-service/       # Incident domain (layered)
    ├── notification-service/   # Notification processing (layered)
    ├── shared/                 # Cross-service DTOs and shared config
    └── user-service/           # User profiles (layered)
```
