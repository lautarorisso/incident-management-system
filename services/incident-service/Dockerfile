FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY services/incident-service/pom.xml services/incident-service/pom.xml
RUN mvn -pl services/incident-service -am dependency:go-offline -B
COPY . .
RUN mvn -pl services/incident-service -am package -DskipTests -B

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/services/incident-service/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
