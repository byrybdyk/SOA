# Stage 1: Build WAR
FROM gradle:8.10-jdk21-alpine AS builder
WORKDIR /app

# Копируем только Gradle wrapper и build files для кэша
COPY build.gradle settings.gradle /app/
COPY gradle /app/gradle
RUN gradle build --no-daemon -x test || true

# Копируем остальной код
COPY . /app
RUN gradle clean war --no-daemon

# Stage 2: WildFly runtime
FROM quay.io/wildfly/wildfly:latest
USER root
COPY keystore.jks /opt/jboss/wildfly/standalone/configuration/
COPY standalone.xml /opt/jboss/wildfly/standalone/configuration/standalone.xml
COPY --from=builder /app/build/libs/second-service.war /opt/jboss/wildfly/standalone/deployments/
USER jboss
EXPOSE 8081 8443
ENTRYPOINT ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0"]
