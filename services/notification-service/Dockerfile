FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY services/notification-service/pom.xml services/notification-service/pom.xml
RUN mvn -pl services/notification-service -am dependency:go-offline -B
COPY . .
RUN mvn -pl services/notification-service -am package -DskipTests -B

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/services/notification-service/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
