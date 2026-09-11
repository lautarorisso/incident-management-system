package com.ims.e2e;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the Keycloak token endpoint contract used by the whole suite.
 */
@DisplayName("Keycloak authentication")
class AuthE2E {

    @Test
    @DisplayName("password grant returns HTTP 200 with a non-empty access token")
    void tokenEndpointIssuesAccessToken() {
        String tokenUrl = E2EConfig.KEYCLOAK_URL + "/realms/" + E2EConfig.KEYCLOAK_REALM
                + "/protocol/openid-connect/token";

        Response response = RestAssured.given()
                .formParam("grant_type", "password")
                .formParam("client_id", E2EConfig.KEYCLOAK_CLIENT)
                .formParam("username", E2EConfig.E2E_USER)
                .formParam("password", E2EConfig.E2E_PASSWORD)
                .when()
                .post(tokenUrl)
                .thenReturn();

        assertThat(response.getStatusCode())
                .as("token endpoint HTTP status for %s", tokenUrl)
                .isEqualTo(200);
        assertThat(response.getContentType())
                .as("token endpoint content type")
                .contains("json");
        assertThat(response.jsonPath().getString("access_token"))
                .as("access_token from %s", tokenUrl)
                .isNotBlank();
    }
}