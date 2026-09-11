# Notification Owner Check Specification

## Purpose

Enforce authorization on `GET /api/notifications/{userId}`: a caller may only access notifications for their own user ID (`sub` claim == `userId` path variable) or if they hold `ROLE_ADMIN`. All other roles (including `ROLE_AGENT`, `ROLE_USER`) receive 403 when accessing another user's notifications.

## Requirements

### Requirement: Owner or Admin Access Only

The notification endpoint MUST authorize access by comparing the JWT `sub` claim to the `userId` path variable. The system SHALL grant access if `sub == userId` OR the caller has `ROLE_ADMIN`. All other cases return 403.

#### Scenario: Owner accesses own notifications

- GIVEN a JWT with `sub = "user-123"` and `ROLE_USER`
- WHEN requesting `GET /api/notifications/user-123`
- THEN the response status MUST be 200
- AND the notifications for user-123 are returned

#### Scenario: Admin accesses any user's notifications

- GIVEN a JWT with `sub = "admin-001"` and `ROLE_ADMIN`
- WHEN requesting `GET /api/notifications/user-123`
- THEN the response status MUST be 200
- AND the notifications for user-123 are returned

#### Scenario: Agent accessing another user's notifications returns 403

- GIVEN a JWT with `sub = "agent-001"` and `ROLE_AGENT`
- WHEN requesting `GET /api/notifications/user-123`
- THEN the response status MUST be 403
- AND the response body indicates insufficient permissions

#### Scenario: User accessing another user's notifications returns 403

- GIVEN a JWT with `sub = "user-456"` and `ROLE_USER`
- WHEN requesting `GET /api/notifications/user-123`
- THEN the response status MUST be 403

#### Scenario: User with multiple roles including ADMIN accesses any

- GIVEN a JWT with `sub = "user-999"` and roles `ROLE_USER`, `ROLE_ADMIN`
- WHEN requesting `GET /api/notifications/user-123`
- THEN the response status MUST be 200

#### Scenario: Missing userId parameter (if query-based) handled

- GIVEN a JWT with `sub = "user-123"` and `ROLE_USER`
- WHEN requesting `GET /api/notifications` without userId
- THEN the response status MUST be 400 (bad request) or 403
- AND the behavior is consistent with API design

### Requirement: Owner Check Applied Only to Notification Endpoint

The owner-check authorization rule MUST apply only to notification endpoints. Incident and user service endpoints retain their role-based authorization model (no owner-check).

#### Scenario: Incident endpoints use role-based auth only

- GIVEN a JWT with `ROLE_AGENT`
- WHEN requesting `GET /api/incidents` or `POST /api/incidents`
- THEN access is granted based on role matrix (not owner-check)
- AND no `sub` vs path variable comparison occurs

#### Scenario: User endpoints use role-based auth only

- GIVEN a JWT with `ROLE_ADMIN`
- WHEN requesting `GET /api/users/{userId}`
- THEN access is granted based on role matrix
- AND no owner-check is performed

### Requirement: Unauthenticated Requests Rejected at Filter Level

Requests without valid JWT are rejected by the JWT resource server filter before reaching the owner-check logic. The system SHALL return 401 for missing/invalid tokens.

#### Scenario: No token returns 401 (not 403)

- GIVEN a request without `Authorization` header
- WHEN requesting `GET /api/notifications/user-123`
- THEN the response status MUST be 401
- AND the owner-check logic is never invoked