# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY api ./api
COPY backend/learning-service ./backend/learning-service

WORKDIR /app/backend/learning-service

RUN chmod +x gradlew
RUN --mount=type=cache,id=team-drops-gradle,target=/root/.gradle,sharing=locked \
    ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/backend/learning-service/build/libs/*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
