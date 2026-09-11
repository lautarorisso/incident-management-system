# Feign Token Relay Specification

## Purpose

The Feign client in incident-service (`UserServiceClient`) MUST propagate the incoming `Authorization` header to downstream calls to user-service. This ensures the JWT validated at the gateway is forwarded through the service mesh.

## Requirements

### Requirement: Authorization Header Forwarding

The `UserServiceClient` Feign client MUST forward the `Authorization` header from the current HTTP request to all outgoing calls. The system SHALL read the header from `RequestContextHolder` and add it to the Feign request.

#### Scenario: Valid token relayed to user-service

- GIVEN an authenticated request to incident-service with `Authorization: Bearer <token>`
- WHEN incident-service calls `UserServiceClient.findUserById(...)`
- THEN the outgoing Feign request includes `Authorization: Bearer <token>`
- AND user-service receives and validates the same token

#### Scenario: No Authorization header results in no header forwarded

- GIVEN a request to incident-service without `Authorization` header (e.g., scheduler, WireMock test)
- WHEN incident-service calls `UserServiceClient.findUserById(...)`
- THEN the outgoing Feign request MUST NOT include an `Authorization` header
- AND the call proceeds without authentication (for internal/non-HTTP contexts)

#### Scenario: Expired token relayed and rejected downstream

- GIVEN an authenticated request with an expired JWT
- WHEN incident-service calls `UserServiceClient.findUserById(...)`
- THEN the outgoing request includes the expired token
- AND user-service returns 401
- AND incident-service propagates the 401 to the original caller (not 503)

#### Scenario: Malformed token relayed as-is

- GIVEN an authenticated request with a malformed JWT
- WHEN incident-service calls `UserServiceClient.findUserById(...)`
- THEN the outgoing request includes the malformed token
- AND user-service returns 401
- AND incident-service propagates the 401

### Requirement: Interceptor Registered Only Where Needed

The `FeignTokenRelayInterceptor` MUST be registered only on Feign clients that call downstream services requiring JWT validation. The system SHALL NOT add the interceptor to clients calling external systems that don't accept JWT.

#### Scenario: Interceptor attached to UserServiceClient

- GIVEN incident-service declares `UserServiceClient` with `configuration = FeignTokenRelayConfig.class`
- WHEN the Feign client is created
- THEN the `FeignTokenRelayInterceptor` is in the interceptor chain
- AND only `UserServiceClient` has this interceptor (other clients unaffected)

### Requirement: Thread-Local Context Safety

The interceptor MUST safely read from `RequestContextHolder` only when a request context exists. The system SHALL return no-op when called outside a servlet request thread (e.g., `@Scheduled`, test contexts).

#### Scenario: Interceptor no-op in scheduled context

- GIVEN a `@Scheduled` method calls `UserServiceClient`
- WHEN the Feign request is built
- THEN `RequestContextHolder.getRequestAttributes()` returns null
- AND no `Authorization` header is added
- AND no exception is thrown