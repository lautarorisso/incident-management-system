# Multi-stage build for all IMS services.
# Usage: docker build --build-arg SERVICE=<name> -f Dockerfile .
# docker-compose passes SERVICE (and optionally SERVICE_PORT) via build.args.
#
# SERVICE is one of: discovery-service | api-gateway | incident-service |
#                    notification-service | user-service

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
ARG SERVICE
COPY pom.xml .
COPY services/shared/pom.xml services/shared/pom.xml
COPY services/discovery-service/pom.xml services/discovery-service/pom.xml
COPY services/api-gateway/pom.xml services/api-gateway/pom.xml
COPY services/incident-service/pom.xml services/incident-service/pom.xml
COPY services/notification-service/pom.xml services/notification-service/pom.xml
COPY services/user-service/pom.xml services/user-service/pom.xml
RUN mvn -pl services/$SERVICE -am dependency:go-offline -B
COPY . .
RUN mvn -pl services/$SERVICE -am package -DskipTests -B

FROM eclipse-temurin:21-jre
WORKDIR /app
ARG SERVICE
ARG SERVICE_PORT=8081
COPY --from=build /app/services/$SERVICE/target/*.jar app.jar
EXPOSE $SERVICE_PORT
ENTRYPOINT ["java", "-jar", "app.jar"]
