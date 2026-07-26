# Incident Management System

Microservices-based incident management system built with Spring Boot 3.5, Spring Cloud 2025, and Java 21.

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
│ :8082          │ │ :8083        │ │ :8084        │
└───────┬────────┘ └──────┬───────┘ └──────┬───────┘
        │                 │                │
   ┌────▼────┐      ┌────▼────┐     ┌────▼────┐
   │PostgreSQL│     │PostgreSQL│    │PostgreSQL│
   │incident_db│   │notific._db│   │ user_db  │
   └─────────┘     └────┬─────┘    └─────────┘
                        │
                   ┌────▼────┐
                   │RabbitMQ │
                   └─────────┘
```

## Services

| Service | Port | Description |
|---------|------|-------------|
| api-gateway | 8080 | Routing, JWT validation, rate limiting |
| discovery-service | 8761 | Eureka Service Discovery Server |
| incident-service | 8082 | Incident CRUD, state machine, events |
| notification-service | 8083 | Async notification consumption and persistence |
| user-service | 8084 | User profiles, teams, departments |

## Infrastructure

| Component | Port | UI |
|-----------|------|----|
| PostgreSQL | 5432 | — |
| RabbitMQ | 5672 | http://localhost:15672 |
| Keycloak | 8081 | http://localhost:8081 |

## Prerequisites

- Java 21
- Docker & Docker Compose
- Maven 3.9+ (or use the included wrapper `./mvnw`)

## Quick Start

### 1. Start infrastructure

```bash
docker-compose up -d postgres rabbitmq keycloak
```

### 2. Build the project

```bash
./mvnw clean install -DskipTests
```

### 3. Start services (in order)

```bash
# Discovery first — all other services register here
./mvnw spring-boot:run -pl services/discovery-service

# Gateway
./mvnw spring-boot:run -pl services/api-gateway

# Domain services
./mvnw spring-boot:run -pl services/incident-service
./mvnw spring-boot:run -pl services/notification-service
./mvnw spring-boot:run -pl services/user-service
```

### Or with Docker

```bash
docker-compose up -d --build
```

## Useful Endpoints

| Endpoint | Description |
|----------|-------------|
| http://localhost:8080 | API Gateway |
| http://localhost:8761 | Eureka Dashboard |
| http://localhost:8081 | Keycloak Admin Console |
| http://localhost:15672 | RabbitMQ Management UI |
| http://localhost:8082/swagger-ui.html | Incident Service API Docs |
| http://localhost:8084/swagger-ui.html | User Service API Docs |

## Configuration

Each service has a local `application.yaml` with default settings. When the Config Server is running, it overrides these values centrally. Services use `optional:configserver:` so they can start without the Config Server during local development.
