package com.ims.e2e;

import io.restassured.RestAssured;
import io.restassured.response.Response;

/**
 * Obtains a Keycloak bearer token via the OAuth2 password grant (direct access
 * grants are enabled on the public {@code ims-frontend} client) and caches the
 * default suite token for the duration of the suite.
 * <p>
 * The realm seeds three users with distinct roles: {@code agente1}
 * ({@code ims-agent}), {@code lautaro} ({@code ims-admin}) and {@code usuario1}
 * ({@code ims-user}). {@link #bearerToken()} returns the default agent token;
 * use {@link #bearerToken(String, String)} when a specific role is needed.
 */
public final class TokenProvider {

    private static volatile String cachedToken;

    /**
     * Returns the cached default (agent) bearer token, fetching it on first use.
     *
     * @throws IllegalStateException if Keycloak rejects the credentials
     */
    public static String bearerToken() {
        String token = cachedToken;
        if (token == null) {
            synchronized (TokenProvider.class) {
                if (cachedToken == null) {
                    cachedToken = fetchToken(E2EConfig.E2E_USER, E2EConfig.E2E_PASSWORD);
                }
                token = cachedToken;
            }
        }
        return token;
    }

    /**
     * Fetches a bearer token for an explicit realm user (not cached — the
     * default token stays cached, role-specific tokens are fetched on demand).
     *
     * @throws IllegalStateException if Keycloak rejects the credentials
     */
    public static String bearerToken(String username, String password) {
        return fetchToken(username, password);
    }

    /**
     * Performs the password grant and returns the raw access token, or
     * {@code null} when the endpoint does not answer 200 with JSON.
     */
    public static String fetchToken(String username, String password) {
        Response response = RestAssured.given()
                .formParam("grant_type", "password")
                .formParam("client_id", E2EConfig.KEYCLOAK_CLIENT)
                .formParam("username", username)
                .formParam("password", password)
                .when()
                .post(tokenUrl())
                .thenReturn();

        String accessToken = null;
        if (response.getStatusCode() == 200
                && response.getContentType() != null
                && response.getContentType().contains("json")) {
            accessToken = response.jsonPath().getString("access_token");
        }

        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalStateException(
                    "Could not obtain an access token from Keycloak (" + tokenUrl() + "): "
                            + "HTTP " + response.getStatusCode()
                            + ". Check KEYCLOAK_URL / KEYCLOAK_REALM / KEYCLOAK_CLIENT / "
                            + "E2E_USER / E2E_PASSWORD and that the stack is up.");
        }
        return accessToken;
    }

    private static String tokenUrl() {
        return E2EConfig.KEYCLOAK_URL + "/realms/" + E2EConfig.KEYCLOAK_REALM
                + "/protocol/openid-connect/token";
    }

    private TokenProvider() {
    }
}