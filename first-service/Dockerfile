FROM gradle:8.10-jdk21-alpine AS backend-builder
WORKDIR /sources-build

COPY first-service/build.gradle first-service/settings.gradle /sources-build/
COPY first-service/gradle /sources-build/gradle
RUN gradle build --no-daemon -x test || true

COPY --chown=gradle:gradle first-service/ /sources-build
RUN gradle clean build --no-daemon -x test

FROM payara/server-full:latest

COPY --from=backend-builder /sources-build/build/libs/*.war /opt/payara/deployments/first-service.war

USER root
RUN mkdir -p /opt/certs /opt/payara/config

COPY tls/first-service/keystore.p12   /opt/certs/first-service.p12
COPY tls/truststore/truststore.p12    /opt/certs/truststore.p12

RUN chown -R payara:payara /opt/certs

COPY first-service/docker/payara-https-setup.asadmin /opt/payara/config/post-boot-commands.asadmin
RUN chown -R payara:payara /opt/payara/config

ENV JAVA_OPTS="-Djavax.net.ssl.trustStore=/opt/certs/truststore.p12 -Djavax.net.ssl.trustStorePassword=changeit -Djavax.net.ssl.trustStoreType=PKCS12"

USER payara
