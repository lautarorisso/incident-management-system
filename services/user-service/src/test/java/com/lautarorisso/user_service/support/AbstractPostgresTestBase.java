package com.lautarorisso.user_service.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for tests that need a real PostgreSQL database.
 * <p>
 * Starts one shared {@code postgres:16} container per JVM; Spring Boot
 * auto-configures the datasource from the container via {@code @ServiceConnection},
 * so no {@code @DynamicPropertySource} is required. Flyway migrations run against
 * this real database and Hibernate validates entities against the migrated schema.
 * <p>
 * The container is started eagerly (static initializer) and lives for the whole
 * test JVM. The {@code @Testcontainers}/{@code @Container} extension is deliberately
 * NOT used: it stops the container after each test class, which breaks Spring's
 * cached {@code @DataJpaTest} contexts that keep pointing at the stopped
 * container's mapped port.
 */
public abstract class AbstractPostgresTestBase {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = startPostgres();

    private static PostgreSQLContainer<?> startPostgres() {
        PostgreSQLContainer<?> container = new PostgreSQLContainer<>("postgres:16");
        container.start();
        return container;
    }

    protected AbstractPostgresTestBase() {
    }
}