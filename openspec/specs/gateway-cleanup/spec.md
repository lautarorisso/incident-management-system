# Gateway Cleanup Specification

## Purpose

Remove the public `/eureka/**` route from the API gateway and delete the `UserIdHeaderFilter` that injected `X-User-Id` headers. Services will no longer consume `X-User-Id` for authorization — identity is derived from the validated JWT `sub` claim.

## Requirements

### Requirement: Eureka Dashboard Not Publicly Accessible

The `/eureka/**` route MUST NOT be publicly accessible via the gateway. The system SHALL require authentication (valid JWT) for all Eureka dashboard routes, or remove the route entirely from gateway configuration.

#### Scenario: Unauthenticated request to /eureka returns 401

- GIVEN a request to `GET /eureka/` without `Authorization` header
- WHEN the request reaches the gateway
- THEN the response status MUST be 401

#### Scenario: Authenticated request to /eureka allowed (if route retained)

- GIVEN a request to `GET /eureka/` with valid JWT
- WHEN the request reaches the gateway
- THEN the response status MUST be 200 (if route retained with auth)
- OR the route returns 404 (if route removed entirely)

### Requirement: UserIdHeaderFilter Removed

The `UserIdHeaderFilter` (WebFilter, order -60) MUST be deleted from the gateway. The system SHALL NOT inject `X-User-Id` headers into downstream requests.

#### Scenario: Downstream services receive no X-User-Id from gateway

- GIVEN an authenticated request to any downstream service via gateway
- WHEN the request is forwarded
- THEN the downstream request MUST NOT contain `X-User-Id` header
- AND the `Authorization` header is forwarded unchanged (gateway default behavior)

### Requirement: Services No Longer Consume X-User-Id

Incident-service, notification-service, and user-service MUST NOT read or rely on `X-User-Id` header for authorization or identity. The system SHALL derive user identity exclusively from the JWT `sub` claim after validation.

#### Scenario: Service derives identity from JWT sub claim

- GIVEN a request with valid JWT containing `sub = "user-123"`
- WHEN the service processes the request
- THEN the authenticated principal's name MUST be "user-123"
- AND no code reads `X-User-Id` header

#### Scenario: X-User-Id header ignored if present

- GIVEN a request with valid JWT (`sub = "user-123"`) AND `X-User-Id: user-999`
- WHEN the service processes the request
- THEN the identity used MUST be "user-123" (from JWT)
- AND `X-User-Id` header MUST NOT affect authorization decisions

### Requirement: Public Routes Limited to Monitoring and Docs

The gateway's public (permitAll) routes MUST be limited to actuator health, API documentation, and Scalar UI. No business endpoints or Eureka dashboard are public.

#### Scenario: Permitted public routes

- GIVEN requests to `/actuator/health`, `/actuator/info`, `/v3/api-docs/**`, `/scalar/**`
- WHEN unauthenticated
- THEN response status MUST be 200

#### Scenario: All other routes require authentication

- GIVEN requests to `/api/**`, `/eureka/**`, `/actuator/**` (except health/info)
- WHEN unauthenticated
- THEN response status MUST be 401

### Requirement: Feign Hop Already Forwards Authorization

The gateway already forwards `Authorization` headers downstream by default (no stripping filter exists). The cleanup MUST NOT add any filter that removes or blocks the `Authorization` header.

#### Scenario: Authorization header passes through gateway

- GIVEN a request with `Authorization: Bearer <token>`
- WHEN the gateway forwards to a business service
- THEN the downstream request includes `Authorization: Bearer <token>`
- AND no gateway filter removes or modifies it