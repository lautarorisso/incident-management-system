package com.ims.e2e;

import io.restassured.RestAssured;
import io.restassured.response.Response;

/**
 * Obtains a Keycloak bearer token via the OAuth2 password grant (direct access
 * grants are enabled on the public {@code ims-frontend} client) and caches it
 * for the duration of the suite.
 */
public final class TokenProvider {

    private static volatile String cachedToken;

    /**
     * Returns a cached bearer token, fetching (and caching) it on first use.
     *
     * @throws IllegalStateException if Keycloak rejects the credentials
     */
    public static String bearerToken() {
        String token = cachedToken;
        if (token == null) {
            synchronized (TokenProvider.class) {
                if (cachedToken == null) {
                    cachedToken = fetchToken();
                }
                token = cachedToken;
            }
        }
        return token;
    }

    /**
     * Performs the password grant and returns the raw access token.
     */
    public static String fetchToken() {
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

        String accessToken = null;
        if (response.getStatusCode() == 200
                && response.getContentType() != null
                && response.getContentType().contains("json")) {
            accessToken = response.jsonPath().getString("access_token");
        }

        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalStateException(
                    "Could not obtain an access token from Keycloak (" + tokenUrl + "): "
                            + "HTTP " + response.getStatusCode()
                            + ". Check KEYCLOAK_URL / KEYCLOAK_REALM / KEYCLOAK_CLIENT / "
                            + "E2E_USER / E2E_PASSWORD and that the stack is up.");
        }
        return accessToken;
    }

    private TokenProvider() {
    }
}