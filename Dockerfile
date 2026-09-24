FROM maven:3.9.16-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml ./
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN mkdir -p /app/config && chown -R 10001:10001 /app
COPY --from=build --chown=10001:10001 /build/target/portal-0.0.1-SNAPSHOT.jar /app/portal.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/portal.jar"]
