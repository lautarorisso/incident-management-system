FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY services/api-gateway/pom.xml services/api-gateway/pom.xml
RUN mvn -pl services/api-gateway -am dependency:go-offline -B
COPY . .
RUN mvn -pl services/api-gateway -am package -DskipTests -B

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/services/api-gateway/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
