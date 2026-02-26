# --- ESTÁGIO 1: BUILD ---
# Usamos o JDK completo para compilar
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copiamos o pom.xml primeiro para aproveitar o cache de camadas do Docker
COPY pom.xml .
RUN mvn dependency:go-offline

# Copiamos o código fonte e geramos o build (pulando testes pois o Jenkins já os rodou)
COPY src ./src
RUN mvn package -DskipTests

# --- ESTÁGIO 2: RUNTIME ---
# Usamos uma imagem mínima (JRE) para segurança e leveza
FROM eclipse-temurin:21-jre-alpine
WORKDIR /deployments

# Criamos um usuário não-root para segurança (padrão bancário)
RUN addgroup -S quarkus && adduser -S quarkus -G quarkus
USER quarkus

# Copiamos apenas o diretório quarkus-app gerado no estágio anterior
COPY --from=build --chown=quarkus:quarkus /app/target/quarkus-app/ ./

# Expondo a porta padrão
EXPOSE 8080

# Variáveis de ambiente padrão que o Quarkus reconhece
ENV JAVA_OPTS="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"

ENTRYPOINT [ "java", "-jar", "quarkus-run.jar" ]