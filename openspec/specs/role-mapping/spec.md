# Role Mapping Specification

## Purpose

Port the gateway's reactive `JwtRoleConverter` logic to a servlet-compatible `Converter<Jwt, Collection<GrantedAuthority>>` in the shared library. Map Keycloak realm roles `ims-admin`, `ims-agent`, `ims-user` to Spring Security roles `ROLE_ADMIN`, `ROLE_AGENT`, `ROLE_USER`.

## Requirements

### Requirement: Realm Role to Spring Role Conversion

The `JwtGrantedAuthoritiesConverter` MUST extract roles from the `realm_access.roles` claim in the JWT and map them to Spring Security `ROLE_*` authorities. The system SHALL apply the mapping: `ims-admin` → `ROLE_ADMIN`, `ims-agent` → `ROLE_AGENT`, `ims-user` → `ROLE_USER`.

#### Scenario: ims-admin maps to ROLE_ADMIN

- GIVEN a JWT with `realm_access.roles = ["ims-admin", "offline_access"]`
- WHEN the converter processes the token
- THEN the resulting authorities MUST include `ROLE_ADMIN`
- AND `ROLE_ADMIN` is the only mapped role from that token

#### Scenario: ims-agent maps to ROLE_AGENT

- GIVEN a JWT with `realm_access.roles = ["ims-agent", "default-roles-ims"]`
- WHEN the converter processes the token
- THEN the resulting authorities MUST include `ROLE_AGENT`

#### Scenario: ims-user maps to ROLE_USER

- GIVEN a JWT with `realm_access.roles = ["ims-user"]`
- WHEN the converter processes the token
- THEN the resulting authorities MUST include `ROLE_USER`

#### Scenario: Multiple roles produce multiple authorities

- GIVEN a JWT with `realm_access.roles = ["ims-admin", "ims-agent"]`
- WHEN the converter processes the token
- THEN the resulting authorities MUST include `ROLE_ADMIN`
- AND MUST include `ROLE_AGENT`

#### Scenario: Unknown roles ignored

- GIVEN a JWT with `realm_access.roles = ["custom-role", "other-role"]`
- WHEN the converter processes the token
- THEN the resulting authorities MUST NOT include any `ROLE_*` from unknown roles
- AND the authorities collection MAY be empty (if no mapped roles present)

#### Scenario: Missing realm_access produces empty authorities

- GIVEN a JWT without `realm_access` claim
- WHEN the converter processes the token
- THEN the resulting authorities MUST be empty

#### Scenario: Empty realm_access produces empty authorities

- GIVEN a JWT with `realm_access.roles = []`
- WHEN the converter processes the token
- THEN the resulting authorities MUST be empty

### Requirement: Consistent Mapping Across Gateway and Services

The servlet converter in `com.ims.shared.security` MUST produce identical role mappings to the gateway's reactive `JwtRoleConverter`. The system SHALL ensure a user with `ims-admin` gets `ROLE_ADMIN` in both gateway and business services.

#### Scenario: Same token yields same roles in gateway and service

- GIVEN a JWT with `realm_access.roles = ["ims-agent"]`
- WHEN validated by gateway (reactive converter)
- AND validated by incident-service (servlet converter)
- THEN both MUST produce `ROLE_AGENT` authority

### Requirement: Converter Integrated in SecurityFilterChain Factory

The shared `SecurityFilterChainFactory` MUST wire the `JwtGrantedAuthoritiesConverter` into the OAuth2 resource server configuration. The system SHALL apply the converter when building the `JwtAuthenticationConverter`.

#### Scenario: SecurityFilterChain uses shared converter

- GIVEN a service imports `SharedSecurityConfiguration`
- WHEN the `SecurityFilterChain` is built
- THEN the `JwtAuthenticationConverter` uses `JwtGrantedAuthoritiesConverter`
- AND authorities from JWT match the mapping table