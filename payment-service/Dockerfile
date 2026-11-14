# Build context: repository root (see docker-compose.yml).

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY hotel-management-service/pom.xml hotel-management-service/
COPY shop-service/pom.xml shop-service/
COPY payment-service/pom.xml payment-service/
COPY web-app/pom.xml web-app/
# Download dependencies in their own layer so source changes don't re-download them.
RUN mvn -B -q -pl payment-service -am dependency:go-offline
COPY payment-service/src payment-service/src
RUN mvn -B -q -pl payment-service -am -DskipTests package

FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 app
WORKDIR /app
COPY --from=build /workspace/payment-service/target/payment-service.jar app.jar
USER app
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
