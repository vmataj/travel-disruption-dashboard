FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src src
RUN mvn -B package -DskipTests
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/target/travel-disruption-dashboard-1.0.0.jar app.jar
USER 10001
ENTRYPOINT ["java", "-jar", "app.jar"]
