# Stage 1: Build WAR
FROM gradle:8.10-jdk21-alpine AS backend-builder
WORKDIR /sources-build

# Копируем только Gradle wrapper и зависимости для кэша
COPY build.gradle settings.gradle /sources-build/
COPY gradle /sources-build/gradle
RUN gradle build --no-daemon -x test || true

# Копируем остальной исходный код
COPY --chown=gradle:gradle . /sources-build
RUN gradle clean build --no-daemon -x test

# Stage 2: Payara runtime
FROM payara/server-full:latest
COPY --from=backend-builder /sources-build/build/libs/*.war /opt/payara/deployments/first-service.war
