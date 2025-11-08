# ---------- Stage 1: Build WAR ----------
FROM gradle:8.10-jdk21-alpine AS backend-builder
WORKDIR /sources-build

# Кэш зависимостей
COPY first-service/build.gradle first-service/settings.gradle /sources-build/
COPY first-service/gradle /sources-build/gradle
RUN gradle build --no-daemon -x test || true

# Код сервиса
COPY --chown=gradle:gradle first-service/ /sources-build
RUN gradle clean build --no-daemon -x test

# ---------- Stage 2: Payara runtime ----------
FROM payara/server-full:latest

# Деплой приложения
COPY --from=backend-builder /sources-build/build/libs/*.war /opt/payara/deployments/first-service.war

# ---------- TLS и post-boot ----------
USER root
RUN mkdir -p /opt/certs /opt/payara/config

# 1) Кладём p12 и truststore (из репо)
COPY tls/first-service/keystore.p12   /opt/certs/first-service.p12
COPY tls/truststore/truststore.p12    /opt/certs/truststore.p12

RUN chown -R payara:payara /opt/certs

# 2) post-boot команды Payara (пусть настраивают HTTPS, указывая PKCS12)
COPY first-service/docker/payara-https-setup.asadmin /opt/payara/config/post-boot-commands.asadmin
RUN chown -R payara:payara /opt/payara/config

# 3) JVM truststore для исходящих TLS-запросов (PKCS12 + наш пароль)
ENV JAVA_OPTS="-Djavax.net.ssl.trustStore=/opt/certs/truststore.p12 -Djavax.net.ssl.trustStorePassword=changeit -Djavax.net.ssl.trustStoreType=PKCS12"

USER payara
# (опционально) EXPOSE 8443
