# syntaxe multi-étapes : l'image finale ne contient ni Maven, ni le code source, ni le cache
# de dépendances — seulement un JRE et le jar. Une image de build pèse ~800 Mo, celle-ci ~250.

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Les dépendances d'abord, seules : tant que le pom ne change pas, Docker réutilise cette
# couche et un déploiement ne retélécharge pas tout Maven.
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN ./mvnw -B -q dependency:go-offline

COPY src src
# Tests ignorés ici : ils exigent Docker (Testcontainers), qui n'est pas disponible à
# l'intérieur d'un build d'image. Ils tournent avant le déploiement, pas pendant.
RUN ./mvnw -B -q package -DskipTests

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Utilisateur non privilégié : une faille applicative ne doit pas donner root dans le conteneur.
RUN addgroup -S app && adduser -S -G app app \
    && mkdir -p /app/storage && chown -R app:app /app
USER app

COPY --from=build --chown=app:app /build/target/*.jar app.jar

# MaxRAMPercentage plutôt qu'un -Xmx fixe : la JVM lit la limite du conteneur et s'y adapte.
# À 75 %, il reste de quoi loger le métaspace, les piles de threads et les tampons hors tas.
# Le ramasse-miettes n'est pas imposé : la JVM choisit selon les cœurs et la mémoire réellement
# alloués, et ce choix reste bon si le serveur grossit.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"

EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
    CMD wget -qO- http://127.0.0.1:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
