package com.ims.e2e;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Black-box assertions for the gateway cleanup and the role matrix, against
 * the real docker-compose stack:
 * <ul>
 *   <li>the Eureka dashboard is no longer exposed through the gateway
 *       (401 unauthenticated, 404 once the route is removed),</li>
 *   <li>the role matrix holds end-to-end: USER creates incidents but cannot
 *       list them or read the user directory, AGENT can list and read,</li>
 *   <li>the removed {@code X-User-Id} header does not affect authorization —
 *       identity comes exclusively from the validated JWT.</li>
 * </ul>
 * Seed users come from {@code keycloak/import/ims-realm.json}.
 */
@DisplayName("Gateway security cleanup and role matrix")
class GatewaySecurityE2E {

    private static final String AGENT_USER = "agente1";
    private static final String AGENT_PASSWORD = "agente1234";
    private static final String USER_USER = "usuario1";
    private static final String USER_PASSWORD = "usuario1234";
    private static final String X_USER_ID_HEADER = "X-User-Id";

    @Test
    @DisplayName("unauthenticated /eureka/ through the gateway returns 401")
    void unauthenticatedEurekaRouteIsUnauthorized() {
        Response response = RestAssured
                .get(E2EConfig.GATEWAY_URL + "/eureka/")
                .thenReturn();

        assertThat(response.getStatusCode())
                .as("unauthenticated GET /eureka/ via gateway")
                .isEqualTo(401);
    }

    @Test
    @DisplayName("authenticated /eureka/web through the gateway returns 404 (route removed)")
    void authenticatedEurekaRouteIsRemoved() {
        Response response = RestAssured.given()
                .auth().oauth2(TokenProvider.bearerToken())
                .when()
                .get(E2EConfig.GATEWAY_URL + "/eureka/web")
                .thenReturn();

        assertThat(response.getStatusCode())
                .as("authenticated GET /eureka/web via gateway")
                .isEqualTo(404);
    }

    @Test
    @DisplayName("USER can create incidents through the gateway")
    void userRoleCanCreateIncident() {
        Response create = RestAssured.given()
                .auth().oauth2(TokenProvider.bearerToken(USER_USER, USER_PASSWORD))
                .contentType("application/json")
                .body("{\"title\":\"E2E USER-created incident " + System.currentTimeMillis()
                        + "\",\"priority\":\"MEDIUM\"}")
                .when()
                .post(E2EConfig.GATEWAY_URL + "/api/incidents")
                .thenReturn();

        assertThat(create.getStatusCode())
                .as("POST /api/incidents with ROLE_USER HTTP status")
                .isIn(200, 201);
    }

    @Test
    @DisplayName("USER cannot list incidents (403 from incident-service)")
    void userRoleCannotListIncidents() {
        Response list = RestAssured.given()
                .auth().oauth2(TokenProvider.bearerToken(USER_USER, USER_PASSWORD))
                .when()
                .get(E2EConfig.GATEWAY_URL + "/api/incidents")
                .thenReturn();

        assertThat(list.getStatusCode())
                .as("GET /api/incidents with ROLE_USER HTTP status")
                .isEqualTo(403);
    }

    @Test
    @DisplayName("AGENT can list incidents through the gateway")
    void agentRoleCanListIncidents() {
        Response list = RestAssured.given()
                .auth().oauth2(TokenProvider.bearerToken())
                .when()
                .get(E2EConfig.GATEWAY_URL + "/api/incidents")
                .thenReturn();

        assertThat(list.getStatusCode())
                .as("GET /api/incidents with ROLE_AGENT HTTP status")
                .isEqualTo(200);
        assertThat(list.jsonPath().getList("content"))
                .as("incident list payload")
                .isNotNull();
    }

    @Test
    @DisplayName("USER cannot read the user directory through the gateway")
    void userRoleCannotListUsers() {
        Response users = RestAssured.given()
                .auth().oauth2(TokenProvider.bearerToken(USER_USER, USER_PASSWORD))
                .when()
                .get(E2EConfig.GATEWAY_URL + "/api/users")
                .thenReturn();

        assertThat(users.getStatusCode())
                .as("GET /api/users with ROLE_USER HTTP status")
                .isEqualTo(403);
    }

    @Test
    @DisplayName("AGENT can read the user directory through the gateway")
    void agentRoleCanListUsers() {
        Response users = RestAssured.given()
                .auth().oauth2(TokenProvider.bearerToken())
                .when()
                .get(E2EConfig.GATEWAY_URL + "/api/users")
                .thenReturn();

        assertThat(users.getStatusCode())
                .as("GET /api/users with ROLE_AGENT HTTP status")
                .isEqualTo(200);
        assertThat(users.jsonPath().getList("id"))
                .as("user directory is not empty")
                .isNotEmpty();
    }

    @Test
    @DisplayName("X-User-Id header is ignored by the notification owner-check")
    void xUserIdHeaderDoesNotGrantOwnership() {
        String assigneeId = firstUserInternalId();

        // The agent is not the notification owner (JWT sub is the Keycloak id,
        // not the user-service internal id) and holds no ROLE_ADMIN, so the
        // owner-check must reject the request even when X-User-Id spoofs the
        // owner — identity comes from the JWT only.
        Response response = RestAssured.given()
                .auth().oauth2(TokenProvider.bearerToken())
                .header(X_USER_ID_HEADER, assigneeId)
                .when()
                .get(E2EConfig.NOTIFICATION_URL + "/api/notifications?userId=" + assigneeId)
                .thenReturn();

        assertThat(response.getStatusCode())
                .as("GET /api/notifications?userId=%s with spoofed X-User-Id HTTP status", assigneeId)
                .isEqualTo(403);
    }

    /**
     * Resolves the first user-service internal id via the gateway (AGENT-only
     * directory access), used as the target for the notification owner-check
     * assertions.
     */
    private static String firstUserInternalId() {
        Response users = RestAssured.given()
                .auth().oauth2(TokenProvider.bearerToken())
                .when()
                .get(E2EConfig.GATEWAY_URL + "/api/users")
                .thenReturn();

        assertThat(users.getStatusCode())
                .as("GET /api/users with ROLE_AGENT HTTP status")
                .isEqualTo(200);
        List<String> userIds = users.jsonPath().getList("id", String.class);
        assertThat(userIds)
                .as("user list is not empty")
                .isNotEmpty();
        return userIds.get(0);
    }
}