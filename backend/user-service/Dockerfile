FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY api ./api
COPY backend/user-service ./backend/user-service

WORKDIR /app/backend/user-service

RUN chmod +x gradlew
RUN ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/backend/user-service/build/libs/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]