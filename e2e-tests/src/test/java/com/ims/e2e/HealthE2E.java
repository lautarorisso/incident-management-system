package com.ims.e2e;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Health-gate assertions for every service in the docker-compose stack.
 * The launcher script ({@code scripts/e2e-test.sh}) waits for these endpoints
 * before running the suite, so these tests assert promptly.
 */
@DisplayName("Service health endpoints")
class HealthE2E {

    @Test
    @DisplayName("Discovery Service /actuator/health returns 200 with status UP")
    void discoveryServiceHealth() {
        assertHealthy(E2EConfig.DISCOVERY_URL, "Discovery Service");
    }

    @Test
    @DisplayName("API Gateway /actuator/health returns 200 with status UP")
    void gatewayHealth() {
        assertHealthy(E2EConfig.GATEWAY_URL, "API Gateway");
    }

    @Test
    @DisplayName("Incident Service /actuator/health returns 200 with status UP")
    void incidentServiceHealth() {
        assertHealthy(E2EConfig.INCIDENT_URL, "Incident Service");
    }

    @Test
    @DisplayName("Notification Service /actuator/health returns 200 with status UP")
    void notificationServiceHealth() {
        assertHealthy(E2EConfig.NOTIFICATION_URL, "Notification Service");
    }

    @Test
    @DisplayName("User Service /actuator/health returns 200 with status UP")
    void userServiceHealth() {
        assertHealthy(E2EConfig.USER_URL, "User Service");
    }

    private static void assertHealthy(String baseUrl, String serviceName) {
        String healthUrl = baseUrl + "/actuator/health";
        Response response = RestAssured.get(healthUrl).thenReturn();

        assertThat(response.getStatusCode())
                .as("%s health endpoint HTTP status (%s)", serviceName, healthUrl)
                .isEqualTo(200);
        assertThat(response.jsonPath().getString("status"))
                .as("%s health status body", serviceName)
                .isEqualTo("UP");
    }
}