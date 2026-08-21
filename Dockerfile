FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -B -DskipTests dependency:go-offline
COPY src src
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:17-jre
RUN useradd --system --uid 10001 sayarti
WORKDIR /app
COPY --from=build /workspace/target/sayarti-backend-*.jar app.jar
USER sayarti
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
