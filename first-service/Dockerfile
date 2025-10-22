# Build stage
FROM gradle:8.10-jdk21-alpine AS backend-builder
WORKDIR /sources-build
COPY --chown=gradle:gradle . /sources-build
RUN gradle build --no-daemon -x test

# Run stage
FROM payara/server-full:latest
COPY --from=backend-builder /sources-build/build/libs/*.war /opt/payara/deployments/first-service.war
