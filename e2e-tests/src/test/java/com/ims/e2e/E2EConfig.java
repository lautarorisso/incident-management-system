package com.ims.e2e;

/**
 * Central configuration surface for the black-box E2E suite.
 * <p>
 * Every value is read from the environment with the docker-compose default as
 * fallback, mirroring {@code scripts/smoke-test.sh}. The launcher
 * {@code scripts/e2e-test.sh} exports the same variable names, and the JVM
 * spawned by Maven inherits them — nothing is hardwired inside the tests.
 */
public final class E2EConfig {

    // Service URLs (compose defaults).
    public static final String GATEWAY_URL = env("GATEWAY_URL", "http://localhost:8080");
    public static final String INCIDENT_URL = env("INCIDENT_URL", "http://localhost:8081");
    public static final String NOTIFICATION_URL = env("NOTIFICATION_URL", "http://localhost:8083");
    public static final String USER_URL = env("USER_URL", "http://localhost:8082");
    public static final String DISCOVERY_URL = env("DISCOVERY_URL", "http://localhost:8761");

    // Keycloak: realm/client/user seeded by keycloak/import/ims-realm.json.
    public static final String KEYCLOAK_URL = env("KEYCLOAK_URL", "http://localhost:18080");
    public static final String KEYCLOAK_REALM = env("KEYCLOAK_REALM", "ims");
    public static final String KEYCLOAK_CLIENT = env("KEYCLOAK_CLIENT", "ims-frontend");
    public static final String E2E_USER = env("E2E_USER", "agente1");
    public static final String E2E_PASSWORD = env("E2E_PASSWORD", "agente1234");

    private static String env(String name, String defaultValue) {
        String value = System.getenv(name);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    private E2EConfig() {
    }
}