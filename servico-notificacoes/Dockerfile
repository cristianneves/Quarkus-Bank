# --- ESTÁGIO 1: BUILD ---
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn package -DskipTests

# --- ESTÁGIO 2: RUNTIME ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /deployments

RUN addgroup -S quarkus && adduser -S quarkus -G quarkus
USER quarkus

COPY --from=build --chown=quarkus:quarkus /app/target/quarkus-app/ ./

# Expondo a porta configurada no application.properties
EXPOSE 8084

ENV JAVA_OPTS="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"

ENTRYPOINT [ "java", "-jar", "quarkus-run.jar" ]
