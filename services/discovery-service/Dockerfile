FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY services/discovery-service/pom.xml services/discovery-service/pom.xml
RUN mvn -pl services/discovery-service -am dependency:go-offline -B
COPY . .
RUN mvn -pl services/discovery-service -am package -DskipTests -B

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/services/discovery-service/target/*.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]
