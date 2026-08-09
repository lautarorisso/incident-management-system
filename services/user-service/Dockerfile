FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY services/user-service/pom.xml services/user-service/pom.xml
RUN mvn -pl services/user-service -am dependency:go-offline -B
COPY . .
RUN mvn -pl services/user-service -am package -DskipTests -B

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/services/user-service/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
