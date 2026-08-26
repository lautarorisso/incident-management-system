FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY services/shared/pom.xml services/shared/pom.xml
COPY services/config-server/pom.xml services/config-server/pom.xml
COPY services/discovery-service/pom.xml services/discovery-service/pom.xml
COPY services/api-gateway/pom.xml services/api-gateway/pom.xml
COPY services/incident-service/pom.xml services/incident-service/pom.xml
COPY services/notification-service/pom.xml services/notification-service/pom.xml
COPY services/user-service/pom.xml services/user-service/pom.xml
RUN mvn -pl services/config-server -am dependency:go-offline -B
COPY . .
RUN mvn -pl services/config-server -am package -DskipTests -B

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/services/config-server/target/*.jar app.jar
COPY services/config-server/config/ /config/
ENV CONFIG_DIR=/config
EXPOSE 8888
ENTRYPOINT ["java", "-jar", "app.jar"]
