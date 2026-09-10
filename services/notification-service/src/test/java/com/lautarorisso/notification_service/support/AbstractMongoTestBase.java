package com.lautarorisso.notification_service.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MongoDBContainer;

/**
 * Base class for tests that need a real MongoDB.
 * <p>
 * Starts one shared {@code mongo:7} container per JVM; Spring Boot
 * auto-configures the MongoDB connection from the container via
 * {@code @ServiceConnection}, so no {@code @DynamicPropertySource} is required.
 * <p>
 * The container is started eagerly (static initializer) and lives for the whole
 * test JVM. The {@code @Testcontainers}/{@code @Container} extension is
 * deliberately NOT used: it stops the container after each test class, which
 * breaks Spring's cached contexts that keep pointing at the stopped container's
 * mapped port.
 */
public abstract class AbstractMongoTestBase {

    @ServiceConnection
    static final MongoDBContainer MONGO = startMongo();

    private static MongoDBContainer startMongo() {
        MongoDBContainer container = new MongoDBContainer("mongo:7");
        container.start();
        return container;
    }

    protected AbstractMongoTestBase() {
    }
}