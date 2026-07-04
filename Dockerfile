FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /workspace

COPY gradlew gradlew.bat settings.gradle build.gradle gradle.properties ./
COPY gradle ./gradle

RUN chmod +x ./gradlew

COPY src ./src

RUN ./gradlew bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S queueforge && adduser -S queueforge -G queueforge

COPY --from=build /workspace/build/libs/*.jar app.jar

USER queueforge

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
