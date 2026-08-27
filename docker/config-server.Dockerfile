FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
ARG MODULE_DIR=services/config-server

# Copy the config-server submodule sources (pom, src, and the yaml configs
# served to all services, which live at the repo root — not in a config/ dir).
COPY ${MODULE_DIR}/pom.xml .
COPY ${MODULE_DIR}/src ./src
COPY ${MODULE_DIR}/api-gateway.yaml \
     ${MODULE_DIR}/discovery-service.yaml \
     ${MODULE_DIR}/incident-service.yaml \
     ${MODULE_DIR}/notification-service.yaml \
     ${MODULE_DIR}/user-service.yaml ./

RUN mvn -DskipTests package -B

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Copy the yaml configs served to all services (they live at the repo root).
COPY --from=build /app/*.yaml ./

# application.properties uses native.search-locations=file:${CONFIG_DIR:./}
ENV CONFIG_DIR=/app
ENV EUREKA_URI=http://discovery-service:8761/eureka/

EXPOSE 8888
ENTRYPOINT ["java", "-jar", "app.jar"]
