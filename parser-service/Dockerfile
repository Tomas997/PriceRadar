FROM gradle:9.4-jdk17-alpine AS builder
WORKDIR /app
COPY . .
RUN gradle :parser-service:bootJar -x test --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/parser-service/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
