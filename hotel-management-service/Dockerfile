# Build context: repository root (see docker-compose.yml).

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY hotel-management-service/pom.xml hotel-management-service/
COPY shop-service/pom.xml shop-service/
COPY payment-service/pom.xml payment-service/
COPY web-app/pom.xml web-app/
# Download dependencies in their own layer so source changes don't re-download them.
RUN mvn -B -q -pl hotel-management-service -am dependency:go-offline
COPY hotel-management-service/src hotel-management-service/src
RUN mvn -B -q -pl hotel-management-service -am -DskipTests package

FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 app
WORKDIR /app
COPY --from=build /workspace/hotel-management-service/target/hotel-management-service.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
