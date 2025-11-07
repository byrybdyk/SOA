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
COPY tls/first-service/first-service.p12 /opt/certs/first-service.p12
COPY tls/shared/truststore.jks       /opt/certs/truststore.jks

# 2) Конвертируем p12 -> jks во время сборки
#    P12_PASS — пароль, которым запакован твой first-service.p12 (ЧАСТО 123456),
#    SRV_STORE_PASS — пароль итогового JKS (лучше оставить единым везде).
ARG P12_PASS=123456
ENV SRV_STORE_PASS=123456
RUN keytool -importkeystore -noprompt \
      -srckeystore /opt/certs/first-service.p12 -srcstoretype PKCS12 -srcstorepass 123456 \
      -destkeystore /opt/certs/first-service.jks -deststoretype JKS      -deststorepass 123456 \
 && chown -R payara:payara /opt/certs

# 3) Фиксируем post-boot команды Payara (Payara сам подхватит этот файл при старте)
COPY first-service/docker/payara-https-setup.asadmin /opt/payara/config/post-boot-commands.asadmin
RUN chown -R payara:payara /opt/payara/config

# 4) JVM truststore для исходящих TLS-запросов
ENV JAVA_OPTS="-Djavax.net.ssl.trustStore=/opt/certs/truststore.jks -Djavax.net.ssl.trustStorePassword=changeit"

USER payara
# (опционально) EXPOSE 8443
