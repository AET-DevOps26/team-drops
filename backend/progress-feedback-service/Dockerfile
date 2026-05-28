FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY api ./api
COPY backend/progress-feedback-service ./backend/progress-feedback-service

WORKDIR /app/backend/progress-feedback-service

RUN chmod +x gradlew
RUN ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/backend/progress-feedback-service/build/libs/*.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]