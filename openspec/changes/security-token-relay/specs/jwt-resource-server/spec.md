# JWT Resource Server Specification

## Purpose

Each business service (incident-service, notification-service, user-service) validates JWT access tokens locally using Spring Security's OAuth2 Resource Server support. The shared library provides a reusable `SecurityFilterChain` factory that services import explicitly.

## Requirements

### Requirement: Local JWT Validation

Each business service MUST validate incoming requests using JWT access tokens issued by Keycloak. The system SHALL reject requests without a valid `Authorization: Bearer <token>` header with HTTP 401.

#### Scenario: Valid token grants access

- GIVEN a request with a valid JWT issued by the configured issuer
- WHEN the request reaches the service
- THEN the request proceeds to the controller
- AND the `Authentication` principal contains the JWT claims

#### Scenario: Missing Authorization header returns 401

- GIVEN a request without an `Authorization` header
- WHEN the request reaches the service
- THEN the response status MUST be 401
- AND the `WWW-Authenticate` header MUST indicate Bearer token required

#### Scenario: Expired token returns 401

- GIVEN a request with an expired JWT
- WHEN the request reaches the service
- THEN the response status MUST be 401

#### Scenario: Invalid signature returns 401

- GIVEN a request with a JWT signed by an unknown key
- WHEN the request reaches the service
- THEN the response status MUST be 401

#### Scenario: Token from wrong issuer returns 401

- GIVEN a request with a JWT issued by a different realm
- WHEN the request reaches the service
- THEN the response status MUST be 401

### Requirement: Explicit Shared Library Import

Services in `com.lautarorisso.*` MUST explicitly import the shared security configuration from `com.ims.shared.security` because the package is not auto-scanned. The system SHALL fail to start if the import is missing.

#### Scenario: Service with @Import starts successfully

- GIVEN a service declares `@Import(SharedSecurityConfiguration.class)`
- AND the shared library is on the classpath
- WHEN the application context initializes
- THEN the `SecurityFilterChain` beans are registered
- AND the service accepts authenticated requests

#### Scenario: Service without import lacks JWT validation

- GIVEN a service omits `@Import(SharedSecurityConfiguration.class)`
- WHEN the application context initializes
- THEN no `SecurityFilterChain` for OAuth2 resource server is registered
- AND the service accepts unauthenticated requests (configuration error)

### Requirement: Issuer Configuration via Config Server

Each service MUST read the JWT issuer URI from Spring Cloud Config at `spring.security.oauth2.resourceserver.jwt.issuer-uri`. The system SHALL use the Keycloak realm URL (e.g., `http://keycloak:8080/realms/ims`).

#### Scenario: Issuer URI loaded from config server

- GIVEN config-server provides `issuer-uri` for the service
- WHEN the service starts
- THEN the `JwtDecoder` is configured with the correct issuer
- AND tokens from that issuer are accepted

#### Scenario: Missing issuer URI fails startup

- GIVEN config-server does not provide `issuer-uri`
- WHEN the service starts
- THEN the application context fails to initialize
- AND the error indicates missing issuer configuration

### Requirement: Public Endpoints Remain Accessible

Actuator health endpoints and API documentation endpoints MUST remain accessible without authentication. The system SHALL permit all requests to `/actuator/health`, `/actuator/info`, `/v3/api-docs/**`, `/scalar/**`.

#### Scenario: Health endpoint accessible without token

- GIVEN a request to `GET /actuator/health`
- WHEN the request reaches the service
- THEN the response status MUST be 200
- AND no `Authorization` header is required

#### Scenario: API docs accessible without token

- GIVEN a request to `GET /v3/api-docs`
- WHEN the request reaches the service
- THEN the response status MUST be 200
- AND no `Authorization` header is required

#### Scenario: Scalar UI accessible without token

- GIVEN a request to `GET /scalar/**`
- WHEN the request reaches the service
- THEN the response status MUST be 200
- AND no `Authorization` header is required

### Requirement: All Other Endpoints Require Authentication

Any endpoint not explicitly public MUST require a valid JWT. The system SHALL return 401 for unauthenticated requests to business endpoints.

#### Scenario: Business endpoint without token returns 401

- GIVEN a request to `GET /api/incidents` without token
- WHEN the request reaches the service
- THEN the response status MUST be 401

#### Scenario: Business endpoint with valid token proceeds

- GIVEN a request to `GET /api/incidents` with valid JWT
- WHEN the request reaches the service
- THEN the response status MUST be 200
- AND the user identity is derived from `sub` claim