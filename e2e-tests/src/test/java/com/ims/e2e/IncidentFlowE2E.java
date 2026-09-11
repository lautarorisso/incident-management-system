package com.ims.e2e;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The core E2E flow: exercises the full incident lifecycle through the API
 * Gateway with a real Keycloak bearer token, ending with the notification
 * pipeline (transactional outbox {@literal ->} RabbitMQ {@literal ->}
 * notification-service) delivering at least one notification to the assignee.
 * <p>
 * A single ordered {@code @Test} keeps the test independent of JUnit method
 * ordering. A unique title (epoch-millis suffix) makes re-runs deterministic
 * and tolerant of data left over by previous runs.
 */
@DisplayName("Incident lifecycle through the API Gateway")
class IncidentFlowE2E {

    private static final String UUID_PATTERN =
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

    private static final long NOTIFICATION_TIMEOUT_SECONDS = 60;
    private static final long NOTIFICATION_POLL_INTERVAL_SECONDS = 2;

    @Test
    @DisplayName("create → retrieve → list → assign → transition → notification delivered")
    void fullIncidentLifecycleDeliversNotification() {
        String token = TokenProvider.bearerToken();

        // (1) POST /api/incidents — create with a unique title.
        String title = "E2E Incident " + Instant.now().toEpochMilli();
        Response create = authorized(token)
                .contentType("application/json")
                .body("{\"title\":\"" + title + "\","
                        + "\"description\":\"Created by the REST Assured E2E suite\","
                        + "\"priority\":\"HIGH\"}")
                .when()
                .post(E2EConfig.GATEWAY_URL + "/api/incidents")
                .thenReturn();

        assertThat(create.getStatusCode())
                .as("POST /api/incidents HTTP status")
                .isIn(200, 201);
        String incidentId = create.jsonPath().getString("id");
        assertThat(incidentId)
                .as("created incident id")
                .matches(UUID_PATTERN);
        assertThat(create.jsonPath().getString("title"))
                .as("created incident title echoes the request")
                .isEqualTo(title);

        // (2) GET /api/incidents/{id} — retrieve and check initial state.
        Response get = authorized(token)
                .when()
                .get(E2EConfig.GATEWAY_URL + "/api/incidents/" + incidentId)
                .thenReturn();

        assertThat(get.getStatusCode())
                .as("GET /api/incidents/%s HTTP status", incidentId)
                .isEqualTo(200);
        assertThat(get.jsonPath().getString("id"))
                .as("retrieved incident id")
                .isEqualTo(incidentId);
        assertThat(get.jsonPath().getString("status"))
                .as("new incident status")
                .isEqualTo("OPEN");

        // (3) GET /api/incidents — the new incident appears in the list.
        Response list = authorized(token)
                .when()
                .get(E2EConfig.GATEWAY_URL + "/api/incidents")
                .thenReturn();

        assertThat(list.getStatusCode())
                .as("GET /api/incidents HTTP status")
                .isEqualTo(200);
        assertThat(list.jsonPath().getList("content.id", String.class))
                .as("incident list contains the newly created incident")
                .contains(incidentId);

        // (4) GET /api/users — resolve a real assignee (first active user).
        Response users = authorized(token)
                .when()
                .get(E2EConfig.GATEWAY_URL + "/api/users")
                .thenReturn();

        assertThat(users.getStatusCode())
                .as("GET /api/users HTTP status")
                .isEqualTo(200);
        List<String> userIds = users.jsonPath().getList("id", String.class);
        assertThat(userIds)
                .as("user list is not empty")
                .isNotEmpty();
        String assigneeId = userIds.get(0);
        assertThat(assigneeId)
                .as("assignee user id")
                .matches(UUID_PATTERN);

        // (5) PUT /api/incidents/{id}/assign — assign to the resolved user.
        Response assign = authorized(token)
                .contentType("application/json")
                .body("{\"assigneeId\":\"" + assigneeId + "\"}")
                .when()
                .put(E2EConfig.GATEWAY_URL + "/api/incidents/" + incidentId + "/assign")
                .thenReturn();

        assertThat(assign.getStatusCode())
                .as("PUT /api/incidents/%s/assign HTTP status", incidentId)
                .isEqualTo(200);
        assertThat(assign.jsonPath().getString("assigneeId"))
                .as("assigned assignee id")
                .isEqualTo(assigneeId);

        // (6) PUT /api/incidents/{id}/transition — OPEN → IN_PROGRESS.
        Response transition = authorized(token)
                .contentType("application/json")
                .body("{\"newStatus\":\"IN_PROGRESS\"}")
                .when()
                .put(E2EConfig.GATEWAY_URL + "/api/incidents/" + incidentId + "/transition")
                .thenReturn();

        assertThat(transition.getStatusCode())
                .as("PUT /api/incidents/%s/transition HTTP status", incidentId)
                .isEqualTo(200);
        assertThat(transition.jsonPath().getString("status"))
                .as("transitioned incident status")
                .isEqualTo("IN_PROGRESS");

        // (7) Poll notifications through the gateway until the assignee has at
        // least one — proves outbox → RabbitMQ → notification-service delivery.
        boolean delivered = Await.await(
                () -> notificationCount(token, assigneeId) > 0,
                NOTIFICATION_TIMEOUT_SECONDS,
                NOTIFICATION_POLL_INTERVAL_SECONDS);
        int finalCount = notificationCount(token, assigneeId);

        assertThat(delivered)
                .as("at least one notification within %ss for assignee %s",
                        NOTIFICATION_TIMEOUT_SECONDS, assigneeId)
                .isTrue();
        assertThat(finalCount)
                .as("notifications delivered to assignee %s", assigneeId)
                .isGreaterThan(0);
    }

    private static RequestSpecification authorized(String token) {
        return RestAssured.given().auth().oauth2(token);
    }

    /**
     * Returns the number of notifications visible for the given user, or 0 when
     * the endpoint is not yet serving 200 (e.g. gateway cold start or rate
     * limiting) — such responses count as "not delivered yet" while polling.
     */
    private static int notificationCount(String token, String userId) {
        try {
            Response response = authorized(token)
                    .when()
                    .get(E2EConfig.GATEWAY_URL + "/api/notifications?userId=" + userId)
                    .thenReturn();
            if (response.getStatusCode() != 200) {
                return 0;
            }
            List<?> notifications = response.jsonPath().getList("$");
            return notifications == null ? 0 : notifications.size();
        } catch (RuntimeException e) {
            return 0;
        }
    }
}